package com.proyectoing.glocosasmarai.debug

import com.proyectoing.glocosasmarai.services.FastApiService
import kotlinx.coroutines.runBlocking

/**
 * Test de conexión para verificar si la API FastAPI está disponible
 */
object ConnectionTest {
    
    /**
     * Ejecuta todas las pruebas de conexión
     */
    fun runConnectionTests() = runBlocking {
        println("🔍 === INICIANDO TEST DE CONEXIÓN ===")
        
        // Test 1: Verificar API FastAPI
        testFastApiConnection()
        
        // Test 2: Verificar endpoint de chat
        testChatEndpoint()
        
        println("🔍 === FIN DEL TEST DE CONEXIÓN ===")
    }
    
    /**
     * Test 1: Verificar si la API FastAPI está disponible
     */
    private suspend fun testFastApiConnection() {
        println("\n📋 Test 1: Conexión con API FastAPI")
        
        val fastApiService = FastApiService()
        
        try {
            val isAvailable = fastApiService.isApiAvailable()
            
            if (isAvailable) {
                println("✅ API FastAPI está disponible")
                
                // Obtener información de la API
                val apiInfo = fastApiService.getApiInfo()
                println("📄 Información de la API: $apiInfo")
                
            } else {
                println("❌ API FastAPI NO está disponible")
                println("💡 SOLUCIÓN:")
                println("   1. Ejecuta tu API FastAPI:")
                println("      cd Fast-API-Base")
                println("      uvicorn main:app --reload --host 0.0.0.0 --port 8000")
                println("   2. Verifica que esté en: http://localhost:8000")
                println("   3. Verifica que no haya firewall bloqueando")
            }
            
        } catch (e: Exception) {
            println("❌ Error en test de conexión: ${e.message}")
            println("💡 Verifica que tu API FastAPI esté ejecutándose")
        }
    }
    
    /**
     * Test 2: Verificar endpoint de chat
     */
    private suspend fun testChatEndpoint() {
        println("\n📋 Test 2: Endpoint de Chat")
        
        val fastApiService = FastApiService()
        
        try {
            val testMessage = "Hola, ¿puedes ayudarme con diabetes?"
            println("📤 Enviando mensaje de prueba: '$testMessage'")
            
            val result = fastApiService.sendChatMessage(testMessage)
            
            if (result.isSuccess) {
                val response = result.getOrThrow()
                println("✅ Respuesta recibida del chat:")
                println("📥 Respuesta: ${response.response}")
                println("📊 Estado: ${response.status}")
                println("⏰ Timestamp: ${response.timestamp}")
            } else {
                println("❌ Error en endpoint de chat: ${result.exceptionOrNull()?.message}")
            }
            
        } catch (e: Exception) {
            println("❌ Error en test de chat: ${e.message}")
        }
    }
    
    /**
     * Test rápido de estado
     */
    fun quickStatusCheck(): String {
        return buildString {
            appendLine("🔍 Estado de Conexión:")
            appendLine("API FastAPI: Verificando...")
            appendLine("URL: http://10.0.2.2:8000 (emulador)")
            appendLine("URL: http://TU_IP_LOCAL:8000 (dispositivo físico)")
            appendLine()
            appendLine("💡 Para ejecutar tu API:")
            appendLine("1. Clona: git clone https://github.com/RodrigoFA216/Fast-API-Base.git")
            appendLine("2. Instala: pip install -r requirements.txt")
            appendLine("3. Configura: echo 'GOOGLE_API_KEY=tu_api_key' > .env")
            appendLine("4. Ejecuta: uvicorn main:app --reload --host 0.0.0.0 --port 8000")
        }
    }
}
