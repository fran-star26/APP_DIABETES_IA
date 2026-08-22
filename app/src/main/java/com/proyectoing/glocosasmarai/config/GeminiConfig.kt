package com.proyectoing.glocosasmarai.config
import com.proyectoing.glocosasmarai.BuildConfig

object GeminiConfig {

    // IMPORTANTE: No incluyas tu API Key real en el repositorio.
    // Utiliza variables de entorno (local.properties) o inyéctala durante el build.
    const val API_KEY = BuildConfig.GEMINI_API_KEY

    const val MODEL_NAME = "gemini-1.5-flash"
    const val TEMPERATURE = 0.7f
    const val TOP_K = 40
    const val TOP_P = 0.95f
    const val MAX_OUTPUT_TOKENS = 1024

    const val BASE_URL = BuildConfig.BACKEND_URL

    fun isApiKeyConfigured(): Boolean {
        return API_KEY != BuildConfig.GEMINI_API_KEY && API_KEY.isNotBlank()
    }

    fun getApiKey(): String {
        return if (isApiKeyConfigured()) {
            API_KEY
        } else {
            throw IllegalStateException("API key de Gemini no configurada. Por favor, configura tu API key localmente.")
        }
    }
}