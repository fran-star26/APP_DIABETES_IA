package com.proyectoing.glocosasmarai.services
/**
 *
 *Este servicio actúa como un traductor que maneja el lenguaje JSON del chatbot. 
 *
 */
import android.content.Context
import com.proyectoing.glocosasmarai.models.*
import com.google.gson.Gson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable

@Serializable
data class ChatbotData(
    val common_questions: List<CommonQuestion> = emptyList(),
    val glucose_responses: GlucoseResponses = GlucoseResponses(),
    val health_tips: List<String> = emptyList()
)

@Serializable
data class CommonQuestion(
    val question: String,
    val answer: String,
    val intent: String? = null,
    // Asumo que ChatbotSuggestion ya está en tu paquete de 'models'
    val suggestions: List<ChatbotSuggestion>? = emptyList(),
    val follow_up_questions: List<String>? = emptyList()
)

@Serializable
data class GlucoseResponses(
    val high_alert: String = "",
    val low_alert: String = ""
)
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
class ChatbotJsonService(private val context: Context) {
    
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

        val jsonString = loadJsonFromAssets(context, "chatbot_data.json")
        if (jsonString.isNullOrEmpty()) {
            val errorOutput = ChatbotOutput(false, "Error: No se pudo cargar chatbot_data.json", null, "No JSON file")
            return json.encodeToString(errorOutput)
        }

        // Usamos kotlinx.serialization (que ya está en tu clase)
        val chatbotData: ChatbotData
        try {
            chatbotData = json.decodeFromString<ChatbotData>(jsonString)
        } catch (e: Exception) {
            val errorOutput = ChatbotOutput(false, "Error: JSON de assets malformado", null, e.message)
            return json.encodeToString(errorOutput)
        }

        // --- INICIO DE LA LÓGICA DE SIMILITUD ---

        val SIMILARITY_THRESHOLD = 0.3 // Necesita al menos 30% de coincidencia
        var bestMatch: CommonQuestion? = null
        var highestScore = 0.0

        // 1. Itera sobre todas las preguntas comunes
        chatbotData.common_questions.forEach { commonQuestion ->
            // 2. Calcula qué tanto se parece la pregunta del usuario
            val score = calculateSimilarity(userMessage, commonQuestion.question)

            // 3. Si es la mejor coincidencia hasta ahora, la guarda
            if (score > highestScore) {
                highestScore = score
                bestMatch = commonQuestion
            }
        }

        // 4. Si la mejor coincidencia supera nuestro umbral, usa esa respuesta
        if (highestScore >= SIMILARITY_THRESHOLD && bestMatch != null) {
            val responseData = ChatbotResponseData(
                response = bestMatch!!.answer,
                intent = bestMatch!!.intent ?: "faq_match",
                confidence = highestScore,
                emergency_alert = false,
                suggestions = bestMatch!!.suggestions ?: emptyList(),
                follow_up_questions = bestMatch!!.follow_up_questions ?: emptyList()
            )
            val output = ChatbotOutput(true, "Respuesta simulada (Coincidencia)", responseData, null)
            return json.encodeToString(output) // Usamos tu serializador Kotlinx
        }

        // --- FIN DE LA LÓGICA DE SIMILITUD ---

        // --- Lógica de Fallback (la que ya tenías, pero adaptada) ---
        var responseMessage = ""
        var intent = "fallback"
        var emergency = false

        // 5. Si no hubo coincidencia, revisa el nivel de glucosa
        if (glucoseLevel != null) {
            if (glucoseLevel > 250) {
                responseMessage = chatbotData.glucose_responses.high_alert
                intent = "glucose_high_alert"
                emergency = true // Asumimos que >250 es una emergencia
            } else if (glucoseLevel < 70) {
                responseMessage = chatbotData.glucose_responses.low_alert
                intent = "glucose_low_alert"
                emergency = true
            }
        }

        // 6. Si nada de lo anterior funcionó, da un consejo de salud aleatorio
        if (responseMessage.isBlank()) {
            responseMessage = chatbotData.health_tips.random()
            intent = "health_tip"
        }

        // 7. Devuelve el JSON simulado de fallback
        val fallbackData = ChatbotResponseData(
            response = responseMessage,
            intent = intent,
            confidence = 0.5,
            emergency_alert = emergency,
            suggestions = emptyList(),
            follow_up_questions = emptyList()
        )
        val fallbackOutput = ChatbotOutput(true, "Respuesta simulada (Fallback)", fallbackData, null)
        return json.encodeToString(fallbackOutput) // Usamos tu serializador Kotlinx
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

    private fun calculateSimilarity(s1: String, s2: String): Double {
        // 1. Normalizar: minúsculas, quitar puntuación, dividir en palabras únicas (Set)
        val words1 = s1.lowercase()
            // Quita todo excepto letras, números, espacios y acentos comunes en español
            .replace(Regex("[^a-zA-Z0-9áéíóúüñ ]"), "")
            .split(" ")
            .filter { it.isNotBlank() } // Quita espacios vacíos
            .toSet()

        val words2 = s2.lowercase()
            .replace(Regex("[^a-zA-Z0-9áéíóúüñ ]"), "")
            .split(" ")
            .filter { it.isNotBlank() }
            .toSet()

        if (words1.isEmpty() && words2.isEmpty()) return 1.0 // Ambas vacías, son idénticas
        if (words1.isEmpty() || words2.isEmpty()) return 0.0 // Una vacía, 0% similitud

        // 2. Calcular Intersección (palabras que están en ambas)
        val intersection = words1.intersect(words2).size

        // 3. Calcular Unión (palabras totales en ambas, sin repetir)
        val union = words1.union(words2).size

        // 4. Devolver el puntaje (Intersección / Unión)
        return intersection.toDouble() / union.toDouble()
    }
    private fun loadJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ex: java.io.IOException) {
            ex.printStackTrace()
            null
        }
    }
}
