package com.proyectoing.glocosasmarai.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.proyectoing.glocosasmarai.database.dao.*
import com.proyectoing.glocosasmarai.database.entities.*

@Database(
    entities = [
        GlucoseEntryEntity::class,
        FoodEntryEntity::class,
        EmergencyContactEntity::class,
        ChatMessageEntity::class,
        ConversationEntity::class,
        UserProfileEntity::class,
        AppSettingsEntity::class,
        MedicationEntity::class,
        MedicationHistoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun glucoseEntryDao(): GlucoseEntryDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationHistoryDao(): MedicationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glucosa_smart_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}