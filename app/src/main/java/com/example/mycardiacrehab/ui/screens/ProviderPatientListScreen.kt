package com.example.mycardiacrehab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mycardiacrehab.model.User
import com.example.mycardiacrehab.ui.navigation.ProviderScreen
import com.example.mycardiacrehab.viewmodel.ProviderViewModel

@Composable
fun ProviderPatientListScreen(
    navController: NavController,
    providerViewModel: ProviderViewModel = viewModel()
) {
    // Fetches the patient list when the screen is first displayed
    LaunchedEffect(Unit) {
        providerViewModel.loadPatients()
    }

    val patients by providerViewModel.patients.collectAsState()
    val isLoading by providerViewModel.loading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("My Assigned Patients", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (patients.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No patients assigned.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(patients, key = { it.userId }) { patient ->
                    PatientListItem(patient = patient) { selectedPatient ->
                        navController.navigate(ProviderScreen.PatientDetail.createRoute(selectedPatient.userId))
                    }
                }
            }
        }
    }
}

@Composable
fun PatientListItem(patient: User, onClick: (User) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(patient) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(patient.fullName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(patient.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            // The icon button also triggers the same navigation action
            IconButton(onClick = { onClick(patient) }) {
                Icon(Icons.Default.Visibility, contentDescription = "View Details")
            }
        }
    }
}
