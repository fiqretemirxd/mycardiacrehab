package com.example.mycardiacrehab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.ExerciseLog
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.ExerciseViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExerciseScreen(
    authViewModel: AuthViewModel = viewModel(),
    exerciseViewModel: ExerciseViewModel = viewModel()
) {
    val authState = authViewModel.authState.collectAsState().value
    val currentUserId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId ?: return

    LaunchedEffect(currentUserId) {
        exerciseViewModel.loadExerciseHistory(currentUserId)
    }

    val logs by exerciseViewModel.logs.collectAsState()
    val isLoading by exerciseViewModel.loading.collectAsState()

    var type by remember { mutableStateOf("Walk") }
    var duration by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf("Low") }

    val totalDuration = logs.sumOf { it.duration }
    val dateFormatter = remember {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Exercise Tracking",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Total Minutes Logged", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$totalDuration min",
                    style = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onTertiaryContainer)
                )
                Text(
                    "Keep pushing forward with your cardiac rehab!",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Log New Activity", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                val exerciseTypes = listOf("Walk", "Cycle", "Stretching", "Light Resistance")

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { char -> char.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                val intensities = listOf("Low", "Medium", "High")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Type:", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    DropdownMenuSelector(
                        options = exerciseTypes,
                        selectedOption = type,
                        onOptionSelected = { type = it }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Intensity:", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    DropdownMenuSelector(
                        options = intensities,
                        selectedOption = intensity,
                        onOptionSelected = { intensity = it }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val durationInt = duration.toIntOrNull()
                        if (durationInt != null && durationInt > 0) {
                            exerciseViewModel.logExercise(currentUserId, type, durationInt, intensity)
                            duration = ""
                        }
                    },
                    enabled = !isLoading && duration.toIntOrNull() != null && duration.toIntOrNull() !! > 0,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("LOG ACTIVITY")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Recent Activity Logs", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues (vertical = 8.dp)
        ) {
            items(logs) { log ->
                LogItemCard(log = log, dateFormatter = dateFormatter)
            }
            if (logs.isEmpty() && !isLoading) {
                item {
                    Text(
                        "No activities logged yet.",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownMenuSelector(
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

@Composable
fun LogItemCard(log: ExerciseLog, dateFormatter: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${log.exerciseType} (${log.duration} min)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Intensity: ${log.intensity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (log.intensity) {
                        "High" -> MaterialTheme.colorScheme.error
                        "Medium" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            Text(
                text = dateFormatter.format(log.timestamp.toDate()),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}