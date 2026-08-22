package com.proyectoing.glocosasmarai.database.dao

import androidx.room.*
import com.proyectoing.glocosasmarai.database.entities.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications")
    suspend fun deleteAllMedications()

    @Query("SELECT * FROM medications ORDER BY hour, minute ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("UPDATE medications SET trackingState = :newState WHERE id = :id")
    suspend fun updateState(id: Long, newState: String)

}