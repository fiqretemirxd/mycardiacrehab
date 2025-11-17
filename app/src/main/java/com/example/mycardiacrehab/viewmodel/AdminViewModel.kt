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

    init {
        loadAllUsers()
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}