package com.example.timeblessedlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.timeblessedlauncher.Screen.HomeScreen
import com.example.timeblessedlauncher.Screen.SettingsScreen
import com.example.timeblessedlauncher.Service.AppRestrictionService
import com.example.timeblessedlauncher.Service.NotificationPermissionHelper
import com.example.timeblessedlauncher.ViewModel.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(applicationContext) as T
            }
        }
    }

    // Permission launcher for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Start the notification service
            AppRestrictionService.startService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load apps when activity is created
        viewModel.loadApps(packageManager)

        // Handle app launch from notification
        handleNotificationIntent(intent)

        // Request notification permission and start service
        requestNotificationPermissionAndStartService()

        setContent {
            val context = LocalContext.current
            var showSettings by remember { mutableStateOf(false) }
            var showNotificationPrompt by remember {
                mutableStateOf(!NotificationPermissionHelper.hasNotificationPermission(context))
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (!isDefaultLauncher(context)) {
                    DefaultLauncherPrompt(onSetDefault = {
                        openDefaultLauncherSettings(context)
                    })
                }

                if (showNotificationPrompt) {
                    NotificationPermissionPrompt(
                        onGrantPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationPermissionHelper.openNotificationSettings(context)
                            }
                            showNotificationPrompt = false
                        },
                        onDismiss = {
                            showNotificationPrompt = false
                        }
                    )
                }

                if (showSettings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { showSettings = false }
                    )
                } else {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't stop service on destroy as it should run in background
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.getStringExtra("launch_app")?.let { packageName ->
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }
    }

    private fun requestNotificationPermissionAndStartService() {
        if (NotificationPermissionHelper.hasNotificationPermission(this)) {
            AppRestrictionService.startService(this)
        }
    }

    private fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    private fun openDefaultLauncherSettings(context: Context) {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

@Composable
fun DefaultLauncherPrompt(onSetDefault: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { onSetDefault() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔒 TimeBlessedLauncher is not your default home app.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap here to set it as your default launcher.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun NotificationPermissionPrompt(
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Notifications",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Get notified when restricted apps become available",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Later", fontSize = 12.sp)
                }

                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Enable", fontSize = 12.sp)
                }
            }
        }
    }
}