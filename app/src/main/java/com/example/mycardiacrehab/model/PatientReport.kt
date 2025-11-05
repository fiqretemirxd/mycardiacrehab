package com.example.mycardiacrehab.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

/**
 * Represents a summarized report for a patient over a specific date range.
 *
 * This data class is designed to be more robust by using specific date types
 * instead of generic strings for periods.
 *
 * @param patientId The unique ID of the patient this report is for.
 * @param patientName The full name of the patient.
 * @param dateGenerated The date when this report was created.
 * @param startDate The beginning of the reporting period (inclusive).
 * @param endDate The end of the reporting period (inclusive).
 * @param totalExerciseMinutes Total minutes of exercise logged during the period.
 * @param exerciseComplianceRate Percentage of prescribed exercises completed.
 * @param medicationAdherenceRate Percentage of medication doses taken as scheduled.
 * @param mostCommonSymptoms A summary string of the most frequently logged symptoms.
 * @param totalChatInteractions The total number of messages exchanged with the chatbot.
 * @param outOfScopeInteractions The number of times the chatbot flagged a query as out of scope.
 */

@RequiresApi(Build.VERSION_CODES.O)
data class PatientReport(
    val patientId: String = "", // It's good practice to have the ID
    val patientName: String = "",
    val dateGenerated: LocalDate = LocalDate.now(), // Using the modern LocalDate
    val startDate: LocalDate, // More precise than a string
    val endDate: LocalDate,   // More precise than a string
    val totalExerciseMinutes: Int = 0,
    val exerciseComplianceRate: Int = 0,
    val medicationAdherenceRate: Int = 0,
    val mostCommonSymptoms: String = "None",
    val totalChatInteractions: Int = 0,
    val outOfScopeInteractions: Int = 0
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this(
        "",
        "",
        LocalDate.now(),
        LocalDate.now(),
        LocalDate.now(),
        0,
        0,
        0,
        "None",
        0,
        0
    )
}
