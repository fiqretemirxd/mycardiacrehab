package com.example.mycardiacrehab.model

import com.google.firebase.Timestamp
import java.util.Date

data class MedicationReminder(
    val id: String = "",
    val userId: String = "",
    val medicationName: String = "",
    val dosage: String = "",
    val frequency: String = "Once Daily",
    val timeOfDay: String = "",
    val reminderStatus: String = "Pending",
    val timestamp: Timestamp = Timestamp(Date())
) {
    @Suppress("unused")
    constructor() : this("", "", "", "", "", "", "Pending", Timestamp.now())

}