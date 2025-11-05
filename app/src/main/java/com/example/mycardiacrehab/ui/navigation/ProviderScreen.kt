package com.example.mycardiacrehab.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// Defines the tabs for the Provider Dashboard
sealed class ProviderScreen(val route: String, val title: String, val icon: ImageVector) {
    object PatientList : ProviderScreen("patient_list", "Patients", Icons.Default.Group)
    object Appointments : ProviderScreen("appointments_provider", "Schedule", Icons.Default.Schedule)
    object ReportCenter : ProviderScreen("report_center", "Reports", Icons.AutoMirrored.Filled.ListAlt)
    object Settings : ProviderScreen("settings_provider", "Settings", Icons.Default.Settings)

    object Medication : ProviderScreen("medication", "Meds Plan", Icons.Default.MedicalServices)

    object PatientDetail : ProviderScreen("patient_detail/{patientId}", "Patient Details", Icons.Default.Group)

    fun createRoute(patientId: String) = "patient_detail/$patientId"
}
