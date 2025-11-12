package com.example.mycardiacrehab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // Holds the full user profile data
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    // For showing loading spinners on the UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // For showing a success message (like a Snack bar) on the UI
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    /**
     * Loads the complete user profile from Firestore.
     */
    fun loadUserProfile(userId: String) {
        if (userId.isBlank()) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val document = usersCollection.document(userId).get().await()
                _userProfile.value = document.toObject(User::class.java)
            } catch (e: Exception) {
                println("Error loading user profile: ${e.message}")
                _userProfile.value = null // Set to null on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Saves changes to the user's profile.
     * We only update the fields that are editable.
     */
    fun saveProfile(
        userId: String,
        fullName: String,
        medicalHistory: String,
        allergies: String,
        emergencyContactName: String,
        emergencyContactNumber: String
    ) {
        if (userId.isBlank()) return
        _isLoading.value = true
        _saveSuccess.value = false

        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "fullName" to fullName,
                    "medicalHistory" to medicalHistory,
                    "allergies" to allergies,
                    "emergencyContactName" to emergencyContactName,
                    "emergencyContactNumber" to emergencyContactNumber
                )

                usersCollection.document(userId).update(updates).await()

                // Manually update the local StateFlow to reflect changes instantly
                _userProfile.value = _userProfile.value?.copy(
                    fullName = fullName,
                    medicalHistory = medicalHistory,
                    allergies = allergies,
                    emergencyContactName = emergencyContactName,
                    emergencyContactNumber = emergencyContactNumber
                )
                _saveSuccess.value = true // Signal success to the UI
            } catch (e: Exception) {
                println("Error saving profile: ${e.message}")
                _saveSuccess.value = false // Signal failure
            } finally {
                _isLoading.value = false
            }
        }
    }
    /**
     * Resets the save success flag (so the message can be hidden).
     */
    fun saveProviderProfile(
        userId: String,
        fullName: String,
        specialization: String,
    ) {
        if (userId.isBlank()) return
        _isLoading.value = true
        _saveSuccess.value = false

        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "fullName" to fullName,
                    "specialization" to specialization
                )
                usersCollection.document(userId).update(updates).await()

                _userProfile.value = _userProfile.value?.copy(
                    fullName = fullName,
                    specialization = specialization
                )
                _saveSuccess.value = true
            } catch (e: Exception) {
                println("Error saving provider profile: ${e.message}")
                _saveSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}