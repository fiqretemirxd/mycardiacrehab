package com.example.mycardiacrehab.model

import com.google.firebase.Timestamp

data class ExerciseLog(
    val id: String = "",
    val userId: String = "",
    val exerciseType: String = "",
    val duration: Int = 0,
    val intensity: String = "Low",
    val notes: String? = null,
    val timestamp: Timestamp = Timestamp.now()
) {
    @Suppress("unused")
    constructor() : this("", "", "", 0, "Low", null, Timestamp.now())
}