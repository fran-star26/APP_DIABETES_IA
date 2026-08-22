package com.proyectoing.glocosasmarai.models

import kotlinx.serialization.Serializable

@Serializable
/**
 * Modelo para el perfil del usuario
 */
data class UserProfile(
    val id: String = "default_user",
    val name: String? = null,
    val age: Int? = null,
    val diabetesType: String? = null,
    val weight: Float? = null, // en kg
    val height: Float? = null, // en cm
    val diagnosisDate: Long? = null, // timestamp
    val doctorName: String? = null,
    val doctorPhone: String? = null,
    val medication: String? = null,
    val notes: String? = null,
) {
    /**
     * Calcula el IMC (Índice de Masa Corporal)
     */
    fun calculateBMI(): Float? {
        return if (weight != null && height != null && height > 0) {
            val heightInMeters = height / 100f
            weight / (heightInMeters * heightInMeters)
        } else null
    }
    
    /**
     * Obtiene la categoría del IMC
     */
    fun getBMICategory(): String? {
        val bmi = calculateBMI() ?: return null
        return when {
            bmi < 18.5f -> "Bajo peso"
            bmi < 25f -> "Peso normal"
            bmi < 30f -> "Sobrepeso"
            bmi < 35f -> "Obesidad Clase 1"
            bmi < 40f -> "Obesidad Clase 2"
            else -> "Obesidad Clase 3"
        }
    }
    
    /**
     * Verifica si el perfil está completo
     */
    fun isComplete(): Boolean {
        return name != null && age != null && weight != null && height != null
    }
    
    /**
     * Obtiene la edad en años desde el diagnóstico
     */
    fun getYearsSinceDiagnosis(): Int? {
        return if (diagnosisDate != null) {
            val currentTime = System.currentTimeMillis()
            val years = (currentTime - diagnosisDate) / (365L * 24 * 60 * 60 * 1000)
            years.toInt()
        } else null
    }
}
