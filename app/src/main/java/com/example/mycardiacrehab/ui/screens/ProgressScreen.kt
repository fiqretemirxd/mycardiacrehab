package com.example.mycardiacrehab.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.ProgressViewModel
import com.example.mycardiacrehab.viewmodel.WeeklySummary

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProgressScreen(
    authViewModel: AuthViewModel = viewModel(),
    progressViewModel: ProgressViewModel = viewModel()
) {
    val authState = authViewModel.authState.collectAsState().value
    val currentUserId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId ?: return

    LaunchedEffect(currentUserId) {
        progressViewModel.loadWeeklyProgress(currentUserId)
    }

    val summary by progressViewModel.weeklySummary.collectAsState()
    val isLoading by progressViewModel.loading.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                "Your Rehabilitation Progress",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Summary for ${summary?.weekStart ?: "Loading..."}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(Modifier.padding(32.dp))
            } else if (summary != null) {
                SummaryCardsRow(summary!!)
                Spacer(Modifier.height(24.dp))

                Text(
                    "Weekly Exercise Minutes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                WeeklyBarChart(summary!!.dailyMins)
                Spacer(Modifier.height(24.dp))

                ReportActions()
            } else {
                Text(
                    "No recent progress data available",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryCardsRow(summary: WeeklySummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            title = "Exercise Mins",
            value = "${summary.totalExerciseMinutes} ",
            subtitle = "Last 7 Days",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.tertiary
        )
        SummaryCard(
            title = "Meds Adherence",
            value = "${summary.adherenceRate}%",
            subtitle = "Weekly Goal",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.secondary
        )
    }
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            title = "Mood Trend",
            value = summary.moodTrend,
            subtitle = "Journal Status",
            modifier = Modifier.weight(1f),
            color = if (summary.moodTrend == "Good") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun SummaryCard(title: String, value: String, subtitle: String, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(color = color))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WeeklyBarChart(dailyMins: List<Pair<String, Int>>) {
    //val totalMinutes = dailyMins.sumOf { it.second }
    val maxMins = dailyMins.maxOfOrNull { it.second } ?: 1
    val scaleFactor = 1f / maxMins

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            dailyMins.forEach { (day, mins) ->
                val barHeightFraction = mins.toFloat() * scaleFactor

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = mins.toString(), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(150.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(barHeightFraction)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(day, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ReportActions() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Generate PDF Report",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Create a detailed progress report to share with your healthcare provider.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = { /* TODO: Implement PDF generation logic */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download Report")
                Spacer(Modifier.width(8.dp))
                Text("DOWNLOAD REPORT")
            }
        }
    }
}