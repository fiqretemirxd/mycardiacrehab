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

    // FIX 1: Added the new StateFlow to hold the full User object
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
                _currentUser.value = null // Also clear the user details on logout
            }
        }
    }

    // FIX 2: Added the new function to fetch the full user details
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

        // --- THIS IS THE FIX ---
        // We only set 'Loading' if the user is NOT already authenticated.
        // This stops the flicker on every screen load.
        if (_authState.value !is AuthState.Authenticated) {
            _authState.value = AuthState.Loading
        }
        // --- END OF FIX ---

        try {
            val userDoc = db.collection("users").document(userId).get().await()
            val userType = userDoc.getString("userType") ?: "patient"

            val userObject = userDoc.toObject(User::class.java)?.copy(userId = userId)
            _currentUser.value = userObject

            val email = auth.currentUser?.email ?: "N/A"


            _authState.value = AuthState.Authenticated(userId, userType, email)

            // FIX 3: Call the new function to populate the currentUser StateFlow
            fetchUser()
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Failed to fetch user role: ${e.localizedMessage}")
        }
    }

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        isProvider: Boolean = false,
        providerCode: String

        ) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            var userType = "patient"
            if (isProvider) {
                if (providerCode == BuildConfig.PROVIDER_SECRET_CODE) {
                    userType = "provider"
                } else {
                    _authState.value = AuthState.Error("Sign Up Failed: Invalid Provider Code.")
                    return@launch
                }
            }
            val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = userCredential.user?.uid ?: throw Exception("User ID is null after creation")

            val user = User(
                // Using 'uid' to match the User data class property
                userId = userId,
                fullName = fullName,
                email = email,
                userType = userType,
                // The 'specialization' property might not exist in your User model.
                // If it doesn't, this line should be removed.
                // specialization = if (isProvider) "Cardiologist" else null
            )

            db.collection("users").document(userId).set(user).await()
            // After sign-up, the auth state listener will automatically trigger fetchUserType().
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Sign Up Failed: ${e.localizedMessage}")
        }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            // On success, the auth state listener will automatically trigger fetchUserType()
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Login Failed: ${e.localizedMessage}")
        }
    }

    fun logout() {
        auth.signOut()
        // The auth state listener will handle setting the states to unauthenticated.
    }
}
