package com.proyectoing.glocosasmarai.models

/**
 * Modelo para conversaciones guardadas del chatbot
 */
data class SavedConversation(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val timestamp: Long
) {
    /**
     * Obtiene el número de mensajes
     */
    fun getMessageCount(): Int {
        return messages.size
    }
    
    /**
     * Obtiene el último mensaje
     */
    fun getLastMessage(): ChatMessage? {
        return messages.maxByOrNull { it.timestamp }
    }
    
    /**
     * Obtiene el timestamp del último mensaje
     */
    fun getLastMessageTimestamp(): Long {
        return getLastMessage()?.timestamp ?: timestamp
    }
    
    /**
     * Verifica si la conversación está vacía
     */
    fun isEmpty(): Boolean {
        return messages.isEmpty()
    }
    
    /**
     * Obtiene un resumen de la conversación
     */
    fun getSummary(): String {
        return if (messages.isEmpty()) {
            "Conversación vacía"
        } else {
            val userMessages = messages.count { it.isUser }
            val botMessages = messages.count { !it.isUser }
            "$userMessages mensajes del usuario, $botMessages respuestas del bot"
        }
    }
}
