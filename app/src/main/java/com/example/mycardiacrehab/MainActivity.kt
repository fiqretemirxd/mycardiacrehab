package com.example.mycardiacrehab

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mycardiacrehab.ui.screens.AdminDashboardScreen
import com.example.mycardiacrehab.ui.screens.SplashScreen
import com.example.mycardiacrehab.ui.screens.LoginScreen
import com.example.mycardiacrehab.ui.screens.PatientDashboardScreen
import com.example.mycardiacrehab.ui.screens.ProviderDashboardScreen
import com.example.mycardiacrehab.ui.screens.SignUpScreen
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.ui.theme.MycardiacrehabTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        setContent {
            MycardiacrehabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    val authState by authViewModel.authState.collectAsState()
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        // 🟢 2. SET THE STARTING SCREEN TO "splash"
                        startDestination = "splash"
                    ) {
                        // 🟢 3. ADD THE COMPOSABLE ROUTE FOR THE SPLASH SCREEN
                        composable("splash") {
                            SplashScreen(
                                onTimeout = {
                                    // When the timer finishes, navigate to login
                                    // and clear the back stack so the user can't go back to the splash screen.
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- The rest of your navigation graph remains the same ---
                        composable("login") {
                            LoginScreen(
                                //authViewModel = authViewModel,
                                onNavigateToSignUp = { navController.navigate("signup") }
                            )
                        }
                        composable("signup") {
                            SignUpScreen(
                                //authViewModel = authViewModel,
                                onNavigateToLogin = { navController.navigate("login") }
                            )
                        }
                        composable("patient_dashboard") {
                            PatientDashboardScreen(authViewModel = authViewModel)
                        }
                        composable("provider_dashboard") {
                            ProviderDashboardScreen(authViewModel = authViewModel)
                        }
                        composable("admin_dashboard") {
                            AdminDashboardScreen(authViewModel = authViewModel)
                        }
                    }

                    // This state observer logic for redirection remains the same
                    when (val state = authState) {
                        is AuthViewModel.AuthState.Authenticated -> {
                            val destination = when (state.userType) {
                                "patient" -> "patient_dashboard"
                                "provider" -> "provider_dashboard"
                                "admin" -> "admin_dashboard"
                                else -> "login" // Default fallback
                            }
                            navController.navigate(destination) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                        is AuthViewModel.AuthState.Unauthenticated, is AuthViewModel.AuthState.Error -> {
                            val currentRoute = navController.currentBackStackEntry?.destination?.route
                            if (currentRoute != "login" && currentRoute != "signup" && currentRoute != "splash") {
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        }
                        AuthViewModel.AuthState.Loading -> {
                            // While loading, we stay on the splash/login screen
                        }
                    }
                }
            }
        }
    }
}
