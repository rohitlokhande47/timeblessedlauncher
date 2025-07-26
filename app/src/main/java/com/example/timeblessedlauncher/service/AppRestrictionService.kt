package com.example.timeblessedlauncher.Service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.timeblessedlauncher.Data.AppDatabase
import com.example.timeblessedlauncher.MainActivity
import com.example.timeblessedlauncher.R
import kotlinx.coroutines.*
import java.util.*

class AppRestrictionService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notificationJob: Job? = null

    companion object {
        const val CHANNEL_ID = "app_restriction_channel"
        const val NOTIFICATION_ID = 1001
        private const val CHECK_INTERVAL = 60000L // Check every minute

        fun startService(context: Context) {
            val intent = Intent(context, AppRestrictionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AppRestrictionService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        startNotificationChecks()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        notificationJob?.cancel()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Restrictions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about app availability"
                setSound(null, null) // Silent notifications
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TimeBlessedLauncher")
            .setContentText("Monitoring app restrictions")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // You'll need to add this icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startNotificationChecks() {
        notificationJob = serviceScope.launch {
            val database = AppDatabase.getDatabase(this@AppRestrictionService)
            val dao = database.appRestrictionDao()
            val prefs = getSharedPreferences("app_notifications", Context.MODE_PRIVATE)

            while (isActive) {
                try {
                    checkAndNotifyAppAvailability(dao, prefs)
                    delay(CHECK_INTERVAL)
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(CHECK_INTERVAL)
                }
            }
        }
    }

    private suspend fun checkAndNotifyAppAvailability(
        dao: com.example.timeblessedlauncher.Data.AppRestrictionDao,
        prefs: android.content.SharedPreferences
    ) {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        dao.getRestrictedApps().collect { restrictions ->
            restrictions.forEach { restriction ->
                val wasRestricted = prefs.getBoolean("restricted_${restriction.packageName}", true)
                val isCurrentlyRestricted = isAppRestricted(restriction, currentHour, currentMinute)

                // If app was restricted and is now available, send notification
                if (wasRestricted && !isCurrentlyRestricted) {
                    sendAppAvailableNotification(restriction.appName, restriction.packageName)

                    // Update preference to avoid duplicate notifications
                    prefs.edit()
                        .putBoolean("restricted_${restriction.packageName}", false)
                        .apply()
                }

                // Update restriction status for next check
                if (isCurrentlyRestricted != wasRestricted) {
                    prefs.edit()
                        .putBoolean("restricted_${restriction.packageName}", isCurrentlyRestricted)
                        .apply()
                }
            }
        }
    }

    private fun isAppRestricted(
        restriction: com.example.timeblessedlauncher.Data.AppRestriction,
        currentHour: Int,
        currentMinute: Int
    ): Boolean {
        val currentTotalMinutes = currentHour * 60 + currentMinute
        val fromTotalMinutes = restriction.showFromHour * 60
        val toTotalMinutes = restriction.showUntilHour * 60

        return when {
            fromTotalMinutes <= toTotalMinutes -> {
                // Same day restriction (e.g., 9 AM to 5 PM)
                currentTotalMinutes !in fromTotalMinutes..toTotalMinutes
            }
            else -> {
                // Overnight restriction (e.g., 10 PM to 6 AM)
                currentTotalMinutes !in fromTotalMinutes..1439 && currentTotalMinutes !in 0..toTotalMinutes
            }
        }
    }

    private fun sendAppAvailableNotification(appName: String, packageName: String) {
        val notificationPrefs = NotificationPreferences(this)

        // Don't send notification if disabled or in quiet hours
        if (!notificationPrefs.notificationsEnabled || notificationPrefs.isInQuietHours()) {
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("launch_app", packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, packageName.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$appName is now available! 🎉")
            .setContentText("Your restricted app is ready to use")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        // Apply sound and vibration preferences
        if (!notificationPrefs.notificationSound) {
            notificationBuilder.setSilent(true)
        }

        if (notificationPrefs.notificationVibration) {
            notificationBuilder.setVibrate(longArrayOf(0, 250, 250, 250))
        }

        try {
            with(NotificationManagerCompat.from(this)) {
                notify(packageName.hashCode(), notificationBuilder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

// Notification Permission Helper
class NotificationPermissionHelper {
    companion object {
        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }

        fun requestNotificationPermission(activity: android.app.Activity, requestCode: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    requestCode
                )
            }
        }

        fun openNotificationSettings(context: Context) {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    else -> {
                        action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}