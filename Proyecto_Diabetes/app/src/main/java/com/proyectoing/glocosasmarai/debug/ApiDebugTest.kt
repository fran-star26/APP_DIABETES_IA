package com.proyectoing.glocosasmarai.debug

import com.proyectoing.glocosasmarai.config.GeminiConfig
import com.proyectoing.glocosasmarai.chatbot.ChatbotFunctions
import kotlinx.coroutines.runBlocking

/**
 * Test de debug para verificar el estado de la API de Gemini
 */
object ApiDebugTest {
    
    /**
     * Ejecuta todas las pruebas de debug
     */
    fun runAllTests() = runBlocking {
        println("🔍 === INICIANDO DEBUG DE API DE GEMINI ===")
        
        // Test 1: Verificar configuración de API key
        testApiKeyConfiguration()
        
        // Test 2: Verificar disponibilidad de API
        testApiAvailability()
        
        // Test 3: Probar respuesta del chatbot
        testChatbotResponse()
        
        println("🔍 === FIN DEL DEBUG ===")
    }
    
    /**
     * Test 1: Verificar si la API key está configurada
     */
    private fun testApiKeyConfiguration() {
        println("\n📋 Test 1: Configuración de API Key")
        println("API Key actual: ${GeminiConfig.API_KEY}")
        println("¿API Key configurada?: ${GeminiConfig.isApiKeyConfigured()}")
        
        if (GeminiConfig.isApiKeyConfigured()) {
            println("✅ API Key configurada correctamente")
        } else {
            println("❌ API Key NO configurada - usando respuestas simuladas")
            println("💡 Para solucionarlo:")
            println("   1. Ve a https://makersuite.google.com/app/apikey")
            println("   2. Crea una API key")
            println("   3. Reemplaza 'YOUR_GEMINI_API_KEY' en GeminiConfig.kt")
        }
    }
    
    /**
     * Test 2: Verificar si la API está disponible
     */
    private suspend fun testApiAvailability() {
        println("\n📋 Test 2: Disponibilidad de API")
        
        if (!GeminiConfig.isApiKeyConfigured()) {
            println("⏭️ Saltando test de disponibilidad - API key no configurada")
            return
        }
        
        try {
            val isAvailable = ChatbotFunctions.isGeminiApiAvailable()
            println("¿API de Gemini disponible?: $isAvailable")
            
            if (isAvailable) {
                println("✅ API de Gemini está disponible")
            } else {
                println("❌ API de Gemini NO está disponible")
                println("💡 Verifica tu conexión a internet y la API key")
            }
        } catch (e: Exception) {
            println("❌ Error al verificar API: ${e.message}")
        }
    }
    
    /**
     * Test 3: Probar respuesta del chatbot
     */
    private suspend fun testChatbotResponse() {
        println("\n📋 Test 3: Respuesta del Chatbot")
        
        val testMessage = "¿Qué puedo comer si mi glucosa está en 180?"
        println("Mensaje de prueba: '$testMessage'")
        
        try {
            val response = ChatbotFunctions.getChatbotResponse(
                userInput = testMessage,
                glucoseLevel = 180
            )
            
            println("Respuesta recibida: '$response'")
            
            // Analizar si es una respuesta simulada o real
            val isSimulatedResponse = response.contains("Te recomiendo") && 
                                    (response.contains("vegetales verdes") || 
                                     response.contains("proteínas magras"))
            
            if (isSimulatedResponse) {
                println("⚠️ RESPUESTA SIMULADA detectada")
                println("💡 Esto significa que la API de Gemini NO se está usando")
            } else {
                println("✅ Posible respuesta real de Gemini")
            }
            
        } catch (e: Exception) {
            println("❌ Error al obtener respuesta: ${e.message}")
        }
    }
    
    /**
     * Test rápido para verificar el estado
     */
    fun quickStatusCheck(): String {
        return buildString {
            appendLine("🔍 Estado de la API de Gemini:")
            appendLine("API Key configurada: ${GeminiConfig.isApiKeyConfigured()}")
            appendLine("API Key actual: ${GeminiConfig.API_KEY.take(10)}...")
            
            if (!GeminiConfig.isApiKeyConfigured()) {
                appendLine("❌ PROBLEMA: API key no configurada")
                appendLine("💡 SOLUCIÓN: Configura tu API key en GeminiConfig.kt")
            } else {
                appendLine("✅ API key configurada correctamente")
            }
        }
    }
}
