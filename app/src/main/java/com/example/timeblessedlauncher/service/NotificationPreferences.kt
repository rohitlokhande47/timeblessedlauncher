package com.example.timeblessedlauncher.Service

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "notification_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_NOTIFICATION_VIBRATION = "notification_vibration"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_HOURS_START = "quiet_hours_start"
        private const val KEY_QUIET_HOURS_END = "quiet_hours_end"
    }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var notificationSound: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_SOUND, false) // Silent by default
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_SOUND, value).apply()

    var notificationVibration: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_VIBRATION, value).apply()

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, value).apply()

    var quietHoursStart: Int
        get() = prefs.getInt(KEY_QUIET_HOURS_START, 22) // 10 PM
        set(value) = prefs.edit().putInt(KEY_QUIET_HOURS_START, value).apply()

    var quietHoursEnd: Int
        get() = prefs.getInt(KEY_QUIET_HOURS_END, 7) // 7 AM
        set(value) = prefs.edit().putInt(KEY_QUIET_HOURS_END, value).apply()

    fun isInQuietHours(): Boolean {
        if (!quietHoursEnabled) return false

        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        return if (quietHoursStart <= quietHoursEnd) {
            currentHour in quietHoursStart..quietHoursEnd
        } else {
            currentHour >= quietHoursStart || currentHour <= quietHoursEnd
        }
    }
}