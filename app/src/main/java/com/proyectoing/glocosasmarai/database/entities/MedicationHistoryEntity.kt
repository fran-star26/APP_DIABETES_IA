package com.proyectoing.glocosasmarai.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey



@Entity(
    tableName = "medication_history",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE // Si borras la medicina, se borra el historial
        )
    ],
    indices = [Index(value = ["medicationId"])] // Para búsqueda rápida
)
data class MedicationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val historyId: Long = 0,
    val medicationId: Long,     // Vinculado al medicamento original
    val timestamp: Long,        // Fecha y hora exacta de la acción
    val status: String,         // "TAKEN", "MISSED", "SNOOZED"
    val doseTaken: String       // Guardamos la dosis por si cambia en el futuro
)