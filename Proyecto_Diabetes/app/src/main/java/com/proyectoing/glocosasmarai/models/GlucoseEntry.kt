package com.proyectoing.glocosasmarai.models

/**
 * Modelo para registros de glucosa
 */
data class GlucoseEntry(
    val id: Long = 0,
    val value: Int,
    val timestamp: Long,
    val isBeforeMeal: Boolean,
    val notes: String? = null
) {
    /**
     * Obtiene el estado de la glucosa basado en el valor
     */
    fun getGlucoseStatus(): GlucoseStatus {
        return when {
            value < 70 -> GlucoseStatus.LOW
            value > 180 -> GlucoseStatus.HIGH
            else -> GlucoseStatus.NORMAL
        }
    }
    
    /**
     * Obtiene el color asociado al estado de glucosa
     */
    fun getStatusColor(): String {
        return when (getGlucoseStatus()) {
            GlucoseStatus.LOW -> "#FF6B6B" // Rojo
            GlucoseStatus.HIGH -> "#FFA726" // Naranja
            GlucoseStatus.NORMAL -> "#4CAF50" // Verde
        }
    }
    
    /**
     * Verifica si es una lectura crítica
     */
    fun isCritical(): Boolean {
        return value < 70 || value > 250
    }
}

/**
 * Estados de glucosa
 */
enum class GlucoseStatus {
    LOW,
    NORMAL,
    HIGH
}
