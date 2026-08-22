package com.proyectoing.glocosasmarai.services

import com.proyectoing.glocosasmarai.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import java.util.concurrent.TimeUnit
import com.proyectoing.glocosasmarai.BuildConfig

/**
 * Servicio para conectar con la API FastAPI de Gemini
 */
class FastApiService {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    // Configuración de la API FastAPI
    private val baseUrl = BuildConfig.BACKEND_URL // Para emulador Android usar // Tu IP local
    // Para emulador Android usar: "http://10.0.2.2:8000"
    // Para dispositivo físico usar: "http://TU_IP_LOCAL:8000"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Envía una consulta al endpoint de chat de la API FastAPI
     * @param message Mensaje del usuario
     * @param context Contexto adicional (glucosa, historial, etc.)
     * @return Respuesta del chatbot
     */
    suspend fun sendChatMessage(
        message: String,
        context: ChatContext? = null
    ): Result<FastApiResponse> = withContext(Dispatchers.IO) {
        try {
            // Crear el cuerpo de la petición
            val requestBody = FormBody.Builder()
                .add("message", message)
                .build()
            
            // Crear la petición
            val request = Request.Builder()
                .url("$baseUrl/ai/chat")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            
            // Ejecutar la petición
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val fastApiResponse = json.decodeFromString<FastApiResponse>(responseBody)
                Result.success(fastApiResponse)
            } else {
                val errorMessage = "Error en la API: ${response.code} - ${response.message}"
                Result.failure(Exception(errorMessage))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Envía una consulta con contexto completo usando el formato JSON
     * @param inputJson JSON con el contexto completo del usuario
     * @return Respuesta del chatbot
     */
    suspend fun sendContextualChat(inputJson: String): Result<FastApiResponse> = withContext(Dispatchers.IO) {
        try {
            // Parsear el JSON de entrada para extraer el mensaje
            val chatbotInput = json.decodeFromString<ChatbotInput>(inputJson)
            
            // Crear el cuerpo de la petición con contexto
            val contextualMessage = buildContextualMessage(chatbotInput)
            
            val requestBody = FormBody.Builder()
                .add("message", contextualMessage)
                .build()
            
            val request = Request.Builder()
                .url("$baseUrl/ai/chat")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val fastApiResponse = json.decodeFromString<FastApiResponse>(responseBody)
                Result.success(fastApiResponse)
            } else {
                val errorMessage = "Error en la API: ${response.code} - ${response.message}"
                Result.failure(Exception(errorMessage))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Construye un mensaje contextual basado en el input JSON
     */
    private fun buildContextualMessage(input: ChatbotInput): String {

        // --- 1. DEFINE LOS DOS TIPOS DE PROMPT ---

        // PROMPT 1: Para la PRIMERA interacción (cuando el historial está vacío)
        // Incluye la regla amable de "Manejo de Saludos".
        val promptForNewConversation = """
    Eres "GlucosaSmart IA", un asistente virtual experto y amigable.
    Tu único propósito es ayudar a los usuarios con el manejo de la diabetes.

    Aquí están tus reglas de conversación:

    1.  **Manejo de Saludos:** Si el usuario solo te saluda (ej: "hola", "buenos días", "¿cómo estás?"), responde al saludo amablemente y preséntate. Por ejemplo: "¡Hola! Soy GlucosaSmart IA, tu asistente de diabetes tipo 2. ¿Cómo puedo ayudarte hoy con tu glucosa o tu dieta?"

    2.  **Temas Permitidos:** Solo puedes responder preguntas sobre:
        - Diabetes (Tipo 2)
        - Diabetes (Tipo 1 y gestional en caso que se de comparacion a la tipo 2)
        - Niveles de glucosa
        - Insulina y medicamentos para la diabetes
        - Dieta y nutrición para diabéticos
        - Ejercicio y estilo de vida
        - Síntomas y complicaciones relacionadas con la diabetes

    3.  **Temas Prohibidos (Rechazo):** Si el usuario te pregunta sobre CUALQUIER OTRO TEMA que no esté en la lista de "Temas Permitidos" (como política, deportes, historia, etc.), DEBES rehusarte amablemente.
        Responde ÚNICAMENTE con: "Lo siento, solo estoy programado para ayudarte con temas relacionados con el manejo de la diabetes tipo 2. ¿Tienes alguna pregunta sobre tu glucosa o dieta?"
    """

        // PROMPT 2: Para el RESTO de la conversación (cuando el historial NO está vacío)
        // Es más directo y omite la regla de "Manejo de Saludos".
        val promptForOngoingConversation = """
    Eres "GlucosaSmart IA", un asistente virtual experto en diabetes.
    Tu único propósito es ayudar a los usuarios con el manejo de la diabetes tipo 2.

    Aquí están tus reglas de conversación:ho

    1.  **Temas Permitidos:** Solo puedes responder preguntas sobre:
        - Diabetes (Tipo 2)
        - Diabetes (Tipo 1 y gestional en caso que sea de comparacion a la tipo 2)
        - Niveles de glucosa
        - Insulina y medicamentos para la diabetes
        - Dieta y nutrición para diabéticos
        - Ejercicio y estilo de vida
        - Síntomas y complicaciones relacionadas con la diabetes

    2.  **Temas Prohibidos (Rechazo):** Si el usuario te pregunta sobre CUALQUIER OTRO TEMA que no esté en la lista de "Temas Permitidos" (como política, deportes, historia, etc.), DEBES rehusarte amablemente.
        Responde ÚNICAMENTE con: "Lo siento, solo estoy programado para ayudarte con temas relacionados con el manejo de la diabetes tipo 2. ¿Tienes alguna pregunta sobre tu glucosa o dieta?"
    """

        // --- 2. OBTIENE EL CONTEXTO (Tu código actual) ---
        val context = input.context
        val glucoseInfo = context.glucose_level?.let { "Nivel de glucosa actual: ${it} mg/dL" } ?: ""
        val mealInfo = context.last_meal?.let { "Última comida: $it" } ?: ""
        val medicationInfo = context.medication_taken?.let { "Medicación: ${if (it) "Tomada" else "No tomada"}" } ?: ""

        // Guardamos la lista del historial para comprobar si está vacía
        val conversationHistoryList = context.conversation_history

        val conversationHistory = if (conversationHistoryList.isNotEmpty()) {
            "Historial de conversación:\n" + conversationHistoryList.joinToString("\n") {
                "${it.role}: ${it.content}"
            }
        } else ""

        // --- 3. CONSTRUYE EL PROMPT FINAL (¡AQUÍ ESTÁ LA LÓGICA!) ---

        // Decide qué prompt usar basado en el historial
        val systemPrompt = if (conversationHistoryList.isEmpty()) {
            // Si el historial está vacío, es el primer mensaje. Usa el saludo.
            promptForNewConversation
        } else {
            // Si ya hay historial, la conversación empezó. Usa el prompt directo.
            promptForOngoingConversation
        }

        return buildString {
            appendLine(systemPrompt) // Usa el prompt seleccionado
            appendLine()

            if (glucoseInfo.isNotEmpty()) appendLine(glucoseInfo)
            if (mealInfo.isNotEmpty()) appendLine(mealInfo)
            if (medicationInfo.isNotEmpty()) appendLine(medicationInfo)

            if (conversationHistory.isNotEmpty()) {
                appendLine()
                appendLine(conversationHistory)
            }

            appendLine()
            appendLine("Consulta del usuario: ${input.message}")
        }
    }
    
    /**
     * Verifica si la API FastAPI está disponible
     */
    suspend fun isApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🔍 Verificando conexión con API FastAPI en: $baseUrl")
            
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val isSuccessful = response.isSuccessful
            
            if (isSuccessful) {
                println("✅ API FastAPI disponible en: $baseUrl")
            } else {
                println("❌ API FastAPI no responde. Código: ${response.code}")
            }
            
            response.close()
            isSuccessful
        } catch (e: Exception) {
            println("❌ Error conectando con API FastAPI: ${e.message}")
            println("💡 Verifica que tu API FastAPI esté ejecutándose en: $baseUrl")
            false
        }
    }
    
    /**
     * Obtiene información de la API
     */
    suspend fun getApiInfo(): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string() ?: "No se pudo obtener información"
            } else {
                "Error al obtener información: ${response.code}"
            }
        } catch (e: Exception) {
            "Error de conexión: ${e.message}"
        }
    }
}

/**
 * Respuesta de la API FastAPI
 */
@kotlinx.serialization.Serializable
data class FastApiResponse(
    val response: String,
    val status: String? = null,
    val timestamp: String? = null
)
