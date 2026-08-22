package com.proyectoing.glocosasmarai.models

import kotlinx.serialization.Serializable

@Serializable
/**
 * Modelo para registros de comida
 */
data class FoodEntry(
    val id: Long = 0,
    val type: String, // "Desayuno", "Almuerzo", "Cena", "Snack"
    val description: String,
    val timestamp: Long,
    val calories: Int? = null,
    val carbohydrates: Int? = null,
    val sugars: Int? = null,
    val notes: String? = null
) {
    /**
     * Obtiene el tipo de comida con emoji
     */
    fun getTypeWithEmoji(): String {
        return when (type.lowercase()) {
            "desayuno" -> "🌅 $type"
            "almuerzo" -> "☀️ $type"
            "cena" -> "🌙 $type"
            "snack" -> "🍎 $type"
            else -> "🍽️ $type"
        }
    }
    
    /**
     * Verifica si tiene información nutricional completa
     */
    fun hasNutritionalInfo(): Boolean {
        return calories != null && carbohydrates != null
    }
    
    /**
     * Obtiene un resumen de la información nutricional
     */
    fun getNutritionalSummary(): String {
        return buildString {
            calories?.let { append("$it kcal") }

            // Separador si hay calorías y (carbs o azúcares)
            if (calories != null && (carbohydrates != null || sugars != null)) append(" • ")

            carbohydrates?.let { append("Carbs: ${it}g") }

            // Separador si hay carbs y azúcares
            if (carbohydrates != null && sugars != null) append(" • ")

            sugars?.let { append("Azúcar: ${it}g") }
        }
    }
}
