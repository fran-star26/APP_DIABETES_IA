package com.proyectoing.glocosasmarai.models

/**
 * Modelo para contactos de emergencia
 */
data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val relationship: String? = null, // "Médico", "Familiar", "Amigo", etc.
    val isPrimary: Boolean = false
) {
    /**
     * Obtiene el emoji asociado a la relación
     */
    fun getRelationshipEmoji(): String {
        return when (relationship?.lowercase()) {
            "médico", "doctor" -> "👨‍⚕️"
            "familiar", "familia" -> "👨‍👩‍👧‍👦"
            "amigo", "amiga" -> "👫"
            "pareja", "esposo", "esposa" -> "💑"
            "hermano", "hermana" -> "👫"
            "padre", "madre" -> "👨‍👩‍👧‍👦"
            else -> "📞"
        }
    }
    
    /**
     * Obtiene el nombre completo con emoji
     */
    fun getDisplayName(): String {
        return "${getRelationshipEmoji()} $name"
    }
    
    /**
     * Verifica si el número de teléfono es válido
     */
    fun isValidPhone(): Boolean {
        return phone.matches(Regex("^[+]?[0-9\\s\\-()]{7,15}$"))
    }
    
    /**
     * Obtiene el número de teléfono formateado
     */
    fun getFormattedPhone(): String {
        return if (phone.startsWith("+")) {
            phone
        } else {
            "+52 $phone"
        }
    }
}
