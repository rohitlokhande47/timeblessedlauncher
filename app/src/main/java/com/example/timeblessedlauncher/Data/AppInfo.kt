package com.example.timeblessedlauncher.Data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class AppInfo(
    val name: String,
    val packageName: String,
    val category: AppCategory = AppCategory.OTHER,
    val iconColor: Long? = null
)

@Entity(tableName = "app_restrictions")
data class AppRestriction(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isRestricted: Boolean = false,
    val showFromHour: Int = 0,
    val showUntilHour: Int = 23,
    val customLabel: String? = null
)

@Entity(tableName = "favorite_apps")
data class FavoriteApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val addedAt: Long = System.currentTimeMillis()
)

enum class AppCategory {
    ESSENTIAL,      // Phone, Messages, Camera, etc.
    SOCIAL,         // Instagram, Facebook, Twitter
    WORK,           // Gmail, Slack, Teams
    ENTERTAINMENT,  // Netflix, YouTube, Spotify
    GAMING,         // Games
    NEWS,           // News apps, Reddit
    SHOPPING,       // Amazon, Flipkart
    OTHER           // Everything else
}

// Data class for time selection
data class TimeRange(
    val fromHour: Int,
    val fromMinute: Int = 0,
    val toHour: Int,
    val toMinute: Int = 0
) {
    fun isValid(): Boolean {
        val fromTotalMinutes = fromHour * 60 + fromMinute
        val toTotalMinutes = if (toHour < fromHour) {
            // Next day case
            (toHour + 24) * 60 + toMinute
        } else {
            toHour * 60 + toMinute
        }

        val durationMinutes = toTotalMinutes - fromTotalMinutes
        return durationMinutes >= 60 && durationMinutes <= 120 // Minimum 1 hour, Maximum 2 hours
    }

    fun getDurationHours(): Float {
        val fromTotalMinutes = fromHour * 60 + fromMinute
        val toTotalMinutes = if (toHour < fromHour) {
            (toHour + 24) * 60 + toMinute
        } else {
            toHour * 60 + toMinute
        }

        return (toTotalMinutes - fromTotalMinutes) / 60f
    }

    fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, period)
    }

    override fun toString(): String {
        return "${formatTime(fromHour, fromMinute)} - ${formatTime(toHour, toMinute)}"
    }
}

@Dao
interface AppRestrictionDao {
    @Query("SELECT * FROM app_restrictions")
    fun getAllRestrictions(): Flow<List<AppRestriction>>

    @Query("SELECT * FROM app_restrictions WHERE packageName = :packageName")
    suspend fun getRestriction(packageName: String): AppRestriction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(restriction: AppRestriction)

    @Delete
    suspend fun delete(restriction: AppRestriction)

    @Query("DELETE FROM app_restrictions WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT * FROM app_restrictions WHERE isRestricted = 1")
    fun getRestrictedApps(): Flow<List<AppRestriction>>
}

@Dao
interface FavoriteAppDao {
    @Query("SELECT * FROM favorite_apps ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteApp>>

    @Query("SELECT * FROM favorite_apps WHERE packageName = :packageName")
    suspend fun getFavorite(packageName: String): FavoriteApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoriteApp: FavoriteApp)

    @Delete
    suspend fun delete(favoriteApp: FavoriteApp)

    @Query("DELETE FROM favorite_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM favorite_apps")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM favorite_apps")
    suspend fun getFavoriteCount(): Int
}

@Database(
    entities = [AppRestriction::class, FavoriteApp::class],
    version = 2, // Increment version due to new entity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appRestrictionDao(): AppRestrictionDao
    abstract fun favoriteAppDao(): FavoriteAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration() // For simplicity, you can implement proper migration later
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}