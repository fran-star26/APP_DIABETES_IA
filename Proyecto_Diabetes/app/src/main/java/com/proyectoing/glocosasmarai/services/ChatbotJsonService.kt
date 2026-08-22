package com.proyectoing.glocosasmarai.services
/**
 *
 *Este servicio actúa como un traductor que maneja el lenguaje JSON del chatbot. 
 *
 */
import com.proyectoing.glocosasmarai.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Clase auxiliar para manejar datos de respuesta
 */
data class ResponseData(
    val response: String,
    val intent: String,
    val suggestions: List<ChatbotSuggestion>,
    val followUpQuestions: List<String>
)

/**
 * Servicio para manejar la compresión y descompresión de conversaciones del chatbot a JSON
 */
class ChatbotJsonService {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * Convierte una conversación a JSON de entrada para el chatbot
     * @param userId ID del usuario
     * @param userMessage Mensaje del usuario
     * @param chatMessages Lista de mensajes de la conversación
     * @param glucoseLevel Nivel de glucosa actual (opcional)
     * @param lastMeal Última comida (opcional)
     * @param medicationTaken Si se tomó la medicación (opcional)
     * @return JSON string para enviar al chatbot
     */
    fun compressToInputJson(
        userId: String,
        userMessage: String,
        chatMessages: List<ChatMessage>,
        glucoseLevel: Int? = null,
        lastMeal: String? = null,
        medicationTaken: Boolean? = null
    ): String {
        // Convertir mensajes a formato de historial
        val conversationHistory = chatMessages.map { message ->
            ConversationHistoryMessage(
                role = if (message.isUser) "user" else "assistant",
                content = message.text,
                timestamp = message.timestamp
            )
        }
        
        // Crear contexto
        val context = ChatContext(
            glucose_level = glucoseLevel,
            last_meal = lastMeal,
            medication_taken = medicationTaken,
            conversation_history = conversationHistory
        )
        
        // Crear input JSON
        val input = ChatbotInput(
            user_id = userId,
            message = userMessage,
            timestamp = System.currentTimeMillis(),
            context = context
        )
        
        return json.encodeToString(input)
    }
    
    /**
     * Interpreta una respuesta JSON del chatbot
     * @param jsonResponse Respuesta JSON del chatbot
     * @return Objeto ChatbotOutput interpretado
     */
    fun parseChatbotResponse(jsonResponse: String): ChatbotOutput {
        return try {
            json.decodeFromString<ChatbotOutput>(jsonResponse)
        } catch (e: Exception) {
            // En caso de error, crear una respuesta de error
            ChatbotOutput(
                success = false,
                message = "Error al procesar la respuesta del chatbot",
                data = null,
                error = e.message ?: "Error desconocido"
            )
        }
    }
    
    /**
     * Simula una respuesta del chatbot en formato JSON
     * @param userMessage Mensaje del usuario
     * @param glucoseLevel Nivel de glucosa (opcional)
     * @return Respuesta simulada en formato JSON
     */
    fun generateSimulatedResponse(userMessage: String, glucoseLevel: Int? = null): String {
        val lowerMessage = userMessage.lowercase()
        
        val responseData = when {
            lowerMessage.contains("glucosa") || lowerMessage.contains("azúcar") -> {
                val glucose = glucoseLevel ?: 120
                when {
                    glucose > 200 -> ResponseData(
                        response = "Tu nivel de glucosa está alto (${glucose} mg/dL). Te recomiendo evitar carbohidratos simples y hacer ejercicio ligero. Si persiste, consulta con tu médico.",
                        intent = "glucose_high",
                        suggestions = listOf(
                            ChatbotSuggestion("diet", "Evita azúcares y carbohidratos simples", "high"),
                            ChatbotSuggestion("exercise", "Haz ejercicio ligero como caminar", "medium")
                        ),
                        followUpQuestions = listOf("¿Quieres ideas de ejercicios ligeros?", "¿Necesitas recetas bajas en carbohidratos?")
                    )
                    glucose < 70 -> ResponseData(
                        response = "Tu nivel de glucosa está bajo (${glucose} mg/dL). Consume algo dulce inmediatamente y monitorea tus niveles.",
                        intent = "glucose_low",
                        suggestions = listOf(
                            ChatbotSuggestion("emergency", "Consume 15g de carbohidratos rápidos", "high"),
                            ChatbotSuggestion("monitoring", "Mide tu glucosa en 15 minutos", "high")
                        ),
                        followUpQuestions = listOf("¿Tienes síntomas de hipoglucemia?", "¿Necesitas ayuda de emergencia?")
                    )
                    else -> ResponseData(
                        response = "Tu nivel de glucosa está en rango normal (${glucose} mg/dL). Mantén tu rutina actual y sigue monitoreando regularmente.",
                        intent = "glucose_normal",
                        suggestions = listOf(
                            ChatbotSuggestion("monitoring", "Continúa midiendo regularmente", "medium"),
                            ChatbotSuggestion("diet", "Mantén una dieta balanceada", "medium")
                        ),
                        followUpQuestions = listOf("¿Quieres consejos para mantener niveles estables?", "¿Cómo va tu rutina de ejercicio?")
                    )
                }
            }
            lowerMessage.contains("comida") || lowerMessage.contains("aliment") -> ResponseData(
                response = "Para una dieta saludable con diabetes, prioriza vegetales verdes, proteínas magras y grasas saludables. Evita carbohidratos refinados y azúcares simples.",
                intent = "diet_advice",
                suggestions = listOf(
                    ChatbotSuggestion("diet", "Incluye vegetales verdes en cada comida", "high"),
                    ChatbotSuggestion("diet", "Prefiere proteínas magras como pollo o pescado", "medium")
                ),
                followUpQuestions = listOf("¿Quieres ideas de menú específicas?", "¿Necesitas recetas bajas en carbohidratos?")
            )
            lowerMessage.contains("ejercicio") || lowerMessage.contains("actividad") -> ResponseData(
                response = "El ejercicio regular es fundamental para el control de la diabetes. Te recomiendo 30 minutos de actividad moderada al día, como caminar, nadar o andar en bicicleta.",
                intent = "exercise_advice",
                suggestions = listOf(
                    ChatbotSuggestion("exercise", "Camina 30 minutos diarios", "high"),
                    ChatbotSuggestion("exercise", "Incluye ejercicios de fuerza 2-3 veces por semana", "medium")
                ),
                followUpQuestions = listOf("¿Qué tipo de ejercicio prefieres?", "¿Necesitas un plan de ejercicios personalizado?")
            )
            lowerMessage.contains("medic") || lowerMessage.contains("tratamiento") -> ResponseData(
                response = "Es importante seguir tu tratamiento médico al pie de la letra. Toma tus medicamentos según lo prescrito y mantén un registro de tus dosis.",
                intent = "medication_advice",
                suggestions = listOf(
                    ChatbotSuggestion("medication", "Toma tus medicamentos a la misma hora cada día", "high"),
                    ChatbotSuggestion("monitoring", "Registra cualquier efecto secundario", "medium")
                ),
                followUpQuestions = listOf("¿Tienes dudas sobre tu medicación?", "¿Necesitas recordatorios para tomar tus medicamentos?")
            )
            else -> ResponseData(
                response = "Entiendo tu consulta. Para darte la mejor respuesta, ¿podrías ser más específico sobre tu pregunta relacionada con la diabetes?",
                intent = "general_inquiry",
                suggestions = listOf(
                    ChatbotSuggestion("information", "Proporciona más detalles sobre tu consulta", "medium")
                ),
                followUpQuestions = listOf("¿Es sobre glucosa, dieta, ejercicio o medicación?", "¿Hay algo específico que te preocupa?")
            )
        }
        
        val chatbotResponseData = ChatbotResponseData(
            response = responseData.response,
            intent = responseData.intent,
            confidence = 0.85,
            emergency_alert = glucoseLevel != null && glucoseLevel < 70,
            suggestions = responseData.suggestions,
            follow_up_questions = responseData.followUpQuestions
        )
        
        val output = ChatbotOutput(
            success = true,
            message = "Respuesta generada exitosamente",
            data = chatbotResponseData,
            error = null
        )
        
        return json.encodeToString(output)
    }
    
    /**
     * Extrae el mensaje de respuesta del JSON del chatbot
     * @param chatbotOutput Respuesta interpretada del chatbot
     * @return Mensaje de respuesta o mensaje de error
     */
    fun extractResponseMessage(chatbotOutput: ChatbotOutput): String {
        return if (chatbotOutput.success && chatbotOutput.data != null) {
            chatbotOutput.data.response
        } else {
            chatbotOutput.error ?: "Error al procesar la respuesta"
        }
    }
    
    /**
     * Obtiene las sugerencias de la respuesta del chatbot
     * @param chatbotOutput Respuesta parseada del chatbot
     * @return Lista de sugerencias
     */
    fun extractSuggestions(chatbotOutput: ChatbotOutput): List<ChatbotSuggestion> {
        return chatbotOutput.data?.suggestions ?: emptyList()
    }
    
    /**
     * Obtiene las preguntas de seguimiento de la respuesta del chatbot
     * @param chatbotOutput Respuesta parseada del chatbot
     * @return Lista de preguntas de seguimiento
     */
    fun extractFollowUpQuestions(chatbotOutput: ChatbotOutput): List<String> {
        return chatbotOutput.data?.follow_up_questions ?: emptyList()
    }
    
    /**
     * Verifica si hay una alerta de emergencia en la respuesta
     * @param chatbotOutput Respuesta parseada del chatbot
     * @return true si hay alerta de emergencia
     */
    fun hasEmergencyAlert(chatbotOutput: ChatbotOutput): Boolean {
        return chatbotOutput.data?.emergency_alert ?: false
    }
}
