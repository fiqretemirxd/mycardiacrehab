package com.example.mycardiacrehab.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.mycardiacrehab.ui.navigation.ProviderScreen
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.ProviderViewModel
import com.example.mycardiacrehab.viewmodel.ProfileViewModel
import com.example.mycardiacrehab.viewmodel.ReportViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(
    authViewModel: AuthViewModel,
    providerViewModel: ProviderViewModel = viewModel()
) {

    val currentUser by authViewModel.currentUser.collectAsState()
    val profileViewModel: ProfileViewModel = viewModel()
    val reportViewModel: ReportViewModel = viewModel()

    val items = listOf(
        ProviderScreen.PatientList,
        ProviderScreen.Appointments,
        ProviderScreen.Medication,
        ProviderScreen.ReportCenter,
        ProviderScreen.Settings
    )

    val navController = rememberNavController()

    val primaryBlue = Color(0xFF455A64)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentUser?.fullName ?: "Provider Portal") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryBlue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    val isSelected =
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ProviderScreen.PatientList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- Patient List (Monitoring Core) ---
            composable(ProviderScreen.PatientList.route) {
                ProviderPatientListScreen(
                    navController = navController,
                    providerViewModel = providerViewModel // Now being used
                )
            }

            // --- Appointments Screen (Provider View) ---
            composable(ProviderScreen.Appointments.route) {
                ProviderAppointmentScreen(
                    providerViewModel = providerViewModel,
                    authViewModel = authViewModel
                )
            }

            composable(ProviderScreen.Medication.route) {
                ProviderMedicationScreen(providerViewModel = providerViewModel)
            }

            // --- Report Center (F04-2, Interview Q9) ---
            composable(ProviderScreen.ReportCenter.route) {
                ReportCenterScreen(
                    providerViewModel = providerViewModel,
                    reportViewModel = reportViewModel
                )
            }

            // --- Settings ---
            composable(ProviderScreen.Settings.route) {
                ProviderSettingsScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                )
            }

            composable(ProviderScreen.Profile.route) {
                ProviderProfileScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel
                )
            }

            // --- PATIENT DETAIL SCREEN (Uses navArgument and NavType) ---
            composable(
                route = ProviderScreen.PatientDetail.route,
                arguments = listOf(navArgument("patientId") { type = NavType.StringType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: "error"
                PatientDetailScreen(
                    patientId = patientId,
                    providerViewModel = providerViewModel
                )
            }
        }
    }
}
    @Composable
    fun ProviderSettingsScreen(
        navController: NavHostController,
        authViewModel: AuthViewModel
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            MoreNavigationItem(
                title = "Edit Profile",
                icon = ProviderScreen.Profile.icon,
                onClick = { navController.navigate(ProviderScreen.Profile.route) }
            )
        }
    }

