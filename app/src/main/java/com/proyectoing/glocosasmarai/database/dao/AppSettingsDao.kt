package com.proyectoing.glocosasmarai.database.dao

import androidx.room.*
import com.proyectoing.glocosasmarai.database.entities.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingsEntity>>

    @Query("SELECT * FROM app_settings WHERE key = :key")
    suspend fun getSettingByKey(key: String): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE key = :key")
    fun getSettingByKeyFlow(key: String): Flow<AppSettingsEntity?>

    @Query("SELECT value FROM app_settings WHERE key = :key")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT value FROM app_settings WHERE key = :key")
    fun getSettingValueFlow(key: String): Flow<String?>

    @Query("SELECT * FROM app_settings WHERE type = :type")
    fun getSettingsByType(type: String): Flow<List<AppSettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<AppSettingsEntity>)

    @Update
    suspend fun updateSetting(setting: AppSettingsEntity)

    @Delete
    suspend fun deleteSetting(setting: AppSettingsEntity)

    @Query("DELETE FROM app_settings WHERE key = :key")
    suspend fun deleteSettingByKey(key: String)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAllSettings()

    @Query("UPDATE app_settings SET value = :value, updatedAt = :updatedAt WHERE key = :key")
    suspend fun updateSettingValue(key: String, value: String, updatedAt: Long = System.currentTimeMillis())
}