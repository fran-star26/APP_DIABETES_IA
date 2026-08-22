package com.proyectoing.glocosasmarai.chatbot
/**
 *
 *Este archivo es un documento de configuración que contiene todas las constantes y reglas que el chatbot utiliza para funcionar
 *
 * Configuración del chatbot
 */
object ChatbotConfig {
    
    // Configuración de respuestas
    const val DEFAULT_CONFIDENCE = 0.85
    const val EMERGENCY_GLUCOSE_LOW = 70
    const val EMERGENCY_GLUCOSE_HIGH = 250
    const val NORMAL_GLUCOSE_MIN = 70
    const val NORMAL_GLUCOSE_MAX = 140
    
    // Configuración de UI
    const val MAX_MESSAGE_LENGTH = 500
    const val MAX_CONVERSATION_TITLE_LENGTH = 30
    const val MAX_SAVED_CONVERSATIONS = 50
    
    // Configuración de almacenamiento
    const val JSON_FILE_PREFIX_INPUT = "input_"
    const val JSON_FILE_PREFIX_OUTPUT = "output_"
    const val JSON_FILE_EXTENSION = ".json"
    const val STORAGE_DIR_NAME = "chatbot_conversations"
    
    // Configuración de tiempo
    const val MESSAGE_TIMEOUT_MS = 30000L // 30 segundos
    const val CONVERSATION_AUTO_SAVE_INTERVAL_MS = 60000L // 1 minuto
    
    // Configuración de respuestas por defecto
    val DEFAULT_RESPONSES = listOf(
        "Entiendo tu pregunta sobre diabetes. Te recomiendo consultar con tu médico para obtener información más específica.",
        "Es importante mantener un control regular de tu glucosa. ¿Has medido tus niveles hoy?",
        "Recuerda que una dieta balanceada es fundamental para el control de la diabetes.",
        "La actividad física regular puede ayudar a mantener tus niveles de glucosa estables.",
        "¿Te has tomado tu medicación hoy? Es importante seguir el tratamiento prescrito.",
        "Si tienes dudas sobre tu tratamiento, consulta siempre con tu equipo médico.",
        "Mantener un registro de tus niveles de glucosa te ayudará a identificar patrones.",
        "¿Has experimentado algún síntoma inusual? Es importante estar atento a los cambios.",
        "Recuerda que el estrés puede afectar tus niveles de glucosa. Intenta mantener la calma.",
        "¿Has tenido alguna consulta médica recientemente? Es bueno mantener un seguimiento regular."
    )
    
    // Palabras clave para diferentes tipos de consultas
    val GLUCOSE_KEYWORDS = listOf("glucosa", "azúcar", "sangre", "nivel", "medición")
    val DIET_KEYWORDS = listOf("comida", "aliment", "dieta", "comer", "nutrición", "menú")
    val EXERCISE_KEYWORDS = listOf("ejercicio", "actividad", "deporte", "caminar", "correr", "gimnasio")
    val MEDICATION_KEYWORDS = listOf("medic", "tratamiento", "pastilla", "inyección", "insulina")
    val EMERGENCY_KEYWORDS = listOf("emergencia", "urgencia", "ayuda", "mal", "síntoma", "dolor")
    
    // Tipos de sugerencias disponibles
    val SUGGESTION_TYPES = listOf(
        "diet" to "Alimentación",
        "exercise" to "Ejercicio",
        "medication" to "Medicación",
        "emergency" to "Emergencia",
        "monitoring" to "Monitoreo",
        "information" to "Información"
    )
    
    // Prioridades de sugerencias
    val SUGGESTION_PRIORITIES = listOf(
        "high" to "Alta",
        "medium" to "Media",
        "low" to "Baja"
    )
    
    // Intents del chatbot
    val CHATBOT_INTENTS = listOf(
        "diet_advice" to "Consejo de alimentación",
        "glucose_high" to "Glucosa alta",
        "glucose_low" to "Glucosa baja",
        "glucose_normal" to "Glucosa normal",
        "exercise_advice" to "Consejo de ejercicio",
        "medication_advice" to "Consejo de medicación",
        "emergency" to "Emergencia",
        "general_inquiry" to "Consulta general"
    )
    
    /**
     * Verifica si un nivel de glucosa es crítico
     */
    fun isGlucoseCritical(glucoseLevel: Int): Boolean {
        return glucoseLevel < EMERGENCY_GLUCOSE_LOW || glucoseLevel > EMERGENCY_GLUCOSE_HIGH
    }
    
    /**
     * Verifica si un nivel de glucosa es normal
     */
    fun isGlucoseNormal(glucoseLevel: Int): Boolean {
        return glucoseLevel in NORMAL_GLUCOSE_MIN..NORMAL_GLUCOSE_MAX
    }
    
    /**
     * Obtiene el estado de glucosa basado en el nivel
     */
    fun getGlucoseStatus(glucoseLevel: Int): GlucoseStatus {
        return when {
            glucoseLevel < EMERGENCY_GLUCOSE_LOW -> GlucoseStatus.CRITICAL_LOW
            glucoseLevel > EMERGENCY_GLUCOSE_HIGH -> GlucoseStatus.CRITICAL_HIGH
            glucoseLevel < NORMAL_GLUCOSE_MIN -> GlucoseStatus.LOW
            glucoseLevel > NORMAL_GLUCOSE_MAX -> GlucoseStatus.HIGH
            else -> GlucoseStatus.NORMAL
        }
    }
    
    /**
     * Obtiene una respuesta por defecto aleatoria
     */
    fun getRandomDefaultResponse(): String {
        return DEFAULT_RESPONSES.random()
    }
    
    /**
     * Verifica si un mensaje contiene palabras clave de emergencia
     */
    fun containsEmergencyKeywords(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return EMERGENCY_KEYWORDS.any { keyword -> lowerMessage.contains(keyword) }
    }
    
    /**
     * Obtiene el tipo de consulta basado en las palabras clave
     */
    fun getQueryType(message: String): QueryType {
        val lowerMessage = message.lowercase()
        
        return when {
            GLUCOSE_KEYWORDS.any { keyword -> lowerMessage.contains(keyword) } -> QueryType.GLUCOSE
            DIET_KEYWORDS.any { keyword -> lowerMessage.contains(keyword) } -> QueryType.DIET
            EXERCISE_KEYWORDS.any { keyword -> lowerMessage.contains(keyword) } -> QueryType.EXERCISE
            MEDICATION_KEYWORDS.any { keyword -> lowerMessage.contains(keyword) } -> QueryType.MEDICATION
            EMERGENCY_KEYWORDS.any { keyword -> lowerMessage.contains(keyword) } -> QueryType.EMERGENCY
            else -> QueryType.GENERAL
        }
    }
}

/**
 * Estados de glucosa
 */
enum class GlucoseStatus {
    NORMAL,
    LOW,
    HIGH,
    CRITICAL_LOW,
    CRITICAL_HIGH
}

/**
 * Tipos de consulta
 */
enum class QueryType {
    GLUCOSE,
    DIET,
    EXERCISE,
    MEDICATION,
    EMERGENCY,
    GENERAL
}
