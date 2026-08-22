package com.proyectoing.glocosasmarai.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val dose: String,
    val hour: Int,
    val minute: Int,
    val endDate: Long,
    val trackingState: String,
    val type: String = "Medicamento", // <-- NUEVO CAMPO
    val timesTaken: Int = 0
)