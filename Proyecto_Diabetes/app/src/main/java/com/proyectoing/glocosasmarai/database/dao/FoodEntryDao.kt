package com.proyectoing.glocosasmarai.database.dao

import androidx.room.*
import com.proyectoing.glocosasmarai.database.entities.FoodEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con registros de comida
 */
@Dao
interface FoodEntryDao {
    
    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC")
    fun getAllFoodEntries(): Flow<List<FoodEntryEntity>>
    
    @Query("SELECT * FROM food_entries WHERE id = :id")
    suspend fun getFoodEntryById(id: Long): FoodEntryEntity?
    
    @Query("SELECT * FROM food_entries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getFoodEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<FoodEntryEntity>>
    
    @Query("SELECT * FROM food_entries WHERE type = :type ORDER BY timestamp DESC")
    fun getFoodEntriesByType(type: String): Flow<List<FoodEntryEntity>>
    
    @Query("SELECT * FROM food_entries WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getFoodEntriesFromDate(startTime: Long): Flow<List<FoodEntryEntity>>
    
    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestFoodEntry(): FoodEntryEntity?
    
    @Query("SELECT SUM(calories) FROM food_entries WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalCaloriesInRange(startTime: Long, endTime: Long): Int?
    
    @Query("SELECT SUM(carbohydrates) FROM food_entries WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalCarbohydratesInRange(startTime: Long, endTime: Long): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntry(foodEntry: FoodEntryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntries(foodEntries: List<FoodEntryEntity>)
    
    @Update
    suspend fun updateFoodEntry(foodEntry: FoodEntryEntity)
    
    @Delete
    suspend fun deleteFoodEntry(foodEntry: FoodEntryEntity)
    
    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteFoodEntryById(id: Long)
    
    @Query("DELETE FROM food_entries")
    suspend fun deleteAllFoodEntries()
}
