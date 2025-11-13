package com.example.mycardiacrehab.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.JournalEntry
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(
    authViewModel: AuthViewModel = viewModel(),
    journalViewModel: JournalViewModel = viewModel()
) {
    val authState = authViewModel.authState.collectAsState().value
    val currentUserId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId ?: return

    LaunchedEffect(currentUserId) {
        journalViewModel.loadJournalHistory(currentUserId)
    }

    val entries by journalViewModel.entries.collectAsState()
    val isLoading by journalViewModel.loading.collectAsState()
    var isLoggingVisible by remember { mutableStateOf(false) }

    // State for Edit/Delete Dialog
    var selectedEntry by remember { mutableStateOf<JournalEntry?>(null) }
    val showEditDialog = selectedEntry != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Patient Journal",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            FloatingActionButton(
                onClick = { isLoggingVisible = !isLoggingVisible },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Entry")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = isLoggingVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            JournalEntryForm(
                viewModel = journalViewModel,
                currentUserId = currentUserId,
                onEntryLogged = { isLoggingVisible = false },
                isSaving = isLoading
            )
        }

        Text("Past Entries (Tap to Edit)", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries) { entry ->
                // Passes the onClick lambda to trigger edit
                JournalEntryCard(
                    entry = entry,
                    onClick = { selectedEntry = entry }
                )
            }
            if (entries.isEmpty() && !isLoading) {
                item { Text("No entries yet. Click '+' to log a first entry.", color = MaterialTheme.colorScheme.outline) }
            }
        }
    }

    // Dialog logic
    if (showEditDialog && selectedEntry != null) {
        EditDeleteJournalDialog(
            entry = selectedEntry!!,
            viewModel = journalViewModel,
            onDismiss = { selectedEntry = null }
        )
    }
}

@Composable
fun JournalEntryForm(viewModel: JournalViewModel, currentUserId: String, onEntryLogged: () -> Unit, isSaving: Boolean) {
    var mood by remember { mutableStateOf("Neutral") }
    var symptoms by remember { mutableStateOf("") }
    var freeText by remember { mutableStateOf("") }

    val moods = listOf("Happy", "Neutral", "Tired", "Anxious", "Stressed", "Sad")

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("How are you feeling today?", style = MaterialTheme.typography.titleMedium)

            // Use the renamed private function
            JournalDropdownMenuSelector(options = moods, selectedOption = mood, onOptionSelected = { mood = it })
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = symptoms,
                onValueChange = { symptoms = it },
                label = { Text("Symptoms (eg: Slight fatigue, chest tightness)") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = freeText,
                onValueChange = { freeText = it },
                label = { Text("General Thoughts/Notes") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.logEntry(currentUserId, mood, symptoms, freeText)
                    onEntryLogged()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("SAVE ENTRY")
                }
            }
        }
    }
}

@Composable
fun JournalEntryCard(entry: JournalEntry, onClick: (JournalEntry) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(entry) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatter.format(entry.entryDate.toDate()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.mood, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 8.dp)) {
                    if (entry.symptoms?.isNotBlank() == true) {
                        HorizontalDivider(Modifier.padding(bottom = 4.dp))
                        Text(
                            "Symptoms:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary)
                        )
                        Text(entry.symptoms, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (entry.freeTextEntry.isNotBlank()) {
                        HorizontalDivider(Modifier.padding(top = 4.dp, bottom = 4.dp))
                        Text("Notes:", style = MaterialTheme.typography.bodySmall)
                        Text(entry.freeTextEntry, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun EditDeleteJournalDialog(
    entry: JournalEntry,
    viewModel: JournalViewModel,
    onDismiss: () -> Unit
) {
    var newMood by remember { mutableStateOf(entry.mood) }
    var newSymptoms by remember { mutableStateOf(entry.symptoms ?: "") }
    var newFreeText by remember { mutableStateOf(entry.freeTextEntry) }

    val moods = listOf("Happy", "Neutral", "Tired", "Anxious", "Stressed", "Sad")
    val isSaving by viewModel.loading.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Journal Entry") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Text("Entry on ${dateFormatter.format(entry.entryDate.toDate())}",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))

                Text("How are you feeling?", style = MaterialTheme.typography.bodyMedium)
                // Use the renamed private function
                JournalDropdownMenuSelector(options = moods, selectedOption = newMood, onOptionSelected = { newMood = it })
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = newSymptoms,
                    onValueChange = { newSymptoms = it },
                    label = { Text("Symptoms") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = newFreeText,
                    onValueChange = { newFreeText = it },
                    label = { Text("General Notes") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateEntry(entry.id, newMood, newSymptoms, newFreeText)
                    onDismiss()
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("SAVE CHANGES")
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry.id)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("DELETE", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDismiss) {
                    Text("CANCEL")
                }
            }
        }
    )
}

// 🟢 RENAMED AND PRIVATE to avoid conflicting with ExerciseScreen.kt
@Composable
private fun JournalDropdownMenuSelector(
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
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}