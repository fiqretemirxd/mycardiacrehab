package com.example.mycardiacrehab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.model.ExerciseLog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ExerciseViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _logs = MutableStateFlow<List<ExerciseLog>>(emptyList())
    val logs: StateFlow<List<ExerciseLog>> = _logs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    var userId: String = ""

    fun loadExerciseHistory(currentUserId: String) {
        if (currentUserId.isEmpty()) return
        userId = currentUserId

        db.collection("exerciselog")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Listen failed: $e")
                    return@addSnapshotListener
                }
                val logList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ExerciseLog::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _logs.value = logList
            }
    }

    fun logExercise(
        userId: String,
        exerciseType: String,
        duration: Int,
        intensity: String,
    ) = viewModelScope.launch {
        if (userId.isBlank() || duration <= 0) return@launch
        _loading.value = true

        val log = ExerciseLog(
            userId = userId,
            exerciseType = exerciseType,
            duration = duration,
            intensity = intensity,
            timestamp = Timestamp.now()
        )

        try {
            db.collection("exerciselog").add(log).await()
        } catch (e: Exception) {
            println("Error logging exercise: ${e.message}")
        } finally {
            _loading.value = false
        }
    }

    fun updateExerciseLog(
        logId: String,
        newDuration: Int,
        newIntensity: String,
        newType: String
    ) = viewModelScope.launch {
        if (logId.isBlank() || newDuration <= 0) return@launch
        _loading.value = true

        val updates = mapOf(
            "duration" to newDuration,
            "intensity" to newIntensity,
            "exerciseType" to newType
        )
        try {
            db.collection("exerciselog").document(logId).update(updates).await()
        } catch (e: Exception) {
            println("Error updating exercise log: ${e.message}")
        } finally {
            _loading.value = false
        }
    }

    fun deleteExerciseLog(logId: String) = viewModelScope.launch {
        if (logId.isBlank()) return@launch
        _loading.value = true
        try {
            db.collection("exerciselog").document(logId).delete().await()
        } catch (e: Exception) {
            println("Error deleting exercise log: ${e.message}")
        } finally {
            _loading.value = false
        }
    }
}