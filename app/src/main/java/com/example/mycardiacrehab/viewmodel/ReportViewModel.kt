package com.example.mycardiacrehab.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
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
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
class ReportViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // This holds the generated report object
    private val _report = MutableStateFlow<PatientReport?>(null)
    val report: StateFlow<PatientReport?> = _report

    // This will show a loading spinner on the UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading // <-- THIS PROPERTY WAS MISSING

    /**
     * Generates a comprehensive report for a specific patient over a given number of days.
     */
    fun generateReport(patientId: String, patientName: String, daysToCover: Int) = viewModelScope.launch {
        if (patientId.isBlank()) return@launch
        _isLoading.value = true
        _report.value = null // Clear the previous report

        try {
            // Define the time range for the report
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(daysToCover.toLong() - 1)
            val startTime = Timestamp(startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)

            val exerciseLogs = db.collection("exerciselog")
                .whereEqualTo("userId", patientId)
                .whereGreaterThan("timestamp", startTime)
                .get().await().toObjects(ExerciseLog::class.java)

            val medicationLogs = db.collection("MedicationReminders")
                .whereEqualTo("userId", patientId)
                .whereGreaterThan("timestamp", startTime)
                .get().await().toObjects(MedicationReminder::class.java)

            val journalEntries = db.collection("patientjournal")
                .whereEqualTo("userId", patientId)
                .whereGreaterThan("entryDate", startTime)
                .get().await().toObjects(JournalEntry::class.java)

            val chatInteractions = db.collection("mycardiacrehab_chat")
                .whereEqualTo("userId", patientId)
                .whereGreaterThan("timestamp", startTime)
                .get().await().toObjects(ChatMessage::class.java)

            val totalMins = exerciseLogs.sumOf { it.duration }
            val adherenceRate = calculateAdherenceRate(medicationLogs)
            val commonSymptom = analyzeSymptoms(journalEntries)
            val totalChats = chatInteractions.count { it.role == "user" }
            val outOfScopeChats = chatInteractions.count { !it.isInScope }

            val weeklyExerciseTarget = 150
            val complianceRate = ((totalMins.toDouble() / weeklyExerciseTarget) * 100).roundToInt().coerceAtMost(100)

            _report.value = PatientReport(
                patientId = patientId,
                patientName = patientName,
                dateGenerated = LocalDate.now(),
                startDate = startDate,
                endDate = endDate,
                totalExerciseMinutes = totalMins,
                exerciseComplianceRate = complianceRate,
                medicationAdherenceRate = adherenceRate,
                mostCommonSymptoms = commonSymptom,
                totalChatInteractions = totalChats,
                outOfScopeInteractions = outOfScopeChats
            )

        } catch (e: Exception) {
            println("Error generating report: ${e.message}")
            _report.value = null
        } finally {
            _isLoading.value = false
        }
    }

    private fun calculateAdherenceRate(logs: List<MedicationReminder>): Int {
        val totalDoses = logs.size
        if (totalDoses == 0) return 100
        val dosesTaken = logs.count { it.reminderStatus == "Taken" }
        return (dosesTaken.toDouble() / totalDoses * 100).roundToInt()
    }

    private fun analyzeSymptoms(entries: List<JournalEntry>): String {
        val allSymptoms = entries
            .mapNotNull { it.symptoms }
            .filter { it.isNotBlank() }
            .flatMap { it.split(",", ";") }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (allSymptoms.isEmpty()) {
            return "None Reported"
        }

        return allSymptoms
            .groupBy { it }
            .mapValues { it.value.size }
            .maxByOrNull { it.value }?.key
            ?.replaceFirstChar { it.uppercase() } ?: "None Reported"
    }
}