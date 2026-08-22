package com.proyectoing.glocosasmarai.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.proyectoing.glocosasmarai.database.dao.*
import com.proyectoing.glocosasmarai.database.entities.*

/**
 * Base de datos principal de la aplicación usando Room
 */
@Database(
    entities = [
        GlucoseEntryEntity::class,
        FoodEntryEntity::class,
        EmergencyContactEntity::class,
        ChatMessageEntity::class,
        ConversationEntity::class,
        UserProfileEntity::class,
        AppSettingsEntity::class,
        MedicationEntity::class  // <-- AÑADIDO
    ],
    version = 3, // <-- MODIFICADO DE 1 A 2
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // DAOs
    abstract fun glucoseEntryDao(): GlucoseEntryDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun medicationDao(): MedicationDao
    
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
                .fallbackToDestructiveMigration() // Para desarrollo, en producción usar migraciones
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
