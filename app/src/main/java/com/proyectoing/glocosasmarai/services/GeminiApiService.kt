package com.proyectoing.glocosasmarai.services

import com.proyectoing.glocosasmarai.models.*
import com.proyectoing.glocosasmarai.config.GeminiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Servicio para conectar con la API de Gemini usando la librería oficial de Google AI
 * Maneja la comunicación con el modelo Gemini 1.5 Flash
 */
class GeminiApiService {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    // Modelo de Gemini configurado
    private val generativeModel by lazy {
        if (!GeminiConfig.isApiKeyConfigured()) {
            throw IllegalStateException("API key de Gemini no configurada. Por favor, configura tu API key en GeminiConfig.kt")
        }
        
        GenerativeModel(
            modelName = GeminiConfig.MODEL_NAME,
            apiKey = GeminiConfig.getApiKey(),
            generationConfig = generationConfig {
                temperature = GeminiConfig.TEMPERATURE
                topK = GeminiConfig.TOP_K
                topP = GeminiConfig.TOP_P
                maxOutputTokens = GeminiConfig.MAX_OUTPUT_TOKENS
            }
        )
    }
    
    /**
     * Envía una consulta JSON al chatbot de Gemini y recibe una respuesta
     * @param inputJson JSON de entrada con la consulta del usuario
     * @return Respuesta del chatbot en formato JSON
     */
    suspend fun sendChatbotQuery(inputJson: String): Result<ChatbotOutput> = withContext(Dispatchers.IO) {
        try {
            // Parsear el JSON de entrada para extraer el mensaje
            val chatbotInput = json.decodeFromString<ChatbotInput>(inputJson)
            
            // Crear el prompt para Gemini
            val prompt = buildGeminiPrompt(chatbotInput)
            
            // Enviar la consulta a Gemini
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text ?: ""
            
            // Parsear la respuesta JSON del chatbot
            val chatbotOutput = parseGeminiResponse(responseText, chatbotInput)
            Result.success(chatbotOutput)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Construye el prompt para enviar a Gemini basado en el input JSON
     */
    private fun buildGeminiPrompt(input: ChatbotInput): String {
        val context = input.context
        val glucoseInfo = context.glucose_level?.let { "Nivel de glucosa actual: ${it} mg/dL" } ?: ""
        val mealInfo = context.last_meal?.let { "Última comida: $it" } ?: ""
        val medicationInfo = context.medication_taken?.let { "Medicación: ${if (it) "Tomada" else "No tomada"}" } ?: ""
        
        val conversationHistory = if (context.conversation_history.isNotEmpty()) {
            "Historial de conversación:\n" + context.conversation_history.joinToString("\n") { 
                "${it.role}: ${it.content}" 
            }
        } else ""
        
        return """
        Eres un asistente especializado en diabetes que ayuda a personas con diabetes tipo 1 y tipo 2.
        
        INFORMACIÓN DEL PACIENTE:
        - $glucoseInfo
        - $mealInfo
        - $medicationInfo
        
        $conversationHistory
        
        CONSULTA ACTUAL:
        ${input.message}
        
        INSTRUCCIONES:
        1. Responde de manera clara, empática y profesional
        2. Proporciona consejos específicos basados en el nivel de glucosa
        3. Incluye sugerencias prácticas y preguntas de seguimiento
        4. Si el nivel de glucosa es crítico (<70 o >250), marca como emergencia
        5. Responde en español
        
        FORMATO DE RESPUESTA (JSON):
        {
            "response": "Tu respuesta aquí",
            "intent": "tipo_de_consulta",
            "confidence": 0.95,
            "emergency_alert": false,
            "suggestions": [
                {
                    "type": "diet|exercise|medication|emergency|monitoring",
                    "message": "Sugerencia específica",
                    "priority": "high|medium|low"
                }
            ],
            "follow_up_questions": [
                "Pregunta de seguimiento 1",
                "Pregunta de seguimiento 2"
            ]
        }
        """.trimIndent()
    }
    
    /**
     * Parsea la respuesta de Gemini y la convierte al formato esperado
     */
    private fun parseGeminiResponse(geminiResponse: String, originalInput: ChatbotInput): ChatbotOutput {
        return try {
            // Intentar parsear la respuesta JSON del chatbot
            val chatbotResponseData = json.decodeFromString<ChatbotResponseData>(geminiResponse)
            
            ChatbotOutput(
                success = true,
                message = "Respuesta generada exitosamente",
                data = chatbotResponseData,
                error = null
            )
            
        } catch (e: Exception) {
            // Si falla el parsing JSON, crear una respuesta básica
            val basicResponse = createBasicResponse(geminiResponse, originalInput)
            ChatbotOutput(
                success = true,
                message = "Respuesta generada exitosamente",
                data = basicResponse,
                error = null
            )
        }
    }
    
    /**
     * Crea una respuesta básica cuando falla el parsing JSON
     */
    private fun createBasicResponse(responseText: String, originalInput: ChatbotInput): ChatbotResponseData {
        val glucoseLevel = originalInput.context.glucose_level
        val isEmergency = glucoseLevel != null && (glucoseLevel < 70 || glucoseLevel > 250)
        
        return ChatbotResponseData(
            response = responseText,
            intent = "general_inquiry",
            confidence = 0.8,
            emergency_alert = isEmergency,
            suggestions = if (isEmergency) {
                listOf(
                    ChatbotSuggestion("emergency", "Consulta inmediatamente con tu médico", "high")
                )
            } else {
                listOf(
                    ChatbotSuggestion("monitoring", "Continúa monitoreando tus niveles", "medium")
                )
            },
            follow_up_questions = listOf(
                "¿Necesitas más información sobre este tema?",
                "¿Hay algo más en lo que pueda ayudarte?"
            )
        )
    }
    
    /**
     * Verifica si la API está disponible
     */
    suspend fun isApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val testResponse = generativeModel.generateContent("Hola")
            testResponse.text != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Obtiene información sobre el modelo de Gemini
     */
    suspend fun getModelInfo(): String = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent("¿Cuál es tu nombre y versión?")
            response.text ?: "No se pudo obtener información del modelo"
        } catch (e: Exception) {
            "Error de conexión: ${e.message}"
        }
    }
}
