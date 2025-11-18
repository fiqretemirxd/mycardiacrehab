package com.example.mycardiacrehab.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import com.google.firebase.firestore.Exclude

data class User(
    var userId: String = "",
    var fullName: String = "",
    val email: String = "",
    val userType: String = "patient",
    val specialization: String? = null,

    //addon
    val medicalHistory: String? = null,
    val allergies: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactNumber: String? = null,

    @field:JvmField
    var isActive: Boolean = true,

    @ServerTimestamp
    val createdAt: Date? = null

)