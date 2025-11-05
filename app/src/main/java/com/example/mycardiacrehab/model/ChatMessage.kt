package com.example.mycardiacrehab.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id:  String = "",
    val userId: String = "",
    val role: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isInScope: Boolean = true

) {
    @Suppress("unused")
    constructor() : this("", "", "", "", Timestamp.now(), true)

}