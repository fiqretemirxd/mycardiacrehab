package com.example.mycardiacrehab.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mycardiacrehab.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // TIMER: Wait for a shorter duration for a better user experience
    LaunchedEffect(Unit) {
        delay(3000) // 3 seconds is usually enough
        onTimeout()
    }

    // --- 1. Define your custom color ---
    val primaryTeal = Color(0xFF00695C) // Dark Teal Background

    Column(
        modifier = Modifier
            .fillMaxSize()
            // --- 2. Use your custom color for the background ---
            .background(primaryTeal)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Logo Display ---
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "MyCardiacRehab Logo",
            modifier = Modifier.size(150.dp)
        )
        Spacer(Modifier.height(32.dp))

        // --- Title ---
        Text(
            text = "MyCardiacRehab",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(Modifier.height(8.dp))

        // --- Tagline ---
        Text(
            text = "AI-Powered Remote Rehabilitation",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(64.dp))

        // --- Loading Indicator ---
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
    }
}
