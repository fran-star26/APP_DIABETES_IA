package com.proyectoing.glocosasmarai.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para almacenar registros de comida en la base de datos local
 */
@Entity(tableName = "food_entries")
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "Desayuno", "Almuerzo", "Cena", "Snack"
    val description: String,
    val timestamp: Long,
    val calories: Int? = null,
    val carbohydrates: Int? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
