package com.example.timeblessedlauncher.ViewModel

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.timeblessedlauncher.Data.*

class AppViewModel(private val context: Context) : ViewModel() {

    private val database = AppDatabase.getDatabase(context)
    private val dao = database.appRestrictionDao()

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _visibleApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val visibleApps: StateFlow<List<AppInfo>> = _visibleApps.asStateFlow()

    private val _hiddenAppsCount = MutableStateFlow(0)
    val hiddenAppsCount: StateFlow<Int> = _hiddenAppsCount.asStateFlow()

    // Get all app restrictions from database
    val appRestrictions: StateFlow<List<AppRestriction>> = dao.getAllRestrictions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Observe restrictions and update visible apps when they change
        viewModelScope.launch {
            appRestrictions.collect {
                updateVisibleApps()
            }
        }
    }

    fun loadApps(packageManager: PackageManager) {
        viewModelScope.launch {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
            intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)

            val apps = resolveInfoList.map { resolveInfo ->
                AppInfo(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    category = determineAppCategory(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.loadLabel(packageManager).toString()
                    )
                )
            }.sortedBy { it.name.lowercase() }

            _allApps.value = apps
            updateVisibleApps()
        }
    }

    private fun determineAppCategory(packageName: String, appName: String): AppCategory {
        return when {
            // Essential Apps
            packageName.contains("phone") ||
                    packageName.contains("contacts") ||
                    packageName.contains("messages") ||
                    packageName.contains("camera") ||
                    packageName.contains("maps") ||
                    packageName.contains("calendar") ||
                    packageName.contains("clock") ||
                    packageName.contains("calculator") ||
                    packageName.contains("settings") ||
                    packageName.contains("whatsapp") ||
                    packageName.contains("telegram") -> AppCategory.ESSENTIAL

            // Social Media
            packageName.contains("instagram") ||
                    packageName.contains("facebook") ||
                    packageName.contains("twitter") ||
                    packageName.contains("tiktok") ||
                    packageName.contains("snapchat") ||
                    packageName.contains("discord") -> AppCategory.SOCIAL

            // Work Apps
            packageName.contains("gmail") ||
                    packageName.contains("outlook") ||
                    packageName.contains("slack") ||
                    packageName.contains("teams") ||
                    packageName.contains("zoom") ||
                    packageName.contains("office") -> AppCategory.WORK

            // Entertainment
            packageName.contains("netflix") ||
                    packageName.contains("youtube") ||
                    packageName.contains("spotify") ||
                    packageName.contains("prime") ||
                    packageName.contains("disney") ||
                    packageName.contains("music") -> AppCategory.ENTERTAINMENT

            // Gaming
            packageName.contains("game") ||
                    appName.lowercase().contains("game") -> AppCategory.GAMING

            // News
            packageName.contains("news") ||
                    packageName.contains("reddit") ||
                    packageName.contains("medium") -> AppCategory.NEWS

            // Shopping
            packageName.contains("amazon") ||
                    packageName.contains("flipkart") ||
                    packageName.contains("shop") ||
                    packageName.contains("store") -> AppCategory.SHOPPING

            else -> AppCategory.OTHER
        }
    }

    fun updateVisibleApps() {
        viewModelScope.launch {
            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentTime.get(Calendar.MINUTE)
            val currentTotalMinutes = currentHour * 60 + currentMinute

            val restrictions = appRestrictions.value
            val visible = _allApps.value.filter { app ->
                val restriction = restrictions.find { it.packageName == app.packageName }

                if (restriction == null || !restriction.isRestricted) {
                    // No restriction or not restricted - always visible
                    true
                } else {
                    // Check if current time is within allowed range
                    val fromTotalMinutes = restriction.showFromHour * 60
                    val toTotalMinutes = restriction.showUntilHour * 60

                    when {
                        fromTotalMinutes <= toTotalMinutes -> {
                            // Same day restriction (e.g., 9 AM to 5 PM)
                            currentTotalMinutes in fromTotalMinutes..toTotalMinutes
                        }
                        else -> {
                            // Overnight restriction (e.g., 10 PM to 6 AM)
                            currentTotalMinutes >= fromTotalMinutes || currentTotalMinutes <= toTotalMinutes
                        }
                    }
                }
            }

            _visibleApps.value = visible
            _hiddenAppsCount.value = _allApps.value.size - visible.size
        }
    }

    fun setAppRestriction(
        packageName: String,
        appName: String,
        fromHour: Int,
        toHour: Int,
        fromMinute: Int = 0,
        toMinute: Int = 0
    ) {
        viewModelScope.launch {
            val restriction = AppRestriction(
                packageName = packageName,
                appName = appName,
                isRestricted = true,
                showFromHour = fromHour,
                showUntilHour = toHour
            )
            dao.insertOrUpdate(restriction)
        }
    }

    fun removeRestriction(packageName: String) {
        viewModelScope.launch {
            dao.deleteByPackageName(packageName)
        }
    }

    fun clearAllRestrictions() {
        viewModelScope.launch {
            val allRestrictions = dao.getAllRestrictions().first()
            allRestrictions.forEach { restriction ->
                dao.delete(restriction)
            }
        }
    }

    fun getNextAvailableTime(app: AppInfo): String {
        val restriction = appRestrictions.value.find { it.packageName == app.packageName }
        if (restriction == null || !restriction.isRestricted) {
            return "Available now"
        }

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            currentHour < restriction.showFromHour -> {
                "Available at ${formatHour(restriction.showFromHour)}"
            }
            currentHour > restriction.showUntilHour -> {
                "Available tomorrow at ${formatHour(restriction.showFromHour)}"
            }
            else -> "Available now"
        }
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12:00 AM"
            hour < 12 -> "${hour}:00 AM"
            hour == 12 -> "12:00 PM"
            else -> "${hour - 12}:00 PM"
        }
    }

    fun getRestrictedAppsCount(): Int {
        return appRestrictions.value.count { it.isRestricted }
    }

    fun isAppCurrentlyRestricted(packageName: String): Boolean {
        val restriction = appRestrictions.value.find { it.packageName == packageName }
        if (restriction == null || !restriction.isRestricted) return false

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val fromHour = restriction.showFromHour
        val toHour = restriction.showUntilHour

        return when {
            fromHour <= toHour -> {
                currentHour !in fromHour..toHour
            }
            else -> {
                currentHour !in fromHour..23 && currentHour !in 0..toHour
            }
        }
    }
}