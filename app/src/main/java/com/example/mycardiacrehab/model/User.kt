package com.example.mycardiacrehab.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    var userId: String = "",
    var fullName: String = "",
    val email: String = "",
    val userType: String = "patient",
    val specialization: String? = null,

    @ServerTimestamp
    val createdAt: Date? = null

)