package com.example.mycardiacrehab.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Appointment(

    @get:Exclude @set:Exclude
    var appointmentId: String = "",

    val patientId: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val appointmentDateTime: Timestamp = Timestamp.now(),
    val mode: String = "virtual",
    val status: String = "scheduled",
    val notes: String? = null
) {




    @Suppress("unused")
    constructor() : this("","", "", "", Timestamp.now(), "", "", null)
}