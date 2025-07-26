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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.timeblessedlauncher.Data.*
import com.example.timeblessedlauncher.ViewModel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val allApps by viewModel.allApps.collectAsState()
    val restrictions by viewModel.appRestrictions.collectAsState()
    var showTimePickerFor by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter apps based on search
    val filteredApps = allApps.filter { app ->
        app.name.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F23),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
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

            // Search bar
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

            // Stats card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        title = "Total Apps",
                        value = allApps.size.toString(),
                        icon = Icons.Default.Apps
                    )
                    StatItem(
                        title = "Restricted",
                        value = restrictions.count { it.isRestricted }.toString(),
                        icon = Icons.Default.Lock
                    )
                    StatItem(
                        title = "Free",
                        value = (allApps.size - restrictions.count { it.isRestricted }).toString(),
                        icon = Icons.Default.LockOpen
                    )
                }
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
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRestricted)
                Color.Red.copy(alpha = 0.1f)
            else
                Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF667eea),
                                Color(0xFF764ba2)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App info
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
                IconButton(
                    onClick = onEditTime,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit time",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Toggle switch
            Switch(
                checked = isRestricted,
                onCheckedChange = { shouldRestrict ->
                    if (shouldRestrict) {
                        // Restricting - just call the callback
                        onToggleRestriction(true)
                    } else {
                        // Unrestricting - show dialog first
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
                // Keep the switch in restricted position
            }
        )
    }
}