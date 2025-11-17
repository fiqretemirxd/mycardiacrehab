package com.example.mycardiacrehab.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.model.ExerciseLog
import com.example.mycardiacrehab.model.JournalEntry
import com.example.mycardiacrehab.model.MedicationReminder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class WeeklySummary(
    val weekStart: String,
    val totalExerciseMinutes: Int,
    val adherenceRate: Int,
    val moodTrend: String,
    val dailyMins: List<Pair<String, Int>>
)

class ProgressViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _weeklySummary = MutableStateFlow<WeeklySummary?>(null)
    val weeklySummary: StateFlow<WeeklySummary?> = _weeklySummary

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    var userId: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadWeeklyProgress(currentUserId: String) = viewModelScope.launch {
        if (currentUserId.isBlank()) return@launch
        userId = currentUserId
        _loading.value = true

        try {
            val sevenDaysAgo =
                Timestamp(System.currentTimeMillis() / 1000 - TimeUnit.DAYS.toSeconds(7), 0)

            val exerciseDocs = db.collection("exerciselog")
                .whereEqualTo("userId", currentUserId)
                .whereGreaterThan("timestamp", sevenDaysAgo)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get().await()
            val medicationDocs = db.collection("MedicationReminders")
                .whereEqualTo("userId", currentUserId)
                .whereGreaterThan("timestamp", sevenDaysAgo)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get().await()
            val journalDocs = db.collection("patientjournal")
                .whereEqualTo("userId", currentUserId)
                .whereGreaterThan("entryDate", sevenDaysAgo)
                .orderBy("entryDate", Query.Direction.ASCENDING)
                .get().await()

            val exerciseLogs = exerciseDocs.toObjects(ExerciseLog::class.java)
            val medicationLogs = medicationDocs.toObjects(MedicationReminder::class.java)
            val journalEntries = journalDocs.toObjects(JournalEntry::class.java)

            val summary = analyzeData(exerciseLogs, medicationLogs, journalEntries)
            _weeklySummary.value = summary

        } catch (e: Exception) {
            println("Error loading progress: ${e.message}")
        } finally {
            _loading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun analyzeData(
        exerciseLogs: List<ExerciseLog>,
        medicationLogs: List<MedicationReminder>,
        journalEntries: List<JournalEntry>
    ): WeeklySummary {

        val dailyMinsMap = exerciseLogs
            .groupBy {
                Instant.ofEpochSecond(it.timestamp.seconds).atZone(ZoneId.systemDefault()).dayOfWeek
            }
            .mapValues { (_, logs) -> logs.sumOf { it.duration } }

        val dayOrder = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        val dailyMins = dayOrder.map { day ->
            Pair(day.name.take(1), dailyMinsMap[day] ?: 0)
        }

        val totalMins = exerciseLogs.sumOf { it.duration }

        val totalDoses = medicationLogs.size
        val dosesTaken = medicationLogs.count { it.reminderStatus == "Taken" }
        val adherenceRate =
            if (totalDoses > 0) (dosesTaken.toDouble() / totalDoses * 100).roundToInt() else 100

        val positiveMoods = listOf("Happy", "Neutral")
        val moodCounts = journalEntries.count { it.mood in positiveMoods }
        val moodTrend = if (journalEntries.isEmpty()) "No data"
        else if (moodCounts.toDouble() / journalEntries.size > 0.6) "Good"
        else "Needs Attention"

        return WeeklySummary(
            weekStart = "Last 7 Days",
            totalExerciseMinutes = totalMins,
            adherenceRate = adherenceRate,
            moodTrend = moodTrend,
            dailyMins = dailyMins
        )
    }
}