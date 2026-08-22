package com.proyectoing.glocosasmarai.chatbot
/**
 *
 *Se encarga de procesar el lenguaje del json a lenguaje entendible de la app 
 *
 */
import com.proyectoing.glocosasmarai.models.*
import com.proyectoing.glocosasmarai.services.ChatbotJsonService
import com.proyectoing.glocosasmarai.services.GeminiApiService
import com.proyectoing.glocosasmarai.services.FastApiService
import com.proyectoing.glocosasmarai.config.GeminiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Funciones del chatbot para generar respuestas y manejar conversaciones
 */
object ChatbotFunctions {
    
    private val chatbotService = ChatbotJsonService()
    private val geminiApiService = GeminiApiService()
    private val fastApiService = FastApiService()

    /**
     * Genera una respuesta del chatbot usando tu API FastAPI
     */
    suspend fun getChatbotResponse(
        userInput: String,
        chatMessages: List<ChatMessage> = emptyList(),
        glucoseLevel: Int? = null,
        userId: String = "user_12345"
    ): String = withContext(Dispatchers.IO) {

        try {
            // 1. Intentar con la API FastAPI
            println("🚀 Intentando conectar con API FastAPI...")

            val isFastApiAvailable = fastApiService.isApiAvailable() // Esto puede lanzar una Excepción si no hay red

            if (isFastApiAvailable) {
                println("✅ API FastAPI disponible - usando tu API")
                val inputJson = chatbotService.compressToInputJson(userId, userInput, chatMessages, glucoseLevel)
                val result = fastApiService.sendContextualChat(inputJson)

                if (result.isSuccess) {
                    println("✅ Respuesta recibida de tu API FastAPI")
                    return@withContext result.getOrThrow().response // <-- ÉXITO REAL
                } else {
                    println("❌ Error en tu API FastAPI: ${result.exceptionOrNull()?.message}")
                    // Si la API dio un error (ej. 500), lo lanzamos
                    throw result.exceptionOrNull() ?: Exception("Error desconocido de FastAPI")
                }
            }

            // 2. Fallback: Intentar con Gemini directo
            println("❌ API FastAPI no disponible - verificando Gemini directo")
            if (GeminiConfig.isApiKeyConfigured()) {
                println("🔄 Intentando con Gemini directo...")
                val inputJson = chatbotService.compressToInputJson(userId, userInput, chatMessages, glucoseLevel)
                val geminiResult = geminiApiService.sendChatbotQuery(inputJson)

                if (geminiResult.isSuccess) {
                    println("✅ Respuesta recibida de Gemini directo")
                    val chatbotOutput = geminiResult.getOrThrow()
                    return@withContext chatbotService.extractResponseMessage(chatbotOutput) // <-- ÉXITO REAL
                } else {
                    println("❌ Error en Gemini directo: ${geminiResult.exceptionOrNull()?.message}")
                    throw geminiResult.exceptionOrNull() ?: Exception("Error desconocido de Gemini")
                }
            }

            // 3. Si NADA está disponible (ambos 'if' fallaron)
            println("⚠️ Ni FastAPI ni Gemini están configurados o disponibles.")
            // Lanzamos un error para que la UI sepa que no hay conexión
            throw Exception("No hay servicios de API disponibles")

        } catch (e: Exception) {
            // 4. Si CUALQUIER cosa de arriba falla (ej. no hay internet),
            // relanzamos el error para que MainActivity lo atrape.
            println("❌ Excepción general capturada: ${e.message} - Lanzando a la UI")
            throw e // <-- ESTA ES LA LÍNEA MÁGICA
        }
    }
    
    /**
     * Obtiene la respuesta completa del chatbot con metadatos usando la API real de Gemini
     */
    suspend fun getChatbotResponseWithMetadata(
        userInput: String,
        chatMessages: List<ChatMessage> = emptyList(),
        glucoseLevel: Int? = null,
        userId: String = "user_12345"
    ): ChatbotOutput = withContext(Dispatchers.IO) {
        try {
            // Generar JSON de entrada
            val inputJson = chatbotService.compressToInputJson(
                userId = userId,
                userMessage = userInput,
                chatMessages = chatMessages,
                glucoseLevel = glucoseLevel
            )
            
            // Enviar a la API de Gemini
            val result = geminiApiService.sendChatbotQuery(inputJson)
            
            if (result.isSuccess) {
                result.getOrThrow()
            } else {
                // Si falla la API, usar respuesta simulada como fallback
                val fallbackResponse = chatbotService.generateSimulatedResponse(userInput, glucoseLevel)
                chatbotService.parseChatbotResponse(fallbackResponse)
            }
        } catch (e: Exception) {
            // En caso de error, usar respuesta simulada
            val fallbackResponse = chatbotService.generateSimulatedResponse(userInput, glucoseLevel)
            chatbotService.parseChatbotResponse(fallbackResponse)
        }
    }
    
    /**
     * Comprime una conversación a JSON de entrada
     */
    fun compressConversationToJson(
        userId: String,
        userMessage: String,
        chatMessages: List<ChatMessage>,
        glucoseLevel: Int? = null,
        lastMeal: String? = null,
        medicationTaken: Boolean? = null
    ): String {
        return chatbotService.compressToInputJson(
            userId = userId,
            userMessage = userMessage,
            chatMessages = chatMessages,
            glucoseLevel = glucoseLevel,
            lastMeal = lastMeal,
            medicationTaken = medicationTaken
        )
    }
    
    /**
     * Procesa una respuesta del chatbot y extrae información útil
     */
    fun processChatbotResponse(chatbotOutput: ChatbotOutput): ProcessedResponse {
        return ProcessedResponse(
            message = chatbotService.extractResponseMessage(chatbotOutput),
            suggestions = chatbotService.extractSuggestions(chatbotOutput),
            followUpQuestions = chatbotService.extractFollowUpQuestions(chatbotOutput),
            hasEmergencyAlert = chatbotService.hasEmergencyAlert(chatbotOutput),
            intent = chatbotOutput.data?.intent ?: "unknown",
            confidence = chatbotOutput.data?.confidence ?: 0.0
        )
    }
    
    /**
     * Genera una respuesta contextual basada en el nivel de glucosa usando la API real de Gemini
     */
    suspend fun generateContextualResponse(
        userInput: String,
        glucoseLevel: Int?,
        lastMeal: String? = null,
        medicationTaken: Boolean? = null,
        userId: String = "user_12345"
    ): ChatbotOutput = withContext(Dispatchers.IO) {
        val contextualInput = buildString {
            append(userInput)
            glucoseLevel?.let { append(" (Glucosa: ${it} mg/dL)") }
            lastMeal?.let { append(" (Última comida: $it)") }
            medicationTaken?.let { append(" (Medicación: ${if (it) "Tomada" else "No tomada"})") }
        }
        
        getChatbotResponseWithMetadata(contextualInput, emptyList(), glucoseLevel, userId)
    }
    
    /**
     * Verifica si la API de Gemini está disponible
     */
    suspend fun isGeminiApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            geminiApiService.isApiAvailable()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Obtiene información del modelo de Gemini
     */
    suspend fun getGeminiModelInfo(): String = withContext(Dispatchers.IO) {
        try {
            geminiApiService.getModelInfo()
        } catch (e: Exception) {
            "Error al obtener información del modelo: ${e.message}"
        }
    }
}

/**
 * Datos procesados de una respuesta del chatbot
 */
data class ProcessedResponse(
    val message: String,
    val suggestions: List<ChatbotSuggestion>,
    val followUpQuestions: List<String>,
    val hasEmergencyAlert: Boolean,
    val intent: String,
    val confidence: Double
)
