package com.example.mycardiacrehab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.model.JournalEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class JournalViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: StateFlow<List<JournalEntry>> = _entries

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    var userId: String = ""

    fun loadJournalHistory(currentUserId: String) {
        if (currentUserId.isBlank()) return
        userId = currentUserId

        db.collection("patientjournal")
            .whereEqualTo("userId", currentUserId)
            .orderBy("entryDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Journal listen failed: $e")
                    return@addSnapshotListener
                }

                val entryList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(JournalEntry::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _entries.value = entryList
            }
    }

    fun logEntry(
        userId: String,
        mood: String,
        symptoms: String,
        freeText: String
    ) = viewModelScope.launch {
        if (userId.isBlank() || mood.isBlank() || symptoms.isBlank() && freeText.isBlank()) return@launch
        _loading.value = true

        val newEntry = JournalEntry(
            userId = userId,
            mood = mood,
            symptoms = symptoms.ifBlank { null },
            freeTextEntry = freeText,
            entryDate = Timestamp.now()
        )

        try {
            db.collection("patientjournal").add(newEntry).await()
        } catch (e: Exception) {
            println("Error logging journal: ${e.message}")
        } finally {
            _loading.value = false
        }
    }

    fun updateEntry(
        entryId: String,
        newMood: String,
        newSymptoms: String,
        newFreeText: String
    ) = viewModelScope.launch {
        if (entryId.isBlank()) return@launch
        _loading.value = true

        val updates = mapOf(
            "mood" to newMood,
            "symptoms" to newSymptoms.ifBlank { null },
            "freeTextEntry" to newFreeText
        )

        try {
            db.collection("patientjournal").document(entryId).update(updates).await()
        } catch (e: Exception) {
            println("Error updating journal entry: ${e.message}")
        } finally {
            _loading.value = false
        }
    }

    fun deleteEntry(entryId: String) = viewModelScope.launch {
        if (entryId.isBlank()) return@launch
        _loading.value = true

        try {
            db.collection("patientjournal").document(entryId).delete().await()
        } catch (e: Exception) {
            println("Error deleting journal entry: ${e.message}")
        } finally {
            _loading.value = false
        }
    }
}