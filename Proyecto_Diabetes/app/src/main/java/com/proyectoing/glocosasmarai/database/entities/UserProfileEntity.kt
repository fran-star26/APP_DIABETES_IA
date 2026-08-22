package com.proyectoing.glocosasmarai.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para almacenar el perfil del usuario en la base de datos local
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: String = "default_user",
    val name: String? = null,
    val age: Int? = null,
    val diabetesType: String? = null, // "Tipo 1", "Tipo 2", "Gestacional", etc.
    val weight: Float? = null,
    val height: Float? = null,
    val diagnosisDate: Long? = null,
    val doctorName: String? = null,
    val doctorPhone: String? = null,
    val medication: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
