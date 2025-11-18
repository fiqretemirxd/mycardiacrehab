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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.model.User
import com.example.mycardiacrehab.viewmodel.AdminViewModel
import com.example.mycardiacrehab.viewmodel.AuthViewModel

// Define constants for the tabs
enum class UserTab(val title: String) {
    PATIENTS("Patients"),
    PROVIDERS("Providers")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel = viewModel()
) {
    val users by adminViewModel.users.collectAsState()
    val pendingProviders by adminViewModel.pendingProviders.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(UserTab.PATIENTS) }

    // Filter users based on the selected tab
    val filteredUsers = remember(users, selectedTab) {
        users.filter { user ->
            when (selectedTab) {
                UserTab.PATIENTS -> user.userType == "patient"
                UserTab.PROVIDERS -> user.userType == "provider"
            }
        }
    }

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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // --- SECTION 1: PENDING APPROVALS ---
            if (pendingProviders.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "⚠️ ${pendingProviders.size} Pending Provider Approvals",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        // Show a condensed list of pending providers
                        pendingProviders.take(3).forEach { provider ->
                            PendingProviderItem(
                                user = provider,
                                onApprove = { adminViewModel.approveProvider(provider) }
                            )
                        }
                    }
                }
            }

            // --- SECTION 2: TABS FOR USER MANAGEMENT ---
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- SECTION 3: USER LIST CONTENT ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.padding(top = 32.dp))
                }
            } else if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No ${selectedTab.title.lowercase()} found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            "${selectedTab.title} (${filteredUsers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(filteredUsers, key = { it.userId }) { user ->
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
fun PendingProviderItem(user: User, onApprove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            user.fullName.ifBlank { "Unknown" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onApprove,
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.height(30.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
        ) {
            Text("APPROVE")
        }
    }
}

@Composable
fun AdminUserCard(user: User, onToggleStatus: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFEBEE)
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
                            text = "(SUSPENDED)",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Actions
            Column(horizontalAlignment = Alignment.End) {
                // Button 1: Suspend / Activate (Toggle Status)
                Button(
                    onClick = onToggleStatus,
                    colors = ButtonDefaults.buttonColors(
                        // If active -> Orange button (to suspend). If inactive -> Green button (to activate)
                        containerColor = if (user.isActive) Color(0xFFFF9800) else Color(0xFF4CAF50)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (user.isActive) "Suspend" else "Activate",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Button 2: Permanent Delete (Set to Red)
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Permanently",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Remove",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}