package com.example.timeblessedlauncher.Screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.timeblessedlauncher.Data.*
import com.example.timeblessedlauncher.Service.AppRestrictionService
import com.example.timeblessedlauncher.Service.NotificationPermissionHelper
import com.example.timeblessedlauncher.ViewModel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allApps by viewModel.allApps.collectAsState()
    val restrictions by viewModel.appRestrictions.collectAsState()
    var showTimePickerFor by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showNotificationSettings by remember { mutableStateOf(false) }

    // Filter apps based on search
    val filteredApps = allApps.filter { app ->
        app.name.contains(searchQuery, ignoreCase = true)
    }

    // Clean black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = "App Restrictions",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { viewModel.clearAllRestrictions() },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Settings Card
            NotificationSettingsCard(
                onClick = { showNotificationSettings = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar - clean version
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search apps", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Clean stats display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    title = "Total Apps",
                    value = allApps.size.toString()
                )
                StatItem(
                    title = "Restricted",
                    value = restrictions.count { it.isRestricted }.toString()
                )
                StatItem(
                    title = "Free",
                    value = (allApps.size - restrictions.count { it.isRestricted }).toString()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Apps list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps) { app ->
                    val restriction = restrictions.find { it.packageName == app.packageName }
                    AppRestrictionItem(
                        app = app,
                        restriction = restriction,
                        onToggleRestriction = { isRestricted ->
                            if (isRestricted) {
                                showTimePickerFor = app.packageName
                            } else {
                                viewModel.removeRestriction(app.packageName)
                            }
                        },
                        onEditTime = {
                            showTimePickerFor = app.packageName
                        }
                    )
                }
            }
        }

        // Time picker dialog
        showTimePickerFor?.let { packageName ->
            val app = allApps.find { it.packageName == packageName }
            val currentRestriction = restrictions.find { it.packageName == packageName }

            if (app != null) {
                TimePickerDialog(
                    app = app,
                    currentRestriction = currentRestriction,
                    onSave = { timeRange ->
                        viewModel.setAppRestriction(
                            packageName = app.packageName,
                            appName = app.name,
                            fromHour = timeRange.fromHour,
                            toHour = timeRange.toHour,
                            fromMinute = timeRange.fromMinute,
                            toMinute = timeRange.toMinute
                        )
                        showTimePickerFor = null
                    },
                    onDismiss = {
                        showTimePickerFor = null
                    }
                )
            }
        }

        // Notification settings dialog
        if (showNotificationSettings) {
            NotificationSettingsDialog(
                onDismiss = { showNotificationSettings = false }
            )
        }
    }
}

@Composable
fun NotificationSettingsCard(
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val hasPermission = remember { NotificationPermissionHelper.hasNotificationPermission(context) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = if (hasPermission) Color.Green else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notifications",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (hasPermission)
                        "You'll be notified when apps become available"
                    else
                        "Enable to get notified about app availability",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun NotificationSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasPermission = remember { NotificationPermissionHelper.hasNotificationPermission(context) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notification Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (hasPermission) Color.Green else Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasPermission) "Notifications Enabled" else "Notifications Disabled",
                        color = if (hasPermission) Color.Green else Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = if (hasPermission) {
                        "You'll receive notifications when your restricted apps become available. The notification service is running in the background."
                    } else {
                        "Enable notifications to get alerts when your restricted apps become available. This helps you stay informed without constantly checking."
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                if (!hasPermission) {
                    Button(
                        onClick = {
                            NotificationPermissionHelper.openNotificationSettings(context)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enable Notifications")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                AppRestrictionService.stopService(context)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Stop Service")
                        }

                        Button(
                            onClick = {
                                NotificationPermissionHelper.openNotificationSettings(context)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun AppRestrictionItem(
    app: AppInfo,
    restriction: AppRestriction?,
    onToggleRestriction: (Boolean) -> Unit,
    onEditTime: () -> Unit
) {
    val isRestricted = restriction?.isRestricted == true
    var showUnrestrictDialog by remember { mutableStateOf(false) }

    // Clean app item design
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isRestricted)
                    Color.White.copy(alpha = 0.1f)
                else
                    Color.Transparent
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App name and info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                if (isRestricted && restriction != null) {
                    val timeRange = TimeRange(
                        restriction.showFromHour, 0,
                        restriction.showUntilHour, 0
                    )
                    Text(
                        text = "Available: ${timeRange}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Duration: ${String.format("%.1f", timeRange.getDurationHours())} hours",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            // Edit time button (if restricted)
            if (isRestricted) {
                TextButton(
                    onClick = onEditTime,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Edit", fontSize = 12.sp)
                }
            }

            // Toggle switch
            Switch(
                checked = isRestricted,
                onCheckedChange = { shouldRestrict ->
                    if (shouldRestrict) {
                        onToggleRestriction(true)
                    } else {
                        showUnrestrictDialog = true
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Red,
                    checkedTrackColor = Color.Red.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }
    }

    // Show unrestrict dialog when needed
    if (showUnrestrictDialog) {
        UnrestrictDialog(
            app = app,
            onUnrestrict = {
                showUnrestrictDialog = false
                onToggleRestriction(false)
            },
            onCancel = {
                showUnrestrictDialog = false
            }
        )
    }
}