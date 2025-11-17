package com.example.mycardiacrehab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.BuildConfig
import com.example.mycardiacrehab.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    sealed class AuthState {
        object Unauthenticated : AuthState()
        data class Authenticated(val userId: String, val userType: String, val email: String) : AuthState()
        data class Error(val message: String) : AuthState()
        object Loading : AuthState()
    }

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                fetchUserType(user.uid)
            } else {
                _authState.value = AuthState.Unauthenticated
                _currentUser.value = null
            }
        }
    }

    fun fetchUser() = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        try {
            val userDoc = db.collection("users").document(userId).get().await()
            _currentUser.value = userDoc.toObject(User::class.java)?.copy(userId = userId)
        } catch (e: Exception) {
            println("Error fetching current user details: ${e.message}")
        }
    }

    private fun fetchUserType(userId: String) = viewModelScope.launch {
        // Only show loading if we aren't already authenticated (prevents flicker)
        if (_authState.value !is AuthState.Authenticated) {
            _authState.value = AuthState.Loading
        }

        try {
            val userDoc = db.collection("users").document(userId).get().await()

            if (!userDoc.exists()) {
                _authState.value = AuthState.Error("Setting up profile... Please proceed to login.")
                auth.signOut()
                return@launch
            }

            // 🟢 CHECK: Is account active?
            val isActive = userDoc.getBoolean("isActive") ?: true

            if (!isActive) {
                // 🟢 FIX: Do NOT call signOut() here.
                // Calling signOut() triggers the listener which overwrites this Error state.
                _authState.value = AuthState.Error("Account pending Admin approval.")
                return@launch
            }

            val userType = userDoc.getString("userType") ?: "patient"
            val userObject = userDoc.toObject(User::class.java)?.copy(userId = userId)
            _currentUser.value = userObject

            val email = auth.currentUser?.email ?: "N/A"

            _authState.value = AuthState.Authenticated(userId, userType, email)

            fetchUser()
        } catch (e: Exception) {
            // 🟢 FIX: Do NOT call signOut() here either. Let the user see the network error.
            _authState.value = AuthState.Error("Failed to fetch user role: ${e.localizedMessage}")
        }
    }

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        isProvider: Boolean = false
    ) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val userType = if (isProvider) "provider" else "patient"
            val initialActiveStatus = !isProvider

            val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = userCredential.user?.uid ?: throw Exception("User ID is null after creation")

            val user = User(
                userId = userId,
                fullName = fullName,
                email = email,
                userType = userType,
                isActive = initialActiveStatus
            )

            db.collection("users").document(userId).set(user).await()
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Sign Up Failed: ${e.localizedMessage}")
        }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Login Failed: ${e.localizedMessage}")
        }
    }

    fun logout() {
        auth.signOut()
    }
}