package com.example.mycardiacrehab.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Make sure this is imported
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mycardiacrehab.model.Appointment
import com.example.mycardiacrehab.ui.navigation.PatientScreen
import com.example.mycardiacrehab.viewmodel.AppointmentViewModel
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.MedicationViewModel
import com.example.mycardiacrehab.viewmodel.ProgressViewModel
import com.example.mycardiacrehab.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(
    authViewModel: AuthViewModel,
) {
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthViewModel.AuthState.Authenticated)?.email ?: "User"
    val userId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId ?: "N/A"
    val navController = rememberNavController()

    // 1. CREATE ALL VIEW MODELS HERE (THE PARENT)
    // This ensures there is only ONE instance of each.
    val appointmentViewModel: AppointmentViewModel = viewModel()
    val medicationViewModel: MedicationViewModel = viewModel()
    val progressViewModel: ProgressViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()


    val primaryTeal = Color(0xFF00695C)

    val bottomNavItems = listOf(
        PatientScreen.Dashboard,
        PatientScreen.Exercise,
        PatientScreen.Medication,
        PatientScreen.Chatbot,
        PatientScreen.More
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyCardiacRehab") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryTeal,
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

                bottomNavItems.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

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
            startDestination = PatientScreen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- Dashboard Tab ---
            composable(PatientScreen.Dashboard.route) {
                PatientHomeScreen(
                    userId = userId,
                    userEmail = userEmail,
                    navController = navController,
                    // 2. PASS THE SHARED VIEW MODELS DOWN
                    progressViewModel = progressViewModel,
                    appointmentViewModel = appointmentViewModel,
                    medicationViewModel = medicationViewModel
                )
            }

            // --- Secondary Screens (Accessed via MoreScreen) ---
            composable(PatientScreen.Exercise.route) { ExerciseScreen() }
            composable(PatientScreen.Medication.route) {
                // 3. PASS THE SHARED MEDICATION VIEWMODEL
                MedicationScreen(
                    authViewModel = authViewModel,
                    medicationViewModel = medicationViewModel
                )
            }
            composable(PatientScreen.Chatbot.route) { ChatbotScreen() }
            composable(PatientScreen.Journal.route) { JournalScreen() }
            composable(PatientScreen.Appointments.route) {
                // 4. PASS THE SHARED AUTH AND APPOINTMENT VIEW MODELS
                AppointmentScreen(
                    authViewModel = authViewModel,
                    appointmentViewModel = appointmentViewModel
                )
            }
            composable(PatientScreen.Progress.route) {
                // 5. PASS THE SHARED PROGRESS VIEWMODEL
                ProgressScreen(
                    authViewModel = authViewModel,
                    progressViewModel = progressViewModel
                )
            }

            composable(PatientScreen.Profile.route) {
                ProfileScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel
                )
            }

            composable(PatientScreen.More.route) { MoreScreen(navController = navController) }
        }
    }
}

// -------------------------------------------------------------------------------------
// --- HELPER COMPONENT DEFINITIONS ---
// -------------------------------------------------------------------------------------

// --- Home Screen Content ---
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PatientHomeScreen(
    userId: String,
    userEmail: String,
    navController: NavHostController,
    progressViewModel: ProgressViewModel,
    appointmentViewModel: AppointmentViewModel, // This is the shared VM
    medicationViewModel: MedicationViewModel
) {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        // This effect will run ONLY when the userId becomes stable and is not "N/A"
        if (userId != "N/A") {
            isLoading = true

            // Use joinAll to launch all three data fetches concurrently and wait for them to finish
            joinAll(
                launch { progressViewModel.loadWeeklyProgress(userId) },
                launch { appointmentViewModel.loadAppointmentsForDashboard(userId) },
                launch { medicationViewModel.loadDailySchedule(userId) }
            )

            // Only after all launches complete, set loading to false.
            isLoading = false
        } else {
            // If the user is unauthenticated or loading, show nothing/spinner briefly
            isLoading = false
        }
    }

    val summary by progressViewModel.weeklySummary.collectAsState()

    // --- FIX: Observe the NEW dashboard-specific list ---
    val upcomingAppointments by appointmentViewModel.dashboardAppointments.collectAsState()

    val adherenceRate by medicationViewModel.adherenceRate.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Greeting ---
            item {
                Text(
                    "Hello, ${userEmail.substringBefore("@")}!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Review your adherence and upcoming tasks.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // --- Weekly Progress Summary Cards (Resolves Unresolved reference 'DashboardSummaryCard') ---
            item {
                Text("Weekly Progress Snapshot", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Exercise Minutes Card
                    DashboardSummaryCard(
                        title = "Exercise Mins",
                        value = summary?.totalExerciseMinutes?.toString() ?: "--",
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        onClick = { navController.navigate(PatientScreen.Exercise.route) },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    // 2. Medication Adherence Card
                    DashboardSummaryCard(
                        title = "Meds Adherence",
                        value = if (adherenceRate > 0) "$adherenceRate%" else "--",
                        icon = Icons.Default.Schedule,
                        onClick = { navController.navigate(PatientScreen.Medication.route) },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // --- Upcoming Appointments List (Resolves Unresolved reference 'AppointmentSummaryItem') ---
            item {
                Text(
                    "Upcoming Appointments (${upcomingAppointments.size})",
                    style = MaterialTheme.typography.titleLarge
                )

                if (upcomingAppointments.isEmpty()) {
                    Text(
                        "No upcoming appointments.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = Color.Gray
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        upcomingAppointments.take(3).forEach { appointment ->
                            AppointmentSummaryItem(appointment)
                        }
                    }
                }
                if (upcomingAppointments.isNotEmpty()) {
                    Button(
                        onClick = { navController.navigate(PatientScreen.Appointments.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(top = 12.dp)
                    ) {
                        Text("VIEW ALL APPOINTMENTS")
                    }
                }
            }

            // --- Quick Access to Chat ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(PatientScreen.Chatbot.route) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "Chat",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Need Guidance?", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Ask the AI Chatbot a question.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open Chat"
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Helper Functions (Resolves all Unresolved Reference errors)
// -------------------------------------------------------------------------------------

@Composable
fun DashboardSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    color: Color
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Color.Black)
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun AppointmentSummaryItem(appointment: Appointment) {
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = "Appointment",
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.weight(1f)) {
                Text(appointment.providerName, style = MaterialTheme.typography.titleMedium)
                Text(dateFormatter.format(appointment.appointmentDateTime.toDate()), style = MaterialTheme.typography.bodySmall)
            }
            Text(appointment.mode.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

// --- More Screen Content ---
@Composable
fun MoreScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("More Options", style = MaterialTheme.typography.headlineMedium)

        MoreNavigationItem(
            title = PatientScreen.Profile.title,
            icon = PatientScreen.Profile.icon,
            onClick = { navController.navigate(PatientScreen.Profile.route) }
        )

        MoreNavigationItem(
            title = PatientScreen.Progress.title,
            icon = PatientScreen.Progress.icon,
            onClick = { navController.navigate(PatientScreen.Progress.route) }
        )
        MoreNavigationItem(
            title = PatientScreen.Journal.title,
            icon = PatientScreen.Journal.icon,
            onClick = { navController.navigate(PatientScreen.Journal.route) }
        )
        MoreNavigationItem(
            title = PatientScreen.Appointments.title,
            icon = PatientScreen.Appointments.icon,
            onClick = { navController.navigate(PatientScreen.Appointments.route) }
        )
    }
}

@Composable
fun MoreNavigationItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to $title")
        }
    }
}
