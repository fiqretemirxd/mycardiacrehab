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
import kotlinx.coroutines.tasks.await

class AppointmentViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments

    private val _dashboardAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val dashboardAppointments: StateFlow<List<Appointment>> = _dashboardAppointments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var tabListener: ListenerRegistration? = null
    private var dashboardListener: ListenerRegistration? = null

    fun loadAppointmentsForDashboard(userId: String) {
        if (userId.isBlank()) {
            dashboardListener?.remove()
            _dashboardAppointments.value = emptyList()
            return
        }

        dashboardListener?.remove()
        _dashboardAppointments.value = emptyList()

        val query = db.collection("appointments")
            .whereEqualTo("patientId", userId)

        dashboardListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _dashboardAppointments.value = emptyList()
                return@addSnapshotListener
            }

            val allAppointments = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Appointment::class.java)?.apply { this.appointmentId = doc.id }
            } ?: emptyList()

            // Filter and sort on client side
            val now = Date()
            _dashboardAppointments.value = allAppointments
                .filter { it.status == "scheduled" && it.appointmentDateTime.toDate().after(now) }
                .sortedBy { it.appointmentDateTime.toDate() }
        }
    }

    fun loadAppointmentsForTab(userId: String, category: String) {
        println("========================================")
        println("📅 APPOINTMENT DEBUG - loadAppointmentsForTab called")
        println("UserId: $userId")
        println("Category: $category")
        println("========================================")

        if (userId.isBlank()) {
            println("❌ ERROR: UserId is blank, aborting load")
            tabListener?.remove()
            _appointments.value = emptyList()
            _isLoading.value = false
            return
        }

        tabListener?.remove()
        println("🗑️ Old listener removed")

        _isLoading.value = true
        _appointments.value = emptyList()

        viewModelScope.launch {
            println("⏰ Building simple query (no indexes needed)")

            val query: Query = db.collection("appointments")
                .whereEqualTo("patientId", userId)

            println("🔍 Query created for patientId: $userId")
            println("   - Will fetch ALL appointments for this user")
            println("   - Filtering and sorting will happen on client side")
            println("✅ Query built successfully, attaching listener...")


            tabListener = query.addSnapshotListener { snapshot, e ->
                println("\n🔔 LISTENER FIRED for category: $category")

                if (e != null) {
                    println("❌ ERROR in listener: ${e.message}")
                    println("❌ Error stack trace: ${e.printStackTrace()}")
                    _appointments.value = emptyList()
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                println("📦 Snapshot received, processing documents...")
                println("   Total documents in snapshot: ${snapshot?.documents?.size ?: 0}")

                val list = snapshot?.documents?.mapNotNull { doc ->
                    println("   📄 Document ID: ${doc.id}")
                    val appointment = doc.toObject(Appointment::class.java)?.apply { this.appointmentId = doc.id }
                    if (appointment != null) {
                        println("      ✅ Successfully parsed appointment")
                    } else {
                        println("      ❌ Failed to parse appointment")
                    }
                    appointment
                } ?: emptyList()

                val currentTime = Date()

                println("\n📊 RAW DATA FROM FIRESTORE:")
                println("   Category: $category")
                println("   Raw list size: ${list.size}")
                println("   Current time for filtering: $currentTime")
                println("   Current time in millis: ${currentTime.time}")
                println("\n📋 All appointments returned by Firestore query:")

                list.forEachIndexed { index, apt ->
                    val aptDate = apt.appointmentDateTime.toDate()
                    val isAfterNow = aptDate.after(currentTime)
                    val timeDiffMinutes = (aptDate.time - currentTime.time) / (1000 * 60)

                    println("   [$index] Provider: ${apt.providerName}")
                    println("       Status: ${apt.status}")
                    println("       DateTime: $aptDate")
                    println("       DateTime in millis: ${aptDate.time}")
                    println("       IsAfterNow: $isAfterNow")
                    println("       Time difference: $timeDiffMinutes minutes")
                    println("       Patient ID: ${apt.patientId}")
                    println("       Mode: ${apt.mode}")
                }

                println("\n🔍 APPLYING CLIENT-SIDE FILTERING AND SORTING...")

                val filtered = when (category) {
                    "past" -> {
                        list.filter {
                            val passes = it.status == "completed" || (it.status == "scheduled" && it.appointmentDateTime.toDate().before(currentTime))
                            println("   ${it.providerName}: passes PAST filter = $passes (status=${it.status}, before=${it.appointmentDateTime.toDate().before(currentTime)})")
                            passes
                        }.sortedByDescending { it.appointmentDateTime.toDate() }
                    }
                    "upcoming" -> {
                        list.filter {
                            val isScheduled = it.status == "scheduled"
                            val isAfter = it.appointmentDateTime.toDate().after(currentTime)
                            val passes = isScheduled && isAfter
                            println("   ${it.providerName}: passes UPCOMING filter = $passes (status='scheduled'=$isScheduled, isAfter=$isAfter)")
                            passes
                        }.sortedBy { it.appointmentDateTime.toDate() }
                    }
                    "cancelled" -> {
                        list.filter {
                            val passes = it.status == "cancelled"
                            println("   ${it.providerName}: passes CANCELLED filter = $passes (status=${it.status})")
                            passes
                        }.sortedByDescending { it.appointmentDateTime.toDate() }
                    }
                    else -> {
                        println("   Unknown category: $category, returning all")
                        list
                    }
                }

                _appointments.value = filtered

                println("\n✅ FINAL RESULT:")
                println("   Final filtered list size: ${_appointments.value.size}")
                _appointments.value.forEach { apt ->
                    println("   ✓ ${apt.providerName} at ${apt.appointmentDateTime.toDate()}")
                }
                println("========================================\n")

                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        tabListener?.remove()
        dashboardListener?.remove()
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
        _isLoading.value = true

        viewModelScope.launch {
            val newAppointment = Appointment(
                patientId = patientId,
                providerId = providerId,
                providerName = providerName,
                appointmentDateTime = dateTime,
                status = "scheduled",
                mode = mode,
                notes = notes
            )

            db.collection("appointments")
                .add(newAppointment)
                .addOnSuccessListener {
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _isLoading.value = false
                }
        }
    }

    fun cancelAppointment(appointmentId: String) {
        db.collection("appointments").document(appointmentId)
            .update("status", "cancelled")
    }

    fun updateAppointment(
        appointmentId: String,
        newDateTime: Timestamp,
        newMode: String,
        newNotes: String
    ) = viewModelScope.launch {
        if (appointmentId.isBlank()) return@launch
        _isLoading.value = true

        val updates = mapOf(
            "appointmentDateTime" to newDateTime,
            "mode" to newMode,
            "notes" to newNotes,
            "status" to "scheduled"
        )

        try {
            db.collection("appointments").document(appointmentId).update(updates).await()
        } catch (e: Exception) {
            println("Error updating appointment: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }
}
