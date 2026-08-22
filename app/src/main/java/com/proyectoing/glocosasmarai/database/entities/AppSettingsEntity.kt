package com.proyectoing.glocosasmarai.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para almacenar configuraciones de la aplicación en la base de datos local
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val type: String, // "string", "int", "boolean", "float"
    val description: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
