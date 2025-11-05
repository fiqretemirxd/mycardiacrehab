package com.example.mycardiacrehab.model

import com.google.firebase.Timestamp

data class JournalEntry(
    val id: String = "",
    val userId: String = "",
    val mood: String = "",
    val symptoms: String? = null,
    val dietNotes: String? = null,
    val freeTextEntry: String = "",
    val entryDate: Timestamp = Timestamp.now()
) {
    @Suppress("unused")
    constructor() : this("", "", "", null, null, "", Timestamp.now())
}