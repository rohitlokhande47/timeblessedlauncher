package com.example.timeblessedlauncher.Screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timeblessedlauncher.ViewModel.AppViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onOpenSettings: () -> Unit
) {
    val visibleApps by viewModel.visibleApps.collectAsState()
    val hiddenAppsCount by viewModel.hiddenAppsCount.collectAsState()
    val context = LocalContext.current

    // Update visible apps every minute
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateVisibleApps()
            delay(60000) // Re-check every 60 seconds
        }
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
                .padding(horizontal = 24.dp, vertical = 60.dp)
        ) {
            // Header with settings button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Clock and Date
                ClockAndDate(modifier = Modifier.weight(1f))

                // Settings button
                IconButton(
                    onClick = onOpenSettings,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status indicator
            StatusIndicator(
                visibleCount = visibleApps.size,
                hiddenCount = hiddenAppsCount,
                onOpenSettings = onOpenSettings
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Grid with card-based layout
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleApps.chunked(2)) { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowApps.forEach { app ->
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(300)
                                ) + fadeIn(animationSpec = tween(300)),
                                exit = slideOutVertically(
                                    targetOffsetY = { -it / 4 },
                                    animationSpec = tween(300)
                                ) + fadeOut(animationSpec = tween(300)),
                                modifier = Modifier.weight(1f)
                            ) {
                                AppItem(
                                    app = app,
                                    onClick = {
                                        val launchIntent = context.packageManager
                                            .getLaunchIntentForPackage(app.packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        }
                                    }
                                )
                            }
                        }

                        // Add empty space if odd number of apps in row
                        if (rowApps.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Add some bottom padding for the last item
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun ClockAndDate(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("") }

    // Update time, date, and greeting
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("H:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)

            greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..20 -> "Good evening"
                else -> "Good night"
            }

            delay(1000)
        }
    }

    Column(
        modifier = modifier
    ) {
        // Greeting
        Text(
            text = greeting,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Time
        Text(
            text = currentTime,
            color = Color.White,
            fontSize = 78.sp,
            fontWeight = FontWeight.ExtraLight,
            lineHeight = 78.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Date
        Text(
            text = currentDate,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
fun StatusIndicator(
    visibleCount: Int,
    hiddenCount: Int,
    onOpenSettings: () -> Unit
) {
    // Clean text-only status indicator
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hiddenCount > 0) {
                    Modifier.clickable { onOpenSettings() }
                } else Modifier
            )
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "$visibleCount apps available",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        if (hiddenCount > 0) {
            Text(
                text = "$hiddenCount apps time-restricted • Tap to manage",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        } else {
            Text(
                text = "All apps available • Tap settings to add restrictions",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun AppItem(
    app: com.example.timeblessedlauncher.Data.AppInfo,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // Card-based app item similar to the reference image
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable {
                isPressed = true
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isPressed)
                Color(0xFF2A2A2A)
            else
                Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // App name at bottom left
            Text(
                text = app.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            // Optional: Add some icons in top area similar to reference
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // You can add app-specific icons here if needed
                // For now, we'll keep it minimal
            }
        }
    }

    // Reset pressed state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}