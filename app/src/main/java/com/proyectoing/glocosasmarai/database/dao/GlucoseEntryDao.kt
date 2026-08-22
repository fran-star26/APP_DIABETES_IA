package com.proyectoing.glocosasmarai.database.dao

import androidx.room.*
import com.proyectoing.glocosasmarai.database.entities.GlucoseEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con registros de glucosa
 */
@Dao
interface GlucoseEntryDao {
    
    @Query("SELECT * FROM glucose_entries ORDER BY timestamp DESC")
    fun getAllGlucoseEntries(): Flow<List<GlucoseEntryEntity>>
    
    @Query("SELECT * FROM glucose_entries WHERE id = :id")
    suspend fun getGlucoseEntryById(id: Long): GlucoseEntryEntity?
    
    @Query("SELECT * FROM glucose_entries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getGlucoseEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<GlucoseEntryEntity>>
    
    @Query("SELECT * FROM glucose_entries WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getGlucoseEntriesFromDate(startTime: Long): Flow<List<GlucoseEntryEntity>>
    
    @Query("SELECT * FROM glucose_entries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestGlucoseEntry(): GlucoseEntryEntity?
    
    @Query("SELECT AVG(value) FROM glucose_entries WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageGlucoseInRange(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT COUNT(*) FROM glucose_entries WHERE value < 70 AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getLowGlucoseCount(startTime: Long, endTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM glucose_entries WHERE value > 180 AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getHighGlucoseCount(startTime: Long, endTime: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlucoseEntry(glucoseEntry: GlucoseEntryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlucoseEntries(glucoseEntries: List<GlucoseEntryEntity>)
    
    @Update
    suspend fun updateGlucoseEntry(glucoseEntry: GlucoseEntryEntity)
    
    @Delete
    suspend fun deleteGlucoseEntry(glucoseEntry: GlucoseEntryEntity)
    
    @Query("DELETE FROM glucose_entries WHERE id = :id")
    suspend fun deleteGlucoseEntryById(id: Long)
    
    @Query("DELETE FROM glucose_entries")
    suspend fun deleteAllGlucoseEntries()
}
