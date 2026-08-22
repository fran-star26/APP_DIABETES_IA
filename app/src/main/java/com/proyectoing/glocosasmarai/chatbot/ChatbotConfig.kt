package com.proyectoing.glocosasmarai.chatbot

/**
 * Constantes y reglas de configuración para el funcionamiento del chatbot.
 */
object ChatbotConfig {

    const val DEFAULT_CONFIDENCE = 0.85
    const val EMERGENCY_GLUCOSE_LOW = 70
    const val EMERGENCY_GLUCOSE_HIGH = 250
    const val NORMAL_GLUCOSE_MIN = 70
    const val NORMAL_GLUCOSE_MAX = 140

    const val MAX_MESSAGE_LENGTH = 500
    const val MAX_CONVERSATION_TITLE_LENGTH = 30
    const val MAX_SAVED_CONVERSATIONS = 50

    const val JSON_FILE_PREFIX_INPUT = "input_"
    const val JSON_FILE_PREFIX_OUTPUT = "output_"
    const val JSON_FILE_EXTENSION = ".json"
    const val STORAGE_DIR_NAME = "chatbot_conversations"

    const val MESSAGE_TIMEOUT_MS = 30000L
    const val CONVERSATION_AUTO_SAVE_INTERVAL_MS = 60000L

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

    val GLUCOSE_KEYWORDS = listOf("glucosa", "azúcar", "sangre", "nivel", "medición")
    val DIET_KEYWORDS = listOf("comida", "aliment", "dieta", "comer", "nutrición", "menú")
    val EXERCISE_KEYWORDS = listOf("ejercicio", "actividad", "deporte", "caminar", "correr", "gimnasio")
    val MEDICATION_KEYWORDS = listOf("medic", "tratamiento", "pastilla", "inyección", "insulina")
    val EMERGENCY_KEYWORDS = listOf("emergencia", "urgencia", "ayuda", "mal", "síntoma", "dolor")

    val SUGGESTION_TYPES = listOf(
        "diet" to "Alimentación",
        "exercise" to "Ejercicio",
        "medication" to "Medicación",
        "emergency" to "Emergencia",
        "monitoring" to "Monitoreo",
        "information" to "Información"
    )

    val SUGGESTION_PRIORITIES = listOf(
        "high" to "Alta",
        "medium" to "Media",
        "low" to "Baja"
    )

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

    fun isGlucoseCritical(glucoseLevel: Int): Boolean {
        return glucoseLevel < EMERGENCY_GLUCOSE_LOW || glucoseLevel > EMERGENCY_GLUCOSE_HIGH
    }

    fun isGlucoseNormal(glucoseLevel: Int): Boolean {
        return glucoseLevel in NORMAL_GLUCOSE_MIN..NORMAL_GLUCOSE_MAX
    }

    fun getGlucoseStatus(glucoseLevel: Int): GlucoseStatus {
        return when {
            glucoseLevel < EMERGENCY_GLUCOSE_LOW -> GlucoseStatus.CRITICAL_LOW
            glucoseLevel > EMERGENCY_GLUCOSE_HIGH -> GlucoseStatus.CRITICAL_HIGH
            glucoseLevel < NORMAL_GLUCOSE_MIN -> GlucoseStatus.LOW
            glucoseLevel > NORMAL_GLUCOSE_MAX -> GlucoseStatus.HIGH
            else -> GlucoseStatus.NORMAL
        }
    }

    fun getRandomDefaultResponse(): String {
        return DEFAULT_RESPONSES.random()
    }

    fun containsEmergencyKeywords(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return EMERGENCY_KEYWORDS.any { keyword -> lowerMessage.contains(keyword) }
    }

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

enum class GlucoseStatus {
    NORMAL,
    LOW,
    HIGH,
    CRITICAL_LOW,
    CRITICAL_HIGH
}

enum class QueryType {
    GLUCOSE,
    DIET,
    EXERCISE,
    MEDICATION,
    EMERGENCY,
    GENERAL
}