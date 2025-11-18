package com.example.mycardiacrehab.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.PatientReport
import com.example.mycardiacrehab.model.User
import com.example.mycardiacrehab.viewmodel.ProviderViewModel
import com.example.mycardiacrehab.viewmodel.ReportViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReportCenterScreen(
    providerViewModel: ProviderViewModel = viewModel(),
    reportViewModel: ReportViewModel = viewModel()
) {
    val patients by providerViewModel.patients.collectAsState()
    val isLoadingPatients by providerViewModel.loading.collectAsState()
    val report by reportViewModel.report.collectAsState()
    val isLoadingReport by reportViewModel.isLoading.collectAsState()
    var selectedPatient by remember { mutableStateOf<User?>(null) }
    val daysToCover = 7
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Report Generation Center", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Patient Selection
        PatientSelectionDropdown(
            patients = patients,
            selectedPatient = selectedPatient,
            onPatientSelected = { selectedPatient = it },
            isLoading = isLoadingPatients
        )

        Spacer(Modifier.height(16.dp))

        // Generate Report Button
        Button(
            onClick = {
                selectedPatient?.let {
                    reportViewModel.generateReport(it.userId, it.fullName, daysToCover)
                }
            },
            enabled = selectedPatient != null && !isLoadingReport,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoadingReport) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("GENERATE WEEKLY REPORT ($daysToCover DAYS)")
            }
        }

        Spacer(Modifier.height(24.dp))

        // Report Display
        if (isLoadingReport) {
            CircularProgressIndicator()
        } else if (report != null) {
            ReportCard(report = report!!)

            Spacer(Modifier.height(16.dp))

            // PDF Share Button
            Button(
                onClick = {
                    selectedPatient?.let { patient ->
                        report?.let { currentReport ->
                            generateAndSharePdf(context, currentReport, patient)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share Report")
                Spacer(Modifier.width(8.dp))
                Text("SHARE AS PDF")
            }

        } else if (!isLoadingPatients) {
            Text("Select a patient and generate a report summary.")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun generateAndSharePdf(context: Context, report: PatientReport, patient: User) {
    val pdfDocument = PdfDocument()
    val pageWidth = 595 // A4 width in points
    val pageHeight = 842 // A4 height in points
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    // Margins
    val leftMargin = 60f
    val rightMargin = 535f
    var yPosition = 60f
    val lineHeight = 25f

    // Title Paint
    val titlePaint = Paint().apply {
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = AndroidColor.rgb(0, 105, 92) // Teal color
    }

    // Header Paint
    val headerPaint = Paint().apply {
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = AndroidColor.BLACK
    }

    // Body Paint
    val bodyPaint = Paint().apply {
        textSize = 12f
        color = AndroidColor.DKGRAY
    }

    // Value Paint (for metrics)
    val valuePaint = Paint().apply {
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = AndroidColor.BLACK
    }

    // Date formatter
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    try {
        // ===== TITLE =====
        canvas.drawText("WEEKLY PATIENT REPORT", leftMargin, yPosition, titlePaint)
        yPosition += 35f

        // Draw a line under title
        val linePaint = Paint().apply {
            strokeWidth = 2f
            color = AndroidColor.rgb(0, 105, 92)
        }
        canvas.drawLine(leftMargin, yPosition, rightMargin, yPosition, linePaint)
        yPosition += 25f

        // ===== PATIENT INFORMATION =====
        canvas.drawText("Patient Information", leftMargin, yPosition, headerPaint)
        yPosition += lineHeight

        canvas.drawText("Name:", leftMargin + 20f, yPosition, bodyPaint)
        canvas.drawText(report.patientName, leftMargin + 150f, yPosition, valuePaint)
        yPosition += lineHeight

        canvas.drawText("Patient ID:", leftMargin + 20f, yPosition, bodyPaint)
        canvas.drawText(report.patientId.take(8) + "...", leftMargin + 150f, yPosition, valuePaint)
        yPosition += lineHeight

        canvas.drawText("Report Period:", leftMargin + 20f, yPosition, bodyPaint)
        canvas.drawText(
            "${report.startDate.format(dateFormatter)} to ${report.endDate.format(dateFormatter)}",
            leftMargin + 150f,
            yPosition,
            valuePaint
        )
        yPosition += lineHeight

        canvas.drawText("Generated On:", leftMargin + 20f, yPosition, bodyPaint)
        canvas.drawText(report.dateGenerated.format(dateFormatter), leftMargin + 150f, yPosition, valuePaint)
        yPosition += 35f

        // ===== REHABILITATION METRICS =====
        canvas.drawText("Rehabilitation Metrics", leftMargin, yPosition, headerPaint)
        yPosition += lineHeight

        // Exercise
        canvas.drawText("Total Exercise:", leftMargin + 20f, yPosition, bodyPaint)
        canvas.drawText("${report.totalExerciseMinutes} minutes", leftMargin + 200f, yPosition, valuePaint)
        yPosition += lineHeight

        // Exercise Compliance
        canvas.drawText("Exercise Compliance:", leftMargin + 20f, yPosition, bodyPaint)
        val complianceColor = if (report.exerciseComplianceRate >= 80) {
            AndroidColor.rgb(76, 175, 80) // Green
        } else {
            AndroidColor.rgb(244, 67, 54) // Red
        }
        valuePaint.color = complianceColor
        canvas.drawText("${report.exerciseComplianceRate}%", leftMargin + 200f, yPosition, valuePaint)
        valuePaint.color = AndroidColor.BLACK
        yPosition += lineHeight

        // Medication Adherence
        canvas.drawText("Medication Adherence:", leftMargin + 20f, yPosition, bodyPaint)
        val adherenceColor = if (report.medicationAdherenceRate >= 80) {
            AndroidColor.rgb(76, 175, 80) // Green
        } else {
            AndroidColor.rgb(244, 67, 54) // Red
        }
        valuePaint.color = adherenceColor
        canvas.drawText("${report.medicationAdherenceRate}%", leftMargin + 200f, yPosition, valuePaint)
        valuePaint.color = AndroidColor.BLACK
        yPosition += lineHeight

        // Common Symptoms
        canvas.drawText("Most Common Symptom:", leftMargin + 20f, yPosition, bodyPaint)
        canvas.drawText(report.mostCommonSymptoms, leftMargin + 200f, yPosition, valuePaint)
        yPosition += 35f

        // ===== AI CHATBOT AUDIT =====
        canvas.drawText("AI Chatbot Audit", leftMargin, yPosition, headerPaint)
        yPosition += lineHeight

        canvas.drawText("Total Chat Interactions:", leftMargin + 20f, yPosition, bodyPaint)
        canvas.drawText("${report.totalChatInteractions}", leftMargin + 200f, yPosition, valuePaint)
        yPosition += lineHeight

        canvas.drawText("Out-of-Scope Queries:", leftMargin + 20f, yPosition, bodyPaint)
        val scopeColor = if (report.outOfScopeInteractions > 0) {
            AndroidColor.rgb(244, 67, 54) // Red
        } else {
            AndroidColor.rgb(76, 175, 80) // Green
        }
        valuePaint.color = scopeColor
        canvas.drawText("${report.outOfScopeInteractions}", leftMargin + 200f, yPosition, valuePaint)
        valuePaint.color = AndroidColor.BLACK
        yPosition += 35f

        // ===== CLINICAL RECOMMENDATIONS =====
        canvas.drawText("Clinical Recommendations", leftMargin, yPosition, headerPaint)
        yPosition += lineHeight

        val recommendations = buildRecommendations(report)
        val recommendationPaint = Paint().apply {
            textSize = 11f
            color = AndroidColor.DKGRAY
        }

        recommendations.forEach { recommendation ->
            // Word wrap for recommendations
            val words = recommendation.split(" ")
            var line = ""
            words.forEach { word ->
                val testLine = if (line.isEmpty()) word else "$line $word"
                val testWidth = recommendationPaint.measureText(testLine)
                if (testWidth > (rightMargin - leftMargin - 40f)) {
                    canvas.drawText("• $line", leftMargin + 20f, yPosition, recommendationPaint)
                    yPosition += 20f
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText("• $line", leftMargin + 20f, yPosition, recommendationPaint)
                yPosition += 20f
            }
            yPosition += 5f
        }

        yPosition += 20f

        // ===== FOOTER =====
        val footerY = pageHeight - 40f
        val footerPaint = Paint().apply {
            textSize = 9f
            color = AndroidColor.GRAY
        }
        canvas.drawText("Generated by MyCardiacRehab System", leftMargin, footerY, footerPaint)
        canvas.drawText("Report ID: ${report.patientId.take(8)}-${System.currentTimeMillis()}",
            leftMargin, footerY + 12f, footerPaint)

        pdfDocument.finishPage(page)

        // Save and Share PDF
        val pdfDir = File(context.cacheDir, "reports")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Report_${patient.fullName.replace(" ", "_")}_$timestamp.pdf"
        val file = File(pdfDir, fileName)

        val fileOutputStream = FileOutputStream(file)
        pdfDocument.writeTo(fileOutputStream)
        pdfDocument.close()
        fileOutputStream.close()

        // Share using FileProvider
        val authority = "${context.packageName}.provider"
        val pdfUri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "Patient Report - ${patient.fullName}")
            putExtra(Intent.EXTRA_TEXT, "Weekly rehabilitation report for ${patient.fullName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Report"))

        Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// Helper function to build recommendations based on report data
@RequiresApi(Build.VERSION_CODES.O)
private fun buildRecommendations(report: PatientReport): List<String> {
    val recommendations = mutableListOf<String>()

    // Exercise recommendations
    if (report.exerciseComplianceRate < 50) {
        recommendations.add("Patient requires increased motivation for exercise adherence. Consider setting smaller, achievable goals.")
    } else if (report.exerciseComplianceRate < 80) {
        recommendations.add("Exercise compliance is moderate. Encourage consistency and gradual intensity increase.")
    } else {
        recommendations.add("Excellent exercise compliance. Maintain current routine and consider gradual progression.")
    }

    // Medication recommendations
    if (report.medicationAdherenceRate < 80) {
        recommendations.add("Medication adherence needs improvement. Schedule review of medication schedule and potential barriers.")
    } else {
        recommendations.add("Good medication adherence maintained.")
    }

    // Symptom recommendations
    if (report.mostCommonSymptoms != "None Reported" && report.mostCommonSymptoms != "None") {
        recommendations.add("Monitor reported symptom: ${report.mostCommonSymptoms}. Consider clinical follow-up if symptoms persist.")
    }

    // AI interaction recommendations
    if (report.outOfScopeInteractions > 3) {
        recommendations.add("Patient has ${report.outOfScopeInteractions} out-of-scope queries. Schedule counseling session to address concerns beyond rehabilitation scope.")
    }

    return recommendations
}

// UI Components (unchanged from your original code)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientSelectionDropdown(
    patients: List<User>,
    selectedPatient: User?,
    onPatientSelected: (User) -> Unit,
    isLoading: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedPatient?.fullName ?: "Select Patient",
            onValueChange = {},
            label = { Text("Patient") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (isLoading) {
                DropdownMenuItem(
                    text = { Text("Loading patients...") },
                    onClick = { },
                    enabled = false
                )
            } else if (patients.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No patients found") },
                    onClick = { },
                    enabled = false
                )
            } else {
                patients.forEach { patient ->
                    DropdownMenuItem(
                        text = { Text("${patient.fullName} (${patient.userId.take(4)}...)") },
                        onClick = {
                            onPatientSelected(patient)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReportCard(report: PatientReport) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Weekly Patient Report",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "For: ${report.patientName}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Period: ${report.startDate.format(dateFormatter)} to ${report.endDate.format(dateFormatter)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Generated: ${report.dateGenerated.format(dateFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            HorizontalDivider()

            Spacer(Modifier.height(12.dp))
            ReportMetric("Total Exercise", "${report.totalExerciseMinutes} min")
            ReportMetric("Exercise Compliance", "${report.exerciseComplianceRate}%")
            ReportMetric("Meds Adherence", "${report.medicationAdherenceRate}%")
            ReportMetric("Common Symptom", report.mostCommonSymptoms)

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text("AI Chatbot Audit", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            ReportMetric("Total User Chats", "${report.totalChatInteractions}")
            ReportMetric("Out-of-Scope Flags", "${report.outOfScopeInteractions}",
                valueColor = if (report.outOfScopeInteractions > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReportMetric(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}