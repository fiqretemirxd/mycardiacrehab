package com.example.mycardiacrehab.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.*
import com.example.mycardiacrehab.viewmodel.ChatbotViewModel
import com.example.mycardiacrehab.viewmodel.ExerciseViewModel
import com.example.mycardiacrehab.viewmodel.JournalViewModel
import com.example.mycardiacrehab.viewmodel.MedicationViewModel
import com.example.mycardiacrehab.viewmodel.ProviderViewModel
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PatientDetailScreen(
    patientId: String,
    providerViewModel: ProviderViewModel = viewModel(),
    exerciseViewModel: ExerciseViewModel = viewModel(),
    medicationViewModel: MedicationViewModel = viewModel(),
    journalViewModel: JournalViewModel = viewModel(),
    chatbotViewModel: ChatbotViewModel = viewModel()
) {
    var showArchiveDialog by remember { mutableStateOf(false) }

    // Load all data streams for the specific patient ID
    LaunchedEffect(patientId) {
        providerViewModel.loadUserProfile(patientId)
        exerciseViewModel.loadExerciseHistory(patientId)
        medicationViewModel.loadDailySchedule(patientId)
        journalViewModel.loadJournalHistory(patientId)
        chatbotViewModel.loadChatHistory(patientId)
    }

    val patientProfile by providerViewModel.currentPatientProfile.collectAsState()
    val isArchiving by providerViewModel.loading.collectAsState()

    val exerciseLogs by exerciseViewModel.logs.collectAsState()
    val medicationSchedule by medicationViewModel.dailySchedule.collectAsState()
    val journalEntries by journalViewModel.entries.collectAsState()
    val chatMessages by chatbotViewModel.messages.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        // Display name from the profile object
                        patientProfile?.fullName ?: "Loading Patient...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Detailed Rehabilitation Dashboard", style = MaterialTheme.typography.titleMedium)
                }

                IconButton(
                    onClick = { showArchiveDialog = true },
                    enabled = !isArchiving && patientProfile != null
                ) {
                    Icon(Icons.Default.Archive, contentDescription = "Archive Patient", tint = MaterialTheme.colorScheme.error)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }

        // --- 1. Medication Adherence Summary ---
        item {
            DataSectionTitle("Medication Adherence (Last 10 Doses)")
            MedicationSummary(medicationSchedule)
        }

        // --- 2. Recent Exercise Logs ---
        item {
            DataSectionTitle("Recent Exercise Logs")
            ExerciseSummary(exerciseLogs.take(5))
        }

        // --- 3. Recent Journal Entries ---
        item {
            DataSectionTitle("Recent Patient Journal Entries")
            JournalSummary(journalEntries.take(3))
        }

        // --- 4. Chatbot Interaction Log (F04-3) ---
        item {
            DataSectionTitle("Chatbot Interaction Logs (Safety Audit)")
            ChatLogSummary(chatMessages.takeLast(5))
        }
    }

    if (showArchiveDialog && patientProfile != null) {
        ArchiveConfirmationDialog(
            patientName = patientProfile!!.fullName,
            patientId = patientId,
            viewModel = providerViewModel,
            onDismiss = { showArchiveDialog = false }
        )
    }
}

@Composable
fun ArchiveConfirmationDialog(
    patientName: String,
    patientId: String,
    viewModel: ProviderViewModel,
    onDismiss: () -> Unit
) {
    val isArchiving by viewModel.loading.collectAsState()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Archive Patient: $patientName") },
        text = {
            Text(
                "Are you sure you want to archive this patient? " +
                        "This action will remove the patient from your active list " +
                        "and preserve their historical data."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.archivePatient(patientId)
                    onDismiss()
                    Toast.makeText(
                        context,
                        "$patientName archived successfully.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                enabled = !isArchiving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isArchiving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text(
                    "CONFIRM ARCHIVE"
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun DataSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun MedicationSummary(schedule: List<MedicationReminder>) {
    val taken = schedule.count { it.reminderStatus == "Taken" }
    val total = schedule.size
    val rate = if (total > 0) (taken.toFloat() / total * 100).toInt() else 0

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Overall Adherence Rate: $rate%", style = MaterialTheme.typography.titleMedium)

            // Show recent status
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                schedule.take(5).forEach { med ->
                    Text(
                        med.reminderStatus.take(1),
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                when (med.reminderStatus) {
                                    "Taken" -> Color.Green.copy(alpha = 0.8f)
                                    "Missed" -> Color.Red.copy(alpha = 0.8f)
                                    else -> Color.Gray
                                }, shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(4.dp)
                    )
                }
                Text("...${total} total doses logged.")
            }
        }
    }
}

@Composable
fun ExerciseSummary(logs: List<ExerciseLog>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (logs.isEmpty()) {
            Text("No exercise logs found.")
        } else {
            logs.forEach { log ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${log.exerciseType} for ${log.duration} mins", style = MaterialTheme.typography.bodyLarge)
                        Text("Intensity: ${log.intensity}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun JournalSummary(entries: List<JournalEntry>) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (entries.isEmpty()) {
            Text("No journal entries found.")
        } else {
            entries.forEach { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(dateFormatter.format(entry.entryDate.toDate()), style = MaterialTheme.typography.titleSmall)
                        if (entry.symptoms?.isNotBlank() == true) {
                            Text("Symptoms: ${entry.symptoms}", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
                        }
                        if (entry.freeTextEntry.isNotBlank()) {
                            Text("Notes: ${entry.freeTextEntry}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatLogSummary(messages: List<ChatMessage>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        messages.forEach { message ->
            val isUser = message.role == "user"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    val statusText = if (message.isInScope) "In Scope" else " Out of Scope"
                    Text(
                        "${message.role.uppercase()} (${statusText}):",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(message.text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}