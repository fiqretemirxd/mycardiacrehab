package com.example.mycardiacrehab.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class PatientScreen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : PatientScreen("dashboard", "Home", Icons.Default.Home)
    object Exercise : PatientScreen("exercise", "Exercise", Icons.Default.RunCircle)
    object Medication : PatientScreen("medication", "Meds", Icons.Default.Schedule)
    object Chatbot : PatientScreen("chatbot", "AI Chat", Icons.Default.SmartToy)
    // 🟢 NEW: Consolidated "More" Tab
    object More : PatientScreen("more", "More", Icons.Default.MoreHoriz)

    // Keep these defined for potential deep linking or direct access later
    object Progress : PatientScreen("progress", "Progress", Icons.Filled.TrendingUp)
    object Journal : PatientScreen("journal", "Journal", Icons.Default.Book)
    object Appointments : PatientScreen("appointments", "Appointments", Icons.Default.CalendarMonth)
}

