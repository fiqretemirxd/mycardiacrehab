package com.example.mycardiacrehab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.User
import com.example.mycardiacrehab.viewmodel.AdminViewModel
import com.example.mycardiacrehab.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel = viewModel()
) {
    val users by adminViewModel.users.collectAsState()
    val pendingProviders by adminViewModel.pendingProviders.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = {
                        adminViewModel.loadAllUsers()
                        adminViewModel.loadPendingProviders()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // --- SECTION 1: PENDING APPROVALS ---
                if (pendingProviders.isNotEmpty()) {
                    item {
                        Text(
                            "⚠️ Pending Provider Approvals",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(pendingProviders) { provider ->
                        PendingProviderCard(
                            user = provider,
                            onApprove = { adminViewModel.approveProvider(provider) }
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                } else {
                    // Optional: Message when no pending approvals
                    // item { Text("No pending approvals.", color = Color.Gray) }
                }

                // --- SECTION 2: ALL USERS ---
                item {
                    Text(
                        "All Users (${users.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (users.isEmpty()) {
                    item {
                        Text("No users found.", color = Color.Gray)
                    }
                } else {
                    items(users) { user ->
                        AdminUserCard(
                            user = user,
                            onToggleStatus = { adminViewModel.toggleUserStatus(user) },
                            onDelete = { adminViewModel.deleteUser(user.userId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingProviderCard(user: User, onApprove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), // Light Orange
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFE65100) // Dark Orange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName.ifBlank { "Unknown Name" }, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Requested Role: PROVIDER",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Green
            ) {
                Text("Approve")
            }
        }
    }
}

@Composable
fun AdminUserCard(user: User, onToggleStatus: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFEBEE) // Light Red if inactive
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (user.isActive) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName.ifBlank { "Unknown Name" },
                    fontWeight = FontWeight.Bold,
                    color = if (user.isActive) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.isActive) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Role: ${user.userType.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!user.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(INACTIVE/BANNED)",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Actions
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onToggleStatus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (user.isActive) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        if (user.isActive) "Delete" else "Approve",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Optional Delete button (Comment out if too dangerous)
                /*
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Delete", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                */
            }
        }
    }
}