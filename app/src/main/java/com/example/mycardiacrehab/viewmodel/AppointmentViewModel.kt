package com.example.mycardiacrehab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.model.Appointment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration

class AppointmentViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // A single list to hold the appointments for the currently selected tab
    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments

    // A flag to indicate if we are currently loading data
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 2. ADD THIS VARIABLE
    // This will hold a reference to the currently active listener
    private var currentListener: ListenerRegistration? = null

    fun loadAppointments(userId: String, category: String) {
        if (userId.isBlank()) return

        _isLoading.value = true

        // 3. ADD THIS LINE
        // Cancel any previous listener before attaching a new one.
        // This is the main fix that stops the race condition.
        currentListener?.remove()

        viewModelScope.launch {
            // Get the current date once
            val now = Date()

            // Base query for the user's appointments
            var query: Query = db.collection("appointments")
                .whereEqualTo("patientId", userId)

            // Modify the query based on the selected category
            when (category) {
                "upcoming" -> {
                    query = query
                        .whereEqualTo("status", "scheduled")
                        .whereGreaterThan("appointmentDateTime", now)
                        .orderBy("appointmentDateTime", Query.Direction.ASCENDING)
                }
                "past" -> {
                    query = query
                        // We can't use two `whereNotEqualTo` or inequality filters.
                        // So we query for completed or past scheduled appointments and filter client-side.
                        // For simplicity here, we'll just get all and then filter.
                        .orderBy("appointmentDateTime", Query.Direction.DESCENDING)
                }
                "cancelled" -> {
                    query = query
                        .whereEqualTo("status", "cancelled")
                        .orderBy("appointmentDateTime", Query.Direction.DESCENDING)
                }
            }

            // 4. MODIFY THIS LINE
            // Store the new listener in our variable
            currentListener = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _appointments.value = emptyList()
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Appointment::class.java)?.apply { this.appointmentId = doc.id }
                } ?: emptyList()

                // Final filtering for complex cases like "past"
                if (category == "past") {
                    _appointments.value = list.filter {
                        it.status == "completed" || (it.status == "scheduled" && it.appointmentDateTime.toDate().before(now))
                    }
                } else {
                    _appointments.value = list
                }

                _isLoading.value = false
            }
        }
    }

    // 5. ADD THIS FUNCTION (Good Practice)
    // This ensures the listener is removed if the ViewModel is destroyed.
    override fun onCleared() {
        currentListener?.remove()
        super.onCleared()
    }

    fun createAppointment(
        patientId: String,
        providerId: String,
        providerName: String,
        dateTime: Timestamp,
        mode: String,
        notes: String
    ) {
        _isLoading.value = true // Indicate that an operation is in progress

        viewModelScope.launch {
            val newAppointment = Appointment(
                patientId = patientId,
                providerId = providerId,
                providerName = providerName,
                appointmentDateTime = dateTime,
                status = "scheduled", // New appointments are always "scheduled"
                mode = mode,
                notes = notes,
                //summary = "" // Initially empty
            )

            // Add the new appointment to the "appointments" collection in Firestore
            db.collection("appointments")
                .add(newAppointment)
                .addOnSuccessListener {
                    // Success!
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    // Handle the error, e.g., show a toast
                    _isLoading.value = false
                }
        }
    }

    // This function is still needed for the cancel button action
    fun cancelAppointment(appointmentId: String) {
        db.collection("appointments").document(appointmentId)
            .update("status", "cancelled")
    }
}
