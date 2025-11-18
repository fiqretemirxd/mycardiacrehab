package com.example.mycardiacrehab.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.MedicationReminder
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.MedicationViewModel

@Suppress("Unused")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MedicationScreen(
    authViewModel: AuthViewModel = viewModel(),
    medicationViewModel: MedicationViewModel = viewModel()
) {
    val authState = authViewModel.authState.collectAsState().value
    val currentUserId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId ?: return

    LaunchedEffect(currentUserId) {
        medicationViewModel.loadDailySchedule(currentUserId)
    }

    val schedule by medicationViewModel.dailySchedule.collectAsState()
    val adherenceRate by medicationViewModel.adherenceRate.collectAsState()
    val isLoading by medicationViewModel.loading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Your Medication Plan",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Today's Adherence Rate", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$adherenceRate%",
                    style = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )
                LinearProgressIndicator(
                    progress = { adherenceRate / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "Stay on schedule for better heart health.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Today's Medication", style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(schedule) { medication ->
                MedicationItemCard(
                    medication = medication,
                    viewModel = medicationViewModel,
                    isSaving = isLoading
                )
            }
            item {
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {/* TODO: navigation to add/edit medication screen (Provider/Admin feature) */ },
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Medication")
                    Spacer(Modifier.width(8.dp))
                    Text("Add Medication (Admin Feature)")
                }
            }
        }
    }
}

@Composable
fun MedicationItemCard(medication: MedicationReminder, viewModel: MedicationViewModel, isSaving: Boolean) {
    val status = medication.reminderStatus

    val statusColor = when (status) {
        "Taken" -> Color(0xFF4C4F50)
        "Missed" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = medication.medicationName,
                    style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "${medication.dosage} | ${medication.timeOfDay} | $status",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Text(
                text = status.uppercase(),
                color = Color.White,
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp)
                    .background(statusColor, shape = MaterialTheme.shapes.small)
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = { viewModel.updateStatus(medication, "Taken") },
                enabled = status != "Taken" && !isSaving,
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Mark Taken")
            }

            IconButton(
                onClick = { viewModel.updateStatus(medication, "Missed") },
                enabled = status != "Missed" && !isSaving,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Mark Missed")
            }
        }
    }
}
