package com.example.mycardiacrehab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.Appointment
import com.example.mycardiacrehab.viewmodel.AppointmentViewModel
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppointmentScreen(
    authViewModel: AuthViewModel,
    appointmentViewModel: AppointmentViewModel
) {
    // Get current user's ID
    val authState = authViewModel.authState.collectAsState().value
    val currentUserId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId ?: return

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Past", "Cancelled")

    // This effect now re-runs whenever the user ID OR the selected tab index changes
    LaunchedEffect(currentUserId, selectedTabIndex) {
        val category = tabs[selectedTabIndex].lowercase(Locale.ROOT)
        appointmentViewModel.loadAppointments(currentUserId, category)
    }

    // Observe the single list and the loading state from the ViewModel
    val currentList by appointmentViewModel.appointments.collectAsState()
    val isLoading by appointmentViewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "My Appointments",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            // Show loading indicator
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Display the list once loading is complete
            AppointmentList(
                appointments = currentList,
                emptyMessage = "No appointments in ${tabs[selectedTabIndex]}.",
                viewModel = appointmentViewModel
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// LIST + CARD
// -------------------------------------------------------------------------------------------------

@Composable
private fun AppointmentList(
    appointments: List<Appointment>,
    emptyMessage: String,
    viewModel: AppointmentViewModel
) {
    if (appointments.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emptyMessage, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(appointments) { appointment ->
                AppointmentCard(appointment = appointment, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AppointmentCard(appointment: Appointment, viewModel: AppointmentViewModel) {
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy 'at' hh:mm a", Locale.getDefault()) }

    val isUpcoming =
        appointment.status.equals("scheduled", ignoreCase = true) &&
                appointment.appointmentDateTime.toDate().time > System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUpcoming)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = appointment.providerName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (appointment.mode == "virtual")
                            Icons.Default.Videocam else Icons.Default.CalendarMonth,
                        contentDescription = appointment.mode,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        appointment.mode.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = dateFormatter.format(appointment.appointmentDateTime.toDate()),
                style = MaterialTheme.typography.bodyLarge
            )

            if (!appointment.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${appointment.notes}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Cancel button for upcoming scheduled appointments
            if (isUpcoming) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.cancelAppointment(appointment.appointmentId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("CANCEL APPOINTMENT")
                }
            }
        }
    }
}
