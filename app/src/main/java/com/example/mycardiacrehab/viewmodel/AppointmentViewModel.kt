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
import com.google.firebase.firestore.ListenerRegistration // Ensure this is imported

class AppointmentViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments

    private val _dashboardAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val dashboardAppointments: StateFlow<List<Appointment>> = _dashboardAppointments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- FIX 3: Separate listeners for each screen ---
    private var tabListener: ListenerRegistration? = null
    private var dashboardListener: ListenerRegistration? = null

    // --- NEW FUNCTION: Only for the Dashboard (PatientHomeScreen) ---
    fun loadAppointmentsForDashboard(userId: String) {
        if (userId.isBlank()) {
            dashboardListener?.remove() // Remove any old dashboard listener
            _dashboardAppointments.value = emptyList()
            return
        }

        dashboardListener?.remove() // Remove any old dashboard listener
        _dashboardAppointments.value = emptyList()

        val now = Date()
        val query = db.collection("appointments")
            .whereEqualTo("patientId", userId)
            .whereEqualTo("status", "scheduled")
            .whereGreaterThan("appointmentDateTime", now)
            .orderBy("appointmentDateTime", Query.Direction.ASCENDING)

        dashboardListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _dashboardAppointments.value = emptyList()
                return@addSnapshotListener
            }
            _dashboardAppointments.value = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Appointment::class.java)?.apply { this.appointmentId = doc.id }
            } ?: emptyList()
        }
    }

    // --- RENAMED FUNCTION: Only for the AppointmentScreen ---
    fun loadAppointmentsForTab(userId: String, category: String) {
        if (userId.isBlank()) {
            tabListener?.remove() // Remove any old tab listener
            _appointments.value = emptyList()
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _appointments.value = emptyList()

        tabListener?.remove() // Remove any old tab listener

        viewModelScope.launch {
            val now = Date()
            var query: Query = db.collection("appointments")
                .whereEqualTo("patientId", userId)

            when (category) {
                "upcoming" -> {
                    query = query
                        .whereEqualTo("status", "scheduled")
                        .whereGreaterThan("appointmentDateTime", now)
                        .orderBy("appointmentDateTime", Query.Direction.ASCENDING)
                }

                "past" -> {
                    query = query
                        .orderBy("appointmentDateTime", Query.Direction.DESCENDING)
                }

                "cancelled" -> {
                    query = query
                        .whereEqualTo("status", "cancelled")
                        .orderBy("appointmentDateTime", Query.Direction.DESCENDING)
                }
            }


            tabListener = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _appointments.value = emptyList()
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Appointment::class.java)?.apply { this.appointmentId = doc.id }
                } ?: emptyList()

                if (category == "past") {
                    _appointments.value = list.filter {
                        it.status == "completed" || (it.status == "scheduled" && it.appointmentDateTime.toDate()
                            .before(now))
                    }
                } else {
                    _appointments.value = list
                }
                _isLoading.value = false
            }
        }
    }

    // --- FIX 4: Make sure to clear BOTH listeners ---
    override fun onCleared() {
        tabListener?.remove()
        dashboardListener?.remove()
        super.onCleared()
    }

    // --- (createAppointment and cancelAppointment functions are fine, no changes) ---
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
                notes = notes
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

    fun cancelAppointment(appointmentId: String) {
        db.collection("appointments").document(appointmentId)
            .update("status", "cancelled")
    }
}
