package com.example.mycardiacrehab.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isProvider by remember { mutableStateOf(false) }
    var providerCode by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()

    val primaryTeal = Color(0xFF00695C)
    val primaryBlue = Color(0xFF1E88E5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryTeal)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                // --- FIXES ARE HERE ---
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()), // Already had scroll
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp) // 1. Added automatic spacing
                // -----------------------
            ) {
                // --- Header and Title ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        "Sign up",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Removed Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Already have an account? ",
                        color = Color.Black
                    )
                    TextButton(
                        onClick = onNavigateToLogin,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            "Login",
                            color = primaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Removed Spacer(Modifier.height(16.dp))

                // --- Inputs ---
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Removed Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = "Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                // Removed Spacer(Modifier.height(16.dp))

                // --- Password Input ---
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Set Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                // Removed Spacer(Modifier.height(16.dp))

                // --- Provider Checkbox (Admin Check) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = isProvider, onCheckedChange = { isProvider = it })
                    Text(
                        "Register as Healthcare Provider",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                        )
                }

                AnimatedVisibility(visible = isProvider) {
                    OutlinedTextField(
                        value = providerCode,
                        onValueChange = { providerCode = it },
                        label = { Text("Provider Secret Code") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Provider Code") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                // --- Register Button ---
                Button(
                    onClick = {
                        viewModel.signUp(email, password, fullName, isProvider, providerCode)
                    },
                    enabled = authState != AuthViewModel.AuthState.Loading &&
                            fullName.isNotBlank() &&
                            password.length >= 6 &&
                            (!isProvider || (providerCode.isNotBlank())),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    if (authState == AuthViewModel.AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Register")
                    }
                }

                // --- Error Display ---
                if (authState is AuthViewModel.AuthState.Error) {
                    Text((authState as AuthViewModel.AuthState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}