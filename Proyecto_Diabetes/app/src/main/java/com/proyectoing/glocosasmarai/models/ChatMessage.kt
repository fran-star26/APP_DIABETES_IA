package com.proyectoing.glocosasmarai.models

import kotlinx.serialization.Serializable

/**
 * Representa un mensaje individual en la conversación del chatbot
 */
@Serializable
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)

/**
 * Representa un mensaje en el historial de conversación para JSON
 */
@Serializable
data class ConversationHistoryMessage(
    val role: String, // "user" o "assistant"
    val content: String,
    val timestamp: Long
)

/**
 * Representa el contexto de la conversación para el JSON de salida
 */
@Serializable
data class ChatContext(
    val glucose_level: Int? = null,
    val last_meal: String? = null,
    val medication_taken: Boolean? = null,
    val conversation_history: List<ConversationHistoryMessage> = emptyList()
)

/**
 * Representa el JSON de salida que se envía al chatbot
 */
@Serializable
data class ChatbotInput(
    val user_id: String,
    val message: String,
    val timestamp: Long,
    val context: ChatContext
)

/**
 * Representa una sugerencia en la respuesta del chatbot
 */
@Serializable
data class ChatbotSuggestion(
    val type: String, // "diet", "exercise", "medication", etc.
    val message: String,
    val priority: String // "high", "medium", "low"
)

/**
 * Representa los datos de respuesta del chatbot
 */
@Serializable
data class ChatbotResponseData(
    val response: String,
    val intent: String, // "diet_advice", "glucose_question", "emergency", etc.
    val confidence: Double,
    val emergency_alert: Boolean,
    val suggestions: List<ChatbotSuggestion> = emptyList(),
    val follow_up_questions: List<String> = emptyList()
)

/**
 * Representa el JSON de entrada que se recibe del chatbot
 */
@Serializable
data class ChatbotOutput(
    val success: Boolean,
    val message: String,
    val data: ChatbotResponseData? = null,
    val error: String? = null
)
