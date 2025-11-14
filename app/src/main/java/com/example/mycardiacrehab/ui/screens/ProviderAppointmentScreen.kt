package com.example.mycardiacrehab.ui.screens

import android.app.DatePickerDialog as AndroidDatePickerDialog
import android.app.TimePickerDialog as AndroidTimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.Appointment
import com.example.mycardiacrehab.model.User
import com.example.mycardiacrehab.viewmodel.AppointmentViewModel
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.ProviderViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderAppointmentScreen(
    providerViewModel: ProviderViewModel = viewModel(),
    appointmentViewModel: AppointmentViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    // Load patient list for selection
    LaunchedEffect(Unit) {
        providerViewModel.loadPatients()
        providerViewModel.loadProviderProfile()
    }

    val patients by providerViewModel.patients.collectAsState()
    val providerProfile by providerViewModel.providerProfile.collectAsState()

    val appointments by appointmentViewModel.appointments.collectAsState()
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }

    val authState by authViewModel.authState.collectAsState()
    val providerId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId ?: "N/A"
    // Assuming provider's name can be fetched or is known. For now, a placeholder.
    val providerName = providerProfile?.fullName ?: "Loading..."

    var selectedPatient by remember { mutableStateOf<User?>(null) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedTime by remember { mutableStateOf(Calendar.getInstance()) }
    var mode by remember { mutableStateOf("virtual") }
    var notes by remember { mutableStateOf("") }

    val isScheduling by appointmentViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // --- Date and Time Picker Dialogs (The View-based way) ---
    val datePickerDialog = AndroidDatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
        },
        selectedDate.get(Calendar.YEAR),
        selectedDate.get(Calendar.MONTH),
        selectedDate.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = AndroidTimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
            selectedTime.set(Calendar.MINUTE, minute)
        },
        selectedTime.get(Calendar.HOUR_OF_DAY),
        selectedTime.get(Calendar.MINUTE),
        false // false for 24-hour view
    )

    LaunchedEffect(selectedPatient) {
        if (selectedPatient != null) {
            appointmentViewModel.loadAppointmentsForTab(selectedPatient!!.userId, "upcoming")
        }
    }

    // --- Schedule Appointment Action ---
    val scheduleAppointment: () -> Unit = {
        selectedPatient?.let { patient ->
            if (providerId != "N/A" && providerName != "Loading...") {
                val combinedDateTime = (selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
                }

                appointmentViewModel.createAppointment(
                    patientId = patient.userId, // Use uid from User model
                    providerId = providerId,
                    providerName = providerName, // Using a safe placeholder
                    dateTime = Timestamp(combinedDateTime.time),
                    mode = mode,
                    notes = notes
                )
                // Reset form
                selectedPatient = null
                notes = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Schedule New Appointment", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // 1. Patient Selection
        PatientSelectionDropdown(
            patients = patients,
            selectedPatient = selectedPatient,
            onPatientSelected = { selectedPatient = it }
        )
        Spacer(Modifier.height(16.dp))

        if (selectedPatient != null) {
            Text("Upcoming Appointments", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (appointments.isEmpty()) {
                Text(
                    "No upcoming appointments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    appointments.forEach { appt ->
                        ProviderAppointmentCard(
                            appointment = appt,
                            onClick = { selectedAppointment = appt }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Text("Create New / Reschedule", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            //Date Picker
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = dateFormatter.format(selectedDate.time),
                    onValueChange = { /* Read-only */ },
                    label = { Text("Date") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Date") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { datePickerDialog.show() }
                )
            }

            // Time Picker
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
        Spacer(Modifier.height(16.dp))

        // 3. Mode Selection
        Text("Consultation Mode:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = mode == "virtual",
                onClick = { mode = "virtual" },
                label = { Text("Virtual") }
            )
            FilterChip(
                selected = mode == "in_person",
                onClick = { mode = "in_person" },
                label = { Text("In-Person") }
            )
        }
        Spacer(Modifier.height(16.dp))

        // 4. Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Appointment Notes") },
            placeholder = { Text("e.g., Follow-up, Medication review")},
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
        )
        Spacer(Modifier.height(24.dp))

        // 5. Submit Button
        Button(
            onClick = scheduleAppointment,
            enabled = selectedPatient != null && !isScheduling,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isScheduling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("SCHEDULE APPOINTMENT")
            }
        }
    }

    if (selectedAppointment != null) {
        EditDeleteAppointmentDialog(
            appointment = selectedAppointment!!,
            viewModel = appointmentViewModel,
            onDismiss = { selectedAppointment = null }
        )
    }
}

// FIX: Added the missing PatientSelectionDropdown composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientSelectionDropdown(
    patients: List<User>,
    selectedPatient: User?,
    onPatientSelected: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedPatient?.fullName ?: "Select a patient",
            onValueChange = { },
            readOnly = true,
            label = { Text("Patient") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            patients.forEach { patient ->
                DropdownMenuItem(
                    text = { Text(patient.fullName) },
                    onClick = {
                        onPatientSelected(patient)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ProviderAppointmentCard(appointment: Appointment, onClick: () -> Unit) {
    val dateFormatter =
        remember { SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    dateFormatter.format(appointment.appointmentDateTime.toDate()),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    appointment.mode.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun EditDeleteAppointmentDialog(
    appointment: Appointment,
    viewModel: AppointmentViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isSaving by viewModel.isLoading.collectAsState()

    val calendar = Calendar.getInstance().apply { time = appointment.appointmentDateTime.toDate() }
    var newDate by remember { mutableStateOf(calendar) }
    var newTime by remember { mutableStateOf(calendar) }
    var newMode by remember { mutableStateOf(appointment.mode) }
    var newNotes by remember { mutableStateOf(appointment.notes ?: "") }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val datePickerDialog = AndroidDatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            newDate.set(year, month, dayOfMonth)
        },
        newDate.get(Calendar.YEAR),
        newDate.get(Calendar.MONTH),
        newDate.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = AndroidTimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            newTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
            newTime.set(Calendar.MINUTE, minute)
        },
        newTime.get(Calendar.HOUR_OF_DAY),
        newTime.get(Calendar.MINUTE),
        false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Appointment") },
        text = {
            Column {
                Text(
                    "Patient: ${appointment.providerName}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dateFormatter.format(newDate.time),
                            onValueChange = { }, readOnly = true, label = { Text("Date") },
                            modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = timeFormatter.format(newTime.time),
                            onValueChange = { }, readOnly = true, label = { Text("Time") },
                            modifier = Modifier.fillMaxWidth().clickable { timePickerDialog.show() }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = newMode == "virtual",
                        onClick = { newMode = "virtual" },
                        label = { Text("Virtual") }
                    )
                    FilterChip(
                        selected = newMode == "in_person",
                        onClick = { newMode = "in_person" },
                        label = { Text("In-Person") }
                    )
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = newNotes,
                    onValueChange = { newNotes = it },
                    label = { Text("Appointment Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val combinedDateTime = (newDate.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, newTime.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, newTime.get(Calendar.MINUTE))
                    }
                    viewModel.updateAppointment(
                        appointmentId = appointment.appointmentId,
                        newDateTime = Timestamp(combinedDateTime.time),
                        newMode = newMode,
                        newNotes = newNotes
                    )
                    onDismiss()
                },
                enabled = !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("UPDATE")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        viewModel.cancelAppointment(appointment.appointmentId)
                        onDismiss()
                    }
                ) {
                    Text("CANCEL APPT", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDismiss) { Text("CANCEL") }
            }
        }
    )
}