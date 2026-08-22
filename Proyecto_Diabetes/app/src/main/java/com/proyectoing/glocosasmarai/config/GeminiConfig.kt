package com.proyectoing.glocosasmarai.config

/**
 * Configuración para la API de Gemini
 * 
 * IMPORTANTE: 
 * 1. Reemplaza "YOUR_GEMINI_API_KEY" con tu API key real de Gemini
 * 2. Para obtener tu API key, visita: https://makersuite.google.com/app/apikey
 * 3. Nunca subas tu API key real a un repositorio público
 * 4. Considera usar variables de entorno o un archivo de configuración local
 */
object GeminiConfig {
    
    // TODO: Reemplazar con tu API key real de Gemini
    // Obtén tu API key en: https://makersuite.google.com/app/apikey
    const val API_KEY = "AIzaSyDUUncBaorXmw29EgpNg81F8Ujcn16CcYQ" // ← Reemplaza esto con tu API key real
    
    // Configuración del modelo
    const val MODEL_NAME = "gemini-1.5-flash"
    
    // Configuración de generación
    const val TEMPERATURE = 0.7f
    const val TOP_K = 40
    const val TOP_P = 0.95f
    const val MAX_OUTPUT_TOKENS = 1024
    
    // URLs de la API
    // Apunta a tu PC (que está corriendo el servidor en el puerto 8000)
    const val BASE_URL = "http://10.0.2.2:8000"
    
    /**
     * Verifica si la API key está configurada correctamente
     */
    fun isApiKeyConfigured(): Boolean {
        return API_KEY != "YOUR_GEMINI_API_KEY" && API_KEY.isNotBlank()
    }
    
    /**
     * Obtiene la API key (solo para uso interno)
     */
    fun getApiKey(): String {
        return if (isApiKeyConfigured()) {
            API_KEY
        } else {
            throw IllegalStateException("API key de Gemini no configurada. Por favor, configura tu API key en GeminiConfig.kt")
        }
    }
}
