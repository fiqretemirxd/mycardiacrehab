package com.example.mycardiacrehab.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
// --- THIS IMPORT FIXES YOUR ERRORS ---
import androidx.compose.runtime.collectAsState
// -------------------------------------
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.ProgressViewModel
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProgressScreen(
    authViewModel: AuthViewModel = viewModel(),
    progressViewModel: ProgressViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    // These lines will now work because of the import above
    val summary by progressViewModel.weeklySummary.collectAsState()
    val isLoading by progressViewModel.loading.collectAsState()

    val currentUserId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId

    // Load progress when the user ID is available
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            progressViewModel.loadWeeklyProgress(currentUserId)
        }
    }

    // --- Target values for the charts ---
    val exerciseTarget = 150f
    val adherenceTarget = 100f

    // Calculate percentages
    val exercisePercentage = (summary?.totalExerciseMinutes ?: 0) / exerciseTarget
    val adherencePercentage = (summary?.adherenceRate ?: 0) / adherenceTarget

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Weekly Progress",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 64.dp))
        } else {
            // --- Exercise Progress Chart ---
            Text("Weekly Exercise", style = MaterialTheme.typography.titleLarge)
            Text(
                "Target: ${exerciseTarget.roundToInt()} minutes",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(16.dp))
            ProgressDonutChart(
                percentage = exercisePercentage,
                value = summary?.totalExerciseMinutes ?: 0,
                unit = "min",
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider() // Use HorizontalDivider
            Spacer(Modifier.height(32.dp))

            // --- Medication Adherence Chart ---
            Text("Medication Adherence", style = MaterialTheme.typography.titleLarge)
            Text(
                "Target: ${adherenceTarget.roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(16.dp))
            ProgressDonutChart(
                percentage = adherencePercentage,
                value = summary?.adherenceRate ?: 0,
                unit = "%",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

/**
 * A custom composable for a circular progress (donut) chart.
 */
@Composable
fun ProgressDonutChart(
    percentage: Float,
    value: Int,
    unit: String,
    color: Color,
    strokeWidth: Dp = 16.dp,
    animationDuration: Int = 1000,
    size: Dp = 200.dp
) {
    var animationPlayed by remember { mutableStateOf(false) }

    val currentPercentage = animateFloatAsState(
        targetValue = if (animationPlayed) percentage else 0f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "progressAnimation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        // Background track
        Canvas(modifier = Modifier.size(size)) {
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        // Foreground progress
        Canvas(modifier = Modifier.size(size)) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360 * currentPercentage.value,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        // Text in the center
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = unit,
                fontSize = 20.sp,
                color = Color.Gray
            )
        }
    }
}