package com.proyectoing.glocosasmarai.chatbot

import android.content.Context
import com.proyectoing.glocosasmarai.models.*
import com.proyectoing.glocosasmarai.services.ChatbotJsonService
import com.proyectoing.glocosasmarai.services.GeminiApiService
import com.proyectoing.glocosasmarai.services.FastApiService
import com.proyectoing.glocosasmarai.config.GeminiConfig
import com.proyectoing.glocosasmarai.models.MissedMedicationSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Funciones del chatbot para generar respuestas y manejar conversaciones,
 * actuando como puente entre la interfaz y los servicios de API.
 */
object ChatbotFunctions {

    private val geminiApiService = GeminiApiService()
    private val fastApiService = FastApiService()

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Genera una respuesta del chatbot integrando el contexto del usuario y evaluando
     * la disponibilidad de FastAPI o Gemini.
     */
    suspend fun getChatbotResponse(
        context: Context,
        userInput: String,
        chatMessages: List<ChatMessage> = emptyList(),
        glucoseLevel: Int? = null,
        missedMedications: List<MissedMedicationSummary> = emptyList(),
        userId: String = "user_12345"
    ): String = withContext(Dispatchers.IO) {

        val chatbotService = ChatbotJsonService(context)

        val contextualInput = if (missedMedications.isNotEmpty()) {
            val formattedMissed = missedMedications.joinToString(", ") {
                "${it.name} (${if(it.type == "Insulina") "no aplicada" else "no tomada"}) el ${formatDate(it.timestamp)}"
            }
            """
            $userInput
            [CONTEXTO DEL SISTEMA: El paciente tiene estos medicamentos olvidados recientes: $formattedMissed. 
            Si el usuario pregunta por su progreso o salud, menciona esto.]
            """.trimIndent()
        } else {
            userInput
        }

        try {
            println("🚀 Intentando conectar con API FastAPI...")
            val isFastApiAvailable = fastApiService.isApiAvailable()

            if (isFastApiAvailable) {
                println("✅ API FastAPI disponible - usando tu API")
                val inputJson = chatbotService.compressToInputJson(userId, contextualInput, chatMessages, glucoseLevel)
                val result = fastApiService.sendContextualChat(inputJson)

                if (result.isSuccess) {
                    println("✅ Respuesta recibida de tu API FastAPI")
                    return@withContext result.getOrThrow().response
                } else {
                    println("❌ Error en tu API FastAPI: ${result.exceptionOrNull()?.message}")
                    throw result.exceptionOrNull() ?: Exception("Error desconocido de FastAPI")
                }
            }

            println("❌ API FastAPI no disponible - verificando Gemini directo")
            if (GeminiConfig.isApiKeyConfigured()) {
                println("🔄 Intentando con Gemini directo...")
                val inputJson = chatbotService.compressToInputJson(userId, contextualInput, chatMessages, glucoseLevel)
                val geminiResult = geminiApiService.sendChatbotQuery(inputJson)

                if (geminiResult.isSuccess) {
                    println("✅ Respuesta recibida de Gemini directo")
                    val chatbotOutput = geminiResult.getOrThrow()
                    return@withContext chatbotService.extractResponseMessage(chatbotOutput)
                } else {
                    println("❌ Error en Gemini directo: ${geminiResult.exceptionOrNull()?.message}")
                    throw geminiResult.exceptionOrNull() ?: Exception("Error desconocido de Gemini")
                }
            }

            println("⚠️ Ni FastAPI ni Gemini están configurados o disponibles.")
            throw Exception("No hay servicios de API disponibles")

        } catch (e: Exception) {
            println("❌ Excepción general capturada: ${e.message} - Lanzando a la UI")
            throw e
        }
    }

    /**
     * Obtiene la respuesta completa del chatbot con metadatos.
     */
    suspend fun getChatbotResponseWithMetadata(
        context: Context,
        userInput: String,
        chatMessages: List<ChatMessage> = emptyList(),
        glucoseLevel: Int? = null,
        userId: String = "user_12345"
    ): ChatbotOutput = withContext(Dispatchers.IO) {

        val chatbotService = ChatbotJsonService(context)

        try {
            val inputJson = chatbotService.compressToInputJson(
                userId = userId,
                userMessage = userInput,
                chatMessages = chatMessages,
                glucoseLevel = glucoseLevel
            )

            val result = geminiApiService.sendChatbotQuery(inputJson)

            if (result.isSuccess) {
                result.getOrThrow()
            } else {
                val fallbackResponse = chatbotService.generateSimulatedResponse(userInput, glucoseLevel)
                chatbotService.parseChatbotResponse(fallbackResponse)
            }
        } catch (e: Exception) {
            val fallbackResponse = chatbotService.generateSimulatedResponse(userInput, glucoseLevel)
            chatbotService.parseChatbotResponse(fallbackResponse)
        }
    }

    /**
     * Comprime una conversación a JSON de entrada.
     */
    fun compressConversationToJson(
        context: Context,
        userId: String,
        userMessage: String,
        chatMessages: List<ChatMessage>,
        glucoseLevel: Int? = null,
        lastMeal: String? = null,
        medicationTaken: Boolean? = null
    ): String {
        val chatbotService = ChatbotJsonService(context)
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
     * Procesa una respuesta del chatbot y extrae información útil.
     */
    fun processChatbotResponse(context: Context, chatbotOutput: ChatbotOutput): ProcessedResponse {
        val chatbotService = ChatbotJsonService(context)
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
     * Genera una respuesta contextual basada en el nivel de glucosa, comidas y medicación.
     */
    suspend fun generateContextualResponse(
        context: Context,
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

        getChatbotResponseWithMetadata(context, contextualInput, emptyList(), glucoseLevel, userId)
    }

    /**
     * Verifica si la API de Gemini está disponible.
     */
    suspend fun isGeminiApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            geminiApiService.isApiAvailable()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtiene información del modelo de Gemini.
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
 * Datos procesados de una respuesta del chatbot.
 */
data class ProcessedResponse(
    val message: String,
    val suggestions: List<ChatbotSuggestion>,
    val followUpQuestions: List<String>,
    val hasEmergencyAlert: Boolean,
    val intent: String,
    val confidence: Double
)