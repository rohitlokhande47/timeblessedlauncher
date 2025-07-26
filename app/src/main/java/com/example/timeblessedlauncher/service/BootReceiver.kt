package com.example.timeblessedlauncher.Service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Check if notifications are enabled before starting service
                if (NotificationPermissionHelper.hasNotificationPermission(context)) {
                    AppRestrictionService.startService(context)
                }
            }
        }
    }
}