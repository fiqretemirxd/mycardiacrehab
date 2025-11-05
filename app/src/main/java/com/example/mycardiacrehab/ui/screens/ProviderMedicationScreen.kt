package com.example.mycardiacrehab.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.User
import com.example.mycardiacrehab.viewmodel.MedicationViewModel
import com.example.mycardiacrehab.viewmodel.ProviderViewModel
import java.text.SimpleDateFormat // Added for formatting time
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderMedicationScreen(
    providerViewModel: ProviderViewModel = viewModel(),
    medicationViewModel: MedicationViewModel = viewModel()
) {
    val context = LocalContext.current

    // Load patient list for selection
    LaunchedEffect(Unit) {
        providerViewModel.loadPatients()
    }

    val patients by providerViewModel.patients.collectAsState()
    val isScheduling by medicationViewModel.loading.collectAsState()

    // Form state
    var selectedPatient by remember { mutableStateOf<User?>(null) }
    var medName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Once Daily") }

    // --- NEW Time State Management ---
    val selectedTime by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0) }) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // --- Time Picker Dialog ---
    val timePickerDialog = remember {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, minute)
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            true // Use 24-hour clock
        )
    }

    val saveMedication: () -> Unit = {
        if (selectedPatient != null && medName.isNotBlank() && dosage.isNotBlank()) {
            medicationViewModel.setPrescription(
                // ✅ FIX #1: Correct property is 'uid' not 'userId'
                patientId = selectedPatient!!.userId,
                medicationName = medName,
                dosage = dosage,
                frequency = frequency,
                timeOfDay = timeFormatter.format(selectedTime.time) // Use formatted time
            )
            Toast.makeText(context, "Prescription set for ${selectedPatient?.fullName}", Toast.LENGTH_SHORT).show()

            // Reset form
            medName = ""
            dosage = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Prescribe Patient Medication", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // 1. Patient Selection
        ProviderPatientSelectionDropdown(
            patients = patients,
            selectedPatient = selectedPatient,
            onPatientSelected = { selectedPatient = it }
        )
        Spacer(Modifier.height(24.dp))

        Text("Prescription Details", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        // 2. Medication Details
        OutlinedTextField(
            value = medName,
            onValueChange = { medName = it },
            label = { Text("Medication Name (e.g., Atorvastatin)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        OutlinedTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = { Text("Dosage (e.g., 20mg, 1 tablet)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // 3. Frequency and Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val frequencies = listOf("Once Daily", "Twice Daily", "Three Times Daily")
            Text("Frequency:", modifier = Modifier.padding(end = 8.dp))
            DropdownSelectorHelper(
                options = frequencies,
                selectedOption = frequency,
                onOptionSelected = { frequency = it }
            )

            Text("Time:", modifier = Modifier.padding(start = 16.dp, end = 8.dp))

            // ✅ FIX #2: Integrated your desired Box layout for the time picker
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = timeFormatter.format(selectedTime.time),
                    onValueChange = { /* Read-only */ },
                    label = { Text("Time") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Time") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { timePickerDialog.show() }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 4. Submit Button
        Button(
            onClick = saveMedication,
            enabled = selectedPatient != null && medName.isNotBlank() && dosage.isNotBlank() && !isScheduling,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isScheduling) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("PRESCRIBE & SET REMINDER")
            }
        }
    }
}

// Helper composable remain unchanged
@Composable
private fun DropdownSelectorHelper(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.wrapContentSize(Alignment.TopStart)) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedOption)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPatientSelectionDropdown(
    patients: List<User>,
    selectedPatient: User?,
    onPatientSelected: (User) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedPatient?.fullName ?: "Select Patient",
            onValueChange = { /* Read-only */ },
            label = { Text("Patient") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            patients.forEach { patient ->
                DropdownMenuItem(
                    text = { Text("${patient.fullName} (${patient.email})") },
                    onClick = {
                        onPatientSelected(patient)
                        expanded = false
                    }
                )
            }
        }
    }
}
