package com.example.mycardiacrehab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _pendingProviders = MutableStateFlow<List<User>>(emptyList())
    val pendingProviders: StateFlow<List<User>> = _pendingProviders

    init {
        loadAllUsers()
        loadPendingProviders()
    }

    fun loadAllUsers() = viewModelScope.launch {
        _isLoading.value = true
        try {
            val result = db.collection("users").get().await()
            val userList = result.toObjects(User::class.java)
            _users.value = userList
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    fun loadPendingProviders() = viewModelScope.launch {
        try {
            val result = db.collection("users")
                .whereEqualTo("userType", "provider")
                .whereEqualTo("isActive", false)
                .get().await()
            _pendingProviders.value = result.toObjects(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun approveProvider(user: User) = viewModelScope.launch {
        try {
            val updates = hashMapOf(
                "isActive" to true,
                "active" to com.google.firebase.firestore.FieldValue.delete()
            )
            db.collection("users").document(user.userId)
                .update(updates)
                .await()

            loadAllUsers()
            loadPendingProviders()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Feature: Ban/Unban User (Toggle Active Status)
    fun toggleUserStatus(user: User) = viewModelScope.launch {
        try {
            val newStatus = !user.isActive
            db.collection("users").document(user.userId)
                .update("isActive", newStatus)
                .await()

            // Refresh list locally to update UI instantly
            loadAllUsers()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Feature: Delete User (Optional - be careful with this!)
    fun deleteUser(userId: String) = viewModelScope.launch {
        try {
            db.collection("users").document(userId).delete().await()
            loadAllUsers()
            loadPendingProviders()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}