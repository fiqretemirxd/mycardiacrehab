package com.example.mycardiacrehab.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    // Load patients when the screen is first composed
    LaunchedEffect(Unit) {
        providerViewModel.loadPatients()
    }

    val patients by providerViewModel.patients.collectAsState()
    val isLoadingPatients by providerViewModel.loading.collectAsState()

    val report by reportViewModel.report.collectAsState()
    val isLoadingReport by reportViewModel.loading.collectAsState()

    var selectedPatient by remember { mutableStateOf<User?>(null) }
    val reportDays = 7 // Default report period (Weekly)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Report Generation Center", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // --- 1. Patient Selection Dropdown (Corrected) ---
        PatientSelectionDropdown(
            patients = patients,
            selectedPatient = selectedPatient,
            onPatientSelected = { selectedPatient = it },
            isLoading = isLoadingPatients
        )

        Spacer(Modifier.height(16.dp))

        // --- 2. Generate Report Button (F04-2) ---
        Button(
            onClick = {
                selectedPatient?.let {
                    reportViewModel.generateReport(it.userId, it.fullName, reportDays)
                }
            },
            enabled = selectedPatient != null && !isLoadingReport,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoadingReport) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("GENERATE WEEKLY REPORT ($reportDays DAYS)")
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- 3. Report Display (F04-2, Interview Q9) (Corrected) ---
        if (isLoadingReport) {
            CircularProgressIndicator()
        } else if (report != null) {
            ReportCard(report = report!!) // Pass the non-null report
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

    // Use ExposedDropdownMenuBox for the correct Material 3 implementation
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
                .menuAnchor(MenuAnchorType.PrimaryEditable) // This is crucial for connecting the text field to the menu
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
    // Use a localized, medium-style date formatter for better readability
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "COMPREHENSIVE PATIENT REPORT",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            // Use the new startDate and endDate fields
            Text(
                "For: ${report.patientName} | Period: ${report.startDate.format(dateFormatter)} to ${report.endDate.format(dateFormatter)}",
                style = MaterialTheme.typography.titleMedium
            )
            // Format the new LocalDate `dateGenerated`
            Text(
                "Generated: ${report.dateGenerated.format(dateFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            HorizontalDivider()

            // Metrics Grid
            Spacer(Modifier.height(12.dp))
            ReportMetric("Exercise Mins Logged", "${report.totalExerciseMinutes} min", MaterialTheme.colorScheme.tertiary)
            ReportMetric("Meds Adherence", "${report.medicationAdherenceRate}%", MaterialTheme.colorScheme.secondary)
            ReportMetric("Exercise Compliance", "${report.exerciseComplianceRate}%", MaterialTheme.colorScheme.primary)
            ReportMetric("Most Common Symptom", report.mostCommonSymptoms, MaterialTheme.colorScheme.error)

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // AI Interaction Audit (F04-3)
            Text("AI CHATBOT AUDIT", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            ReportMetric("Total User Messages", "${report.totalChatInteractions}", MaterialTheme.colorScheme.onSurfaceVariant)
            ReportMetric("Out-of-Scope Flags", "${report.outOfScopeInteractions}", MaterialTheme.colorScheme.error)
            Text(
                "Review out-of-scope flags for potential patient confusion or high-risk queries.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReportMetric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(color = color, fontWeight = FontWeight.SemiBold))
    }
}
