package com.example.mycardiacrehab.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.PatientReport
import com.example.mycardiacrehab.model.User
import com.example.mycardiacrehab.viewmodel.ProviderViewModel
import com.example.mycardiacrehab.viewmodel.ReportViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReportCenterScreen(
    providerViewModel: ProviderViewModel = viewModel(),
    reportViewModel: ReportViewModel = viewModel()
) {
    // Load patients for the dropdown
    LaunchedEffect(Unit) {
        providerViewModel.loadPatients()
    }

    val patients by providerViewModel.patients.collectAsState()
    val isLoadingPatients by providerViewModel.loading.collectAsState()

    // Observe the report data and loading state from ReportViewModel
    val report by reportViewModel.report.collectAsState()
    val isLoadingReport by reportViewModel.isLoading.collectAsState() // This resolves '_isLoading'

    var selectedPatient by remember { mutableStateOf<User?>(null) }
    val daysToCover = 7 // This resolves 'daysToCover'

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

        // 1. Patient Selection Dropdown
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
            ReportCard(report = report!!) // Pass the non-null report

            Spacer(Modifier.height(16.dp))

            // PDF Download Button (Placeholder)
            Button(
                onClick = {
                    // TODO: Implement PDF generation logic here
                    // This requires native Android SDK calls (PdfDocument, Canvas)
                    // or a third-party library.
                    Toast.makeText(context, "PDF Generation not implemented.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download Report")
                Spacer(Modifier.width(8.dp))
                Text("DOWNLOAD AS PDF")
            }

        } else if (!isLoadingPatients) {
            Text("Select a patient and generate a report summary.")
        }
    }
}

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
            Divider()

            // Metrics Grid
            Spacer(Modifier.height(12.dp))
            ReportMetric("Total Exercise", "${report.totalExerciseMinutes} min")
            ReportMetric("Exercise Compliance", "${report.exerciseComplianceRate}%")
            ReportMetric("Meds Adherence", "${report.medicationAdherenceRate}%")
            ReportMetric("Common Symptom", report.mostCommonSymptoms)

            Divider(Modifier.padding(vertical = 12.dp))

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