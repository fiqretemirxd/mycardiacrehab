package com.example.mycardiacrehab.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor // 🟢 Import with alias
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share // 🟢 Use Share icon
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.core.content.FileProvider // 🟢 Import
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.PatientReport
import com.example.mycardiacrehab.model.User
import com.example.mycardiacrehab.viewmodel.ProviderViewModel
import com.example.mycardiacrehab.viewmodel.ReportViewModel
import java.io.File // 🟢 Import
import java.io.FileOutputStream // 🟢 Import
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReportCenterScreen(
    providerViewModel: ProviderViewModel = viewModel(),
    reportViewModel: ReportViewModel = viewModel()
) {
    // ... (existing state observations) ...
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

        // 1. Patient Selection
        PatientSelectionDropdown(
            patients = patients,
            selectedPatient = selectedPatient,
            onPatientSelected = { selectedPatient = it },
            isLoading = isLoadingPatients
        )

        Spacer(Modifier.height(16.dp))

        // 2. Generate Report Button
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

        // 3. Report Display
        if (isLoadingReport) {
            CircularProgressIndicator()
        } else if (report != null) {
            ReportCard(report = report!!)

            Spacer(Modifier.height(16.dp))

            // 🟢 UPDATED: PDF Share Button
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

// 🟢 NEW HELPER FUNCTION: Contains your PDF logic, but saves to cache and shares
@RequiresApi(Build.VERSION_CODES.O)
private fun generateAndSharePdf(context: Context, report: PatientReport, patient: User) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val paint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
        color = AndroidColor.BLACK
    }
    canvas.drawText("Weekly Patient Report", 60f, 80f, paint)

    paint.textSize = 14f
    paint.isFakeBoldText = false
    var yPosition = 120f
    val lineSpacing = 25f

    // Use the same formatter as the ReportCard
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    val lines = listOf(
        "Patient: ${report.patientName}",
        // 🟢 FIX: Format the LocalDate objects
        "Period: ${report.startDate.format(dateFormatter)} to ${report.endDate.format(dateFormatter)}",
        "Generated: ${report.dateGenerated.format(dateFormatter)}",
        "",
        "Total Exercise: ${report.totalExerciseMinutes} min",
        "Exercise Compliance: ${report.exerciseComplianceRate}%",
        "Medication Adherence: ${report.medicationAdherenceRate}%",
        "Common Symptom: ${report.mostCommonSymptoms}",
        "",
        "AI Chatbot Audit:",
        "Total Chats: ${report.totalChatInteractions}",
        "Out-of-Scope Flags: ${report.outOfScopeInteractions}"
    )

    for (line in lines) {
        canvas.drawText(line, 60f, yPosition, paint)
        yPosition += lineSpacing
    }

    pdfDocument.finishPage(page)

    try {
        // 1. Create a directory in the app's cache
        val pdfDir = File(context.cacheDir, "reports")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        // 2. Create the file in the cache directory
        val file = File(pdfDir, "Report_${patient.fullName.replace(" ", "_")}.pdf")
        val fileOutputStream = FileOutputStream(file)
        pdfDocument.writeTo(fileOutputStream)
        pdfDocument.close()
        fileOutputStream.close()

        // 3. Get the secure URI using FileProvider
        val authority = "${context.packageName}.provider"
        val pdfUri = FileProvider.getUriForFile(context, authority, file)

        // 4. Create the Share Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "Patient Report for ${patient.fullName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // 5. Launch the Share sheet
        context.startActivity(Intent.createChooser(shareIntent, "Share Report As..."))

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}


// ... (PatientSelectionDropdown composable remains the same) ...
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

// ... (ReportCard and ReportMetric composables remain the same) ...
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
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Metrics Grid
            Spacer(Modifier.height(12.dp))
            ReportMetric("Total Exercise", "${report.totalExerciseMinutes} min")
            ReportMetric("Exercise Compliance", "${report.exerciseComplianceRate}%")
            ReportMetric("Meds Adherence", "${report.medicationAdherenceRate}%")
            ReportMetric("Common Symptom", report.mostCommonSymptoms)

            HorizontalDivider(
                Modifier.padding(vertical = 12.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )

            // AI Interaction Audit
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