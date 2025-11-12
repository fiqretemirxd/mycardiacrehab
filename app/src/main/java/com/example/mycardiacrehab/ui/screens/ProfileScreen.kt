package com.example.mycardiacrehab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val userProfile by profileViewModel.userProfile.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val saveSuccess by profileViewModel.saveSuccess.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentUserId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId

    // Local states for text fields
    var fullName by remember { mutableStateOf("") }
    var medicalHistory by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyNumber by remember { mutableStateOf("") }

    // Load the profile when the user ID is available
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            profileViewModel.loadUserProfile(currentUserId)
        }
    }

    // Populate local states once the profile is loaded from ViewModel
    LaunchedEffect(userProfile) {
        userProfile?.let {
            fullName = it.fullName
            medicalHistory = it.medicalHistory ?: ""
            allergies = it.allergies ?: ""
            emergencyName = it.emergencyContactName ?: ""
            emergencyNumber = it.emergencyContactNumber ?: ""
        }
    }

    // Show a snackbar on successful save
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("Profile saved successfully!")
            profileViewModel.resetSaveSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && userProfile == null) {
            // Show a full-screen loader only on initial load
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Show the form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Update your personal and medical information.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(24.dp))

                // --- Personal Details ---
                Text("Personal Details", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(Modifier.height(16.dp))

                // --- Medical Details ---
                Text("Medical Details", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = medicalHistory ?: "",
                    onValueChange = { medicalHistory = it },
                    label = { Text("Medical History (e.g., Heart attack 2023)") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    enabled = !isLoading
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = allergies ?: "",
                    onValueChange = { allergies = it },
                    label = { Text("Allergies (e.g., Penicillin, Peanuts)") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    enabled = !isLoading
                )
                Spacer(Modifier.height(16.dp))

                // --- Emergency Contact ---
                Text("Emergency Contact", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = emergencyName ?: "",
                    onValueChange = { emergencyName = it },
                    label = { Text("Emergency Contact Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyNumber ?: "",
                    onValueChange = { emergencyNumber = it },
                    label = { Text("Emergency Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(Modifier.height(32.dp))

                // --- Save Button ---
                Button(
                    onClick = {
                        if (currentUserId != null) {
                            profileViewModel.saveProfile(
                                userId = currentUserId,
                                fullName = fullName,
                                medicalHistory = medicalHistory,
                                allergies = allergies,
                                emergencyContactName = emergencyName,
                                emergencyContactNumber = emergencyNumber
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("SAVE CHANGES")
                    }
                }
            }
        }
    }
}