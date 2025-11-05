package com.example.mycardiacrehab.viewmodel

import android.os.Build // Import Build
import androidx.annotation.RequiresApi // Import RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
class ReportViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _report = MutableStateFlow<PatientReport?>(null)
    val report: StateFlow<PatientReport?> = _report

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // --- Core Logic: Generate Comprehensive Report (F04-2) ---
    fun generateReport(patientId: String, patientName: String, days: Int) = viewModelScope.launch {
        if (patientId.isBlank()) return@launch
        _loading.value = true

        try {
            // ✨ 3. Define the start and end dates using the modern LocalDate API
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(days.toLong() - 1)
            val startTime = Timestamp(startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)

            // 1. Fetch ALL Data Streams
            val exerciseLogs = db.collection("exerciselog").whereEqualTo("userId", patientId)
                .whereGreaterThan("timestamp", startTime).get().await().toObjects(ExerciseLog::class.java)
            val medicationLogs = db.collection("medicationreminders").whereEqualTo("userId", patientId)
                .whereGreaterThan("logDate", startTime).get().await().toObjects(MedicationReminder::class.java)
            val journalEntries = db.collection("patientjournal").whereEqualTo("userId", patientId)
                .whereGreaterThan("entryDate", startTime).get().await().toObjects(JournalEntry::class.java)
            val chatInteractions = db.collection("mycardiacrehab_chat").whereEqualTo("userId", patientId)
                .whereGreaterThan("timestamp", startTime).get().await().toObjects(ChatMessage::class.java)

            // 2. Aggregate Metrics
            val totalMins = exerciseLogs.sumOf { it.duration }
            val adherenceRate = calculateAdherenceRate(medicationLogs)
            val symptom = analyzeSymptoms(journalEntries)
            val outOfScopeCount = chatInteractions.count { !it.isInScope }
            val totalChats = chatInteractions.count { it.role == "user" }

            val complianceRate = if (totalMins > 150 * (days/7.0)) 100 else (totalMins / (150 * (days/7.0).toFloat()) * 100).roundToInt()

            // ✨ 4. Call the new PatientReport constructor with the correct parameters
            _report.value = PatientReport(
                patientName = patientName,
                startDate = startDate,
                endDate = endDate,
                dateGenerated = LocalDate.now(), // Use today's date
                totalExerciseMinutes = totalMins,
                exerciseComplianceRate = complianceRate,
                medicationAdherenceRate = adherenceRate,
                mostCommonSymptoms = symptom,
                totalChatInteractions = totalChats,
                outOfScopeInteractions = outOfScopeCount
            )

        } catch (e: Exception) {
            println("Error generating report: ${e.message}")
            _report.value = null
        } finally {
            _loading.value = false
        }
    }

    // Helper to calculate Medication Adherence
    private fun calculateAdherenceRate(logs: List<MedicationReminder>): Int {
        val relevantLogs = logs.filter { it.reminderStatus == "Taken" || it.reminderStatus == "Missed" }
        val totalDoses = relevantLogs.size
        if (totalDoses == 0) return 100 // If no doses were scheduled, adherence is 100%
        val dosesTaken = relevantLogs.count { it.reminderStatus == "Taken" }
        return (dosesTaken.toDouble() / totalDoses * 100).roundToInt()
    }

    // Helper to analyze Symptoms
    private fun analyzeSymptoms(entries: List<JournalEntry>): String {
        return entries.mapNotNull { it.symptoms }.flatMap { it.split(",", ";") }
            .map { it.trim().lowercase() } // Use lowercase to group similar symptoms
            .filter { it.isNotBlank() }
            .groupBy { it }
            .maxByOrNull { it.value.size }?.key?.replaceFirstChar { it.uppercase() } ?: "None reported."
    }
}
