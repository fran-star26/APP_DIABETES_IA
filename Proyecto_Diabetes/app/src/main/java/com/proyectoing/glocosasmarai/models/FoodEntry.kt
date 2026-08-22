package com.proyectoing.glocosasmarai.models

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
            calories?.let { append("$it cal") }
            if (calories != null && carbohydrates != null) append(" • ")
            carbohydrates?.let { append("$it g carb") }
        }
    }
}
