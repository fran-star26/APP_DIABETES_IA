package com.proyectoing.glocosasmarai.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad para almacenar registros de glucosa en la base de datos local
 */
@Entity(tableName = "glucose_entries")
data class GlucoseEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: Int,
    val timestamp: Long,
    val isBeforeMeal: Boolean,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
