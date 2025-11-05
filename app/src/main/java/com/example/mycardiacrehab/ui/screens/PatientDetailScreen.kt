package com.example.mycardiacrehab.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.* // Import all data models
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
    // Load all data streams for the specific patient ID
    LaunchedEffect(patientId) {
        exerciseViewModel.loadExerciseHistory(patientId)
        medicationViewModel.loadDailySchedule(patientId)
        journalViewModel.loadJournalHistory(patientId)
        chatbotViewModel.loadChatHistory(patientId)
    }

    // 🟢 FIX: Collect all data states using 'by' delegation for correct usage
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
            Text("Patient ID: $patientId", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("Detailed Rehabilitation Dashboard", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }

        // --- 1. Medication Adherence Summary ---
        item {
            DataSectionTitle("Medication Adherence (Last 10 Doses)")
            // 🟢 FIX: Pass the correctly observed state list
            MedicationSummary(medicationSchedule)
        }

        // --- 2. Recent Exercise Logs ---
        item {
            DataSectionTitle("Recent Exercise Logs")
            // 🟢 FIX: Pass the correctly observed state list
            ExerciseSummary(exerciseLogs.take(5))
        }

        // --- 3. Recent Journal Entries ---
        item {
            DataSectionTitle("Recent Patient Journal Entries")
            // 🟢 FIX: Pass the correctly observed state list
            JournalSummary(journalEntries.take(3))
        }

        // --- 4. Chatbot Interaction Log (F04-3) ---
        item {
            DataSectionTitle("Chatbot Interaction Logs (Safety Audit)")
            // 🟢 FIX: Pass the correctly observed state list
            ChatLogSummary(chatMessages.takeLast(5))
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Data Visualization Helpers
// -------------------------------------------------------------------------------------------------
// ... (All helper functions remain the same as they were correctly defined outside the error zone) ...

@Composable
fun DataSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun MedicationSummary(schedule: List<MedicationReminder>) { // Parameter type is correct
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
fun ExerciseSummary(logs: List<ExerciseLog>) { // Parameter type is correct
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
fun JournalSummary(entries: List<JournalEntry>) { // Parameter type is correct
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
fun ChatLogSummary(messages: List<ChatMessage>) { // Parameter type is correct
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