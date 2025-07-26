package com.example.timeblessedlauncher.Screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timeblessedlauncher.ViewModel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onOpenSettings: () -> Unit
) {
    val visibleApps by viewModel.visibleApps.collectAsState()
    val hiddenAppsCount by viewModel.hiddenAppsCount.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState() // New state for favorite apps
    val allApps by viewModel.allApps.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pager state for handling swipe between home and app drawer
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    // Update visible apps every minute
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateVisibleApps()
            delay(60000) // Re-check every 60 seconds
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> {
                // Home Screen - Only favorite apps
                HomeScreenContent(
                    favoriteApps = favoriteApps.filter { favoriteApp ->
                        visibleApps.any { it.packageName == favoriteApp.packageName }
                    },
                    hiddenAppsCount = hiddenAppsCount,
                    onOpenSettings = onOpenSettings,
                    onAppClick = { app ->
                        val launchIntent = context.packageManager
                            .getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    },
                    onSwipeLeft = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
            }
            1 -> {
                // App Drawer Screen - All apps
                AppDrawerScreen(
                    allApps = visibleApps,
                    favoriteApps = favoriteApps,
                    onAppClick = { app ->
                        val launchIntent = context.packageManager
                            .getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    },
                    onToggleFavorite = { app ->
                        viewModel.toggleFavorite(app.packageName)
                    },
                    onSwipeRight = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    favoriteApps: List<com.example.timeblessedlauncher.Data.AppInfo>,
    hiddenAppsCount: Int,
    onOpenSettings: () -> Unit,
    onAppClick: (com.example.timeblessedlauncher.Data.AppInfo) -> Unit,
    onSwipeLeft: () -> Unit
) {
    // Clean black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -50) { // Swipe left threshold
                        onSwipeLeft()
                    }
                }
            }
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
            HomeStatusIndicator(
                favoriteCount = favoriteApps.size,
                hiddenCount = hiddenAppsCount,
                onOpenSettings = onOpenSettings
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Favorite Apps Grid
            if (favoriteApps.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No favorite apps added",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Swipe left to add apps to favorites",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favoriteApps.chunked(2)) { rowApps ->
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
                                        onClick = { onAppClick(app) }
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

        // Swipe indicator at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Swipe left for all apps",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AppDrawerScreen(
    allApps: List<com.example.timeblessedlauncher.Data.AppInfo>,
    favoriteApps: List<com.example.timeblessedlauncher.Data.AppInfo>,
    onAppClick: (com.example.timeblessedlauncher.Data.AppInfo) -> Unit,
    onToggleFavorite: (com.example.timeblessedlauncher.Data.AppInfo) -> Unit,
    onSwipeRight: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter apps based on search
    val filteredApps = allApps.filter { app ->
        app.name.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50) { // Swipe right threshold
                        onSwipeRight()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 40.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSwipeRight,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home")
                }

                Text(
                    text = "All Apps",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onOpenSettings,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
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

            // Apps list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredApps) { app ->
                    val isFavorite = favoriteApps.any { it.packageName == app.packageName }

                    AppDrawerItem(
                        app = app,
                        isFavorite = isFavorite,
                        onAppClick = { onAppClick(app) },
                        onToggleFavorite = { onToggleFavorite(app) }
                    )
                }
            }
        }

        // Swipe indicator at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Swipe right to go back",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun AppDrawerItem(
    app: com.example.timeblessedlauncher.Data.AppInfo,
    isFavorite: Boolean,
    onAppClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAppClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = app.name,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onToggleFavorite,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isFavorite) Color.Yellow else Color.White.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun HomeStatusIndicator(
    favoriteCount: Int,
    hiddenCount: Int,
    onOpenSettings: () -> Unit
) {
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
            text = "$favoriteCount favorite apps",
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
                text = "Swipe left to browse all apps",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

// Keep the existing ClockAndDate and AppItem composables
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