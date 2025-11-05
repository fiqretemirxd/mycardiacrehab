package com.example.mycardiacrehab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProviderViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _patients = MutableStateFlow<List<User>>(emptyList())
    val patients: StateFlow<List<User>> = _patients

    private val _providerProfile = MutableStateFlow<User?>(null)
    val providerProfile: StateFlow<User?> = _providerProfile

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun loadPatients() = viewModelScope.launch {
        _loading.value = true
        try {
            val snapshot = db.collection("users")
                .whereEqualTo("userType", "patient")
                .get().await()
            _patients.value = snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            println("Error loading patients: ${e.message}")
        } finally {
            _loading.value = false
        }
    }

    // ✅ --- REFACTORED FUNCTION ---
    fun loadProviderProfile() = viewModelScope.launch {
        // No need to set loading here if the UI doesn't depend on it
        try {
            val providerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val document = db.collection("users").document(providerId).get().await()
            if (document.exists()) {
                _providerProfile.value = document.toObject(User::class.java)
            }
        } catch (e: Exception) {
            println("Error loading provider profile: ${e.message}")
            // Handle error, maybe set profile to null or an error state
        }
    }
}