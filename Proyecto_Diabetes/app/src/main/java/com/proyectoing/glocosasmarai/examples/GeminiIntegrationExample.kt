package com.proyectoing.glocosasmarai.examples

import com.proyectoing.glocosasmarai.chatbot.ChatbotFunctions
import com.proyectoing.glocosasmarai.models.ChatMessage
import com.proyectoing.glocosasmarai.config.GeminiConfig
import kotlinx.coroutines.runBlocking

/**
 * Ejemplo de uso de la integración con la API de Gemini
 * 
 * Este archivo muestra cómo usar el chatbot con la API real de Gemini
 * en lugar de las respuestas simuladas.
 */
object GeminiIntegrationExample {
    
    /**
     * Ejemplo básico de uso del chatbot con Gemini
     */
    fun basicExample() = runBlocking {
        println("=== Ejemplo Básico de Chatbot con Gemini ===")
        
        // Verificar si la API key está configurada
        if (!GeminiConfig.isApiKeyConfigured()) {
            println("❌ API key de Gemini no configurada")
            println("Por favor, configura tu API key en GeminiConfig.kt")
            return@runBlocking
        }
        
        // Verificar si la API está disponible
        val isAvailable = ChatbotFunctions.isGeminiApiAvailable()
        println("API de Gemini disponible: $isAvailable")
        
        if (!isAvailable) {
            println("❌ No se puede conectar con la API de Gemini")
            return@runBlocking
        }
        
        // Obtener información del modelo
        val modelInfo = ChatbotFunctions.getGeminiModelInfo()
        println("Información del modelo: $modelInfo")
        
        // Ejemplo de consulta simple
        val userMessage = "¿Qué puedo comer si mi glucosa está en 180?"
        println("\nUsuario: $userMessage")
        
        val response = ChatbotFunctions.getChatbotResponse(
            userInput = userMessage,
            glucoseLevel = 180,
            userId = "ejemplo_user"
        )
        
        println("Chatbot: $response")
    }
    
    /**
     * Ejemplo con conversación completa y contexto
     */
    fun contextualExample() = runBlocking {
        println("\n=== Ejemplo Contextual con Gemini ===")
        
        if (!GeminiConfig.isApiKeyConfigured()) {
            println("❌ API key de Gemini no configurada")
            return@runBlocking
        }
        
        // Crear historial de conversación
        val chatHistory = listOf(
            ChatMessage(
                text = "Hola, me siento un poco mareado",
                isUser = true,
                timestamp = System.currentTimeMillis() - 300000
            ),
            ChatMessage(
                text = "Hola, entiendo que te sientes mareado. ¿Podrías medir tu nivel de glucosa?",
                isUser = false,
                timestamp = System.currentTimeMillis() - 240000
            )
        )
        
        // Consulta con contexto
        val userMessage = "Mi glucosa está en 65 mg/dL"
        println("Usuario: $userMessage")
        
        val response = ChatbotFunctions.getChatbotResponse(
            userInput = userMessage,
            chatMessages = chatHistory,
            glucoseLevel = 65,
            userId = "ejemplo_user"
        )
        
        println("Chatbot: $response")
    }
    
    /**
     * Ejemplo con respuesta completa y metadatos
     */
    fun metadataExample() = runBlocking {
        println("\n=== Ejemplo con Metadatos de Gemini ===")
        
        if (!GeminiConfig.isApiKeyConfigured()) {
            println("❌ API key de Gemini no configurada")
            return@runBlocking
        }
        
        val userMessage = "¿Qué ejercicios puedo hacer con diabetes?"
        println("Usuario: $userMessage")
        
        val fullResponse = ChatbotFunctions.getChatbotResponseWithMetadata(
            userInput = userMessage,
            glucoseLevel = 120,
            userId = "ejemplo_user"
        )
        
        println("Respuesta completa:")
        println("- Éxito: ${fullResponse.success}")
        println("- Mensaje: ${fullResponse.message}")
        println("- Error: ${fullResponse.error}")
        
        fullResponse.data?.let { data ->
            println("- Respuesta: ${data.response}")
            println("- Intent: ${data.intent}")
            println("- Confianza: ${data.confidence}")
            println("- Alerta de emergencia: ${data.emergency_alert}")
            
            println("- Sugerencias:")
            data.suggestions.forEach { suggestion ->
                println("  • [${suggestion.priority}] ${suggestion.type}: ${suggestion.message}")
            }
            
            println("- Preguntas de seguimiento:")
            data.follow_up_questions.forEach { question ->
                println("  • $question")
            }
        }
    }
    
    /**
     * Ejemplo de respuesta contextual avanzada
     */
    fun advancedContextualExample() = runBlocking {
        println("\n=== Ejemplo Contextual Avanzado con Gemini ===")
        
        if (!GeminiConfig.isApiKeyConfigured()) {
            println("❌ API key de Gemini no configurada")
            return@runBlocking
        }
        
        val userMessage = "No me siento bien"
        println("Usuario: $userMessage")
        
        val contextualResponse = ChatbotFunctions.generateContextualResponse(
            userInput = userMessage,
            glucoseLevel = 45, // Nivel crítico
            lastMeal = "Hace 6 horas",
            medicationTaken = false,
            userId = "ejemplo_user"
        )
        
        println("Respuesta contextual:")
        contextualResponse.data?.let { data ->
            println("- Respuesta: ${data.response}")
            println("- Alerta de emergencia: ${data.emergency_alert}")
            println("- Sugerencias de emergencia:")
            data.suggestions.forEach { suggestion ->
                println("  • [${suggestion.priority}] ${suggestion.message}")
            }
        }
    }
    
    /**
     * Ejecuta todos los ejemplos
     */
    fun runAllExamples() = runBlocking {
        println("🚀 Iniciando ejemplos de integración con Gemini...")
        println("=" * 50)
        
        try {
            basicExample()
            contextualExample()
            metadataExample()
            advancedContextualExample()
            
            println("\n" + "=" * 50)
            println("✅ Todos los ejemplos completados exitosamente")
            
        } catch (e: Exception) {
            println("\n❌ Error durante la ejecución de ejemplos: ${e.message}")
            println("Asegúrate de que tu API key de Gemini esté configurada correctamente")
        }
    }
}

/**
 * Función de extensión para repetir strings
 */
private operator fun String.times(n: Int): String = this.repeat(n)
