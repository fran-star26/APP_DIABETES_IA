package com.proyectoing.glocosasmarai.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.proyectoing.glocosasmarai.database.entities.MedicationHistoryEntity
import com.proyectoing.glocosasmarai.models.MissedMedicationSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationHistoryDao {
    @Insert
    suspend fun insertLog(history: MedicationHistoryEntity)

    // Obtener historial de un medicamento específico
    @Query("SELECT * FROM medication_history WHERE medicationId = :medId ORDER BY timestamp DESC")
    fun getHistoryForMedication(medId: Long): Flow<List<MedicationHistoryEntity>>

    // Obtener todo el historial (útil para reportes generales)
    @Query("SELECT * FROM medication_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<MedicationHistoryEntity>>

    @Query("""
        SELECT m.name, m.type, h.timestamp 
        FROM medication_history h 
        INNER JOIN medications m ON h.medicationId = m.id 
        WHERE h.status = 'MISSED' 
        AND h.timestamp BETWEEN :startDate AND :endDate
        ORDER BY h.timestamp ASC
    """)
    fun getMissedMedicationsInDateRange(startDate: Long, endDate: Long): Flow<List<MissedMedicationSummary>>
}