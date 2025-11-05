package com.example.mycardiacrehab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.model.MedicationReminder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class MedicationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // --- State Management ---
    private val _dailySchedule = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val dailySchedule: StateFlow<List<MedicationReminder>> = _dailySchedule

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // Adherence Rate (used by Progress Screen)
    private val _adherenceRate = MutableStateFlow(0)
    val adherenceRate: StateFlow<Int> = _adherenceRate

    private var userId: String = "" // Variable to hold the current user ID for listeners

    // --- Load Daily Medication Schedule ---
    fun loadDailySchedule(currentUserId: String) {
        if (currentUserId.isBlank()) return
        userId = currentUserId

        // Use consistent, lowercase collection name
        db.collection("MedicationReminders")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING) // Corrected field to 'timestamp'
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Medication listen failed: $e")
                    return@addSnapshotListener
                }

                val logList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MedicationReminder::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _dailySchedule.value = logList
                calculateAdherence(logList) // Calculate adherence immediately after load
            }
    }

    // --- CRITICAL ADDITION: The Adherence Calculation function needed by the Progress screen ---
    private fun calculateAdherence(schedule: List<MedicationReminder>) {
        if (schedule.isEmpty()) {
            _adherenceRate.value = 0
            return
        }

        // For simplicity in MVP: check ratio of 'Taken' vs. total logged entries
        val totalDoses = schedule.size
        val dosesTaken = schedule.count { it.reminderStatus == "Taken" }

        val rate = (dosesTaken.toDouble() / totalDoses.toDouble() * 100).roundToInt()

        _adherenceRate.value = rate
    }

    // --- Set Prescription (F02-2 Control) ---
    fun setPrescription(
        patientId: String,
        medicationName: String,
        dosage: String,
        frequency: String,
        timeOfDay: String
    ) = viewModelScope.launch {
        if (patientId.isBlank()) return@launch
        _loading.value = true

        val newReminder = MedicationReminder(
            userId = patientId,
            medicationName = medicationName,
            dosage = dosage,
            frequency = frequency,
            timeOfDay = timeOfDay,
            timestamp = Timestamp.now(),
            reminderStatus = "Pending"
        )

        try {
            // Use consistent, lowercase collection name
            db.collection("MedicationReminders").add(newReminder).await()
        } catch (e: Exception) {
            println("Error setting prescription: ${e.message}")
        } finally {
            _loading.value = false
        }
    }

    // --- NEWLY ADDED: Update Medication Status ---
    fun updateStatus(medication: MedicationReminder, newStatus: String) = viewModelScope.launch {
        if (medication.id.isBlank()) return@launch // Cannot update if there's no ID

        _loading.value = true // Indicate that a save operation is in progress
        try {
            // Update the 'reminderStatus' field in the specific document
            db.collection("MedicationReminders").document(medication.id)
                .update("reminderStatus", newStatus)
                .await() // Wait for the update to complete
        } catch (e: Exception) {
            println("Error updating medication status: ${e.message}")
        } finally {
            _loading.value = false // Operation finished, hide loading indicator
        }
        // The snapshot listener in loadDailySchedule will automatically refresh the UI
    }
}
