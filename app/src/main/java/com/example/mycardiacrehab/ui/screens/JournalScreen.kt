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

        Text("Past Entries", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries) { entry ->
                JournalEntryCard(entry = entry)
            }
            if (entries.isEmpty() && !isLoading) {
                item { Text("No entries yet. Click '+' to log a first entry.", color = MaterialTheme.colorScheme.outline) }
            }
        }
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

            DropdownMenuSelector(options = moods, selectedOption = mood, onOptionSelected = { mood = it })
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
fun JournalEntryCard(entry: JournalEntry) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
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
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
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





















