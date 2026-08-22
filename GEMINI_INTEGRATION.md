# Integración con API de Gemini

## 📋 Descripción

Se ha implementado la integración completa con la API de Gemini para reemplazar las respuestas simuladas del chatbot con respuestas reales generadas por IA.

## 🔧 Configuración

### 1. Obtener API Key de Gemini

1. Visita [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Inicia sesión con tu cuenta de Google
3. Crea una nueva API key
4. Copia la API key generada

### 2. Configurar la API Key

Edita el archivo `app/src/main/java/com/proyectoing/glocosasmarai/config/GeminiConfig.kt`:

```kotlin
object GeminiConfig {
    // Reemplaza "YOUR_GEMINI_API_KEY" con tu API key real
    const val API_KEY = "tu_api_key_aqui"
    
    // ... resto de la configuración
}
```

### 3. Verificar Configuración

```kotlin
if (GeminiConfig.isApiKeyConfigured()) {
    println("✅ API key configurada correctamente")
} else {
    println("❌ API key no configurada")
}
```

## 🚀 Uso

### Función Básica

```kotlin
// Respuesta simple
val response = ChatbotFunctions.getChatbotResponse(
    userInput = "¿Qué puedo comer si mi glucosa está en 180?",
    glucoseLevel = 180,
    userId = "user_123"
)
```

### Respuesta con Metadatos

```kotlin
// Respuesta completa con sugerencias y preguntas de seguimiento
val fullResponse = ChatbotFunctions.getChatbotResponseWithMetadata(
    userInput = "¿Qué ejercicios puedo hacer?",
    chatMessages = chatHistory,
    glucoseLevel = 120,
    userId = "user_123"
)

// Acceder a los datos
fullResponse.data?.let { data ->
    println("Respuesta: ${data.response}")
    println("Intent: ${data.intent}")
    println("Confianza: ${data.confidence}")
    println("Alerta de emergencia: ${data.emergency_alert}")
    
    // Sugerencias
    data.suggestions.forEach { suggestion ->
        println("Sugerencia: ${suggestion.message}")
    }
    
    // Preguntas de seguimiento
    data.follow_up_questions.forEach { question ->
        println("Pregunta: $question")
    }
}
```

### Respuesta Contextual

```kotlin
// Respuesta con contexto completo
val contextualResponse = ChatbotFunctions.generateContextualResponse(
    userInput = "No me siento bien",
    glucoseLevel = 65,
    lastMeal = "Hace 4 horas",
    medicationTaken = true,
    userId = "user_123"
)
```

## 📊 Características

### ✅ **Respuestas Inteligentes**
- Respuestas generadas por IA especializada en diabetes
- Contexto completo del paciente (glucosa, medicación, historial)
- Sugerencias personalizadas y preguntas de seguimiento

### ✅ **Detección de Emergencias**
- Alerta automática para niveles críticos (<70 o >250 mg/dL)
- Sugerencias de emergencia prioritarias
- Respuestas inmediatas para situaciones críticas

### ✅ **Fallback Inteligente**
- Si la API de Gemini falla, usa respuestas simuladas
- Garantiza que el chatbot siempre responda
- Manejo robusto de errores de red

### ✅ **Configuración Flexible**
- Fácil cambio de modelo (gemini-1.5-flash, gemini-pro, etc.)
- Ajuste de parámetros de generación (temperatura, top-k, etc.)
- Configuración centralizada

## 🔄 Flujo de Datos

```
Usuario → ChatbotFunctions → GeminiApiService → API de Gemini
    ↓
JSON de entrada → Prompt especializado → Respuesta JSON → ChatbotOutput
    ↓
Fallback a respuestas simuladas si hay error
```

## 📝 Formato de JSON

### Entrada (Enviado a Gemini)
```json
{
  "user_id": "user_12345",
  "message": "¿Qué puedo comer si mi glucosa está en 180?",
  "timestamp": 1704067200000,
  "context": {
    "glucose_level": 180,
    "last_meal": "Hace 3 horas",
    "medication_taken": true,
    "conversation_history": [...]
  }
}
```

### Salida (Recibido de Gemini)
```json
{
  "success": true,
  "message": "Respuesta generada exitosamente",
  "data": {
    "response": "Con 180 mg/dL, te recomiendo...",
    "intent": "diet_advice",
    "confidence": 0.92,
    "emergency_alert": false,
    "suggestions": [
      {
        "type": "diet",
        "message": "Consume vegetales verdes",
        "priority": "high"
      }
    ],
    "follow_up_questions": [
      "¿Quieres ideas de menú específicas?",
      "¿Necesitas recetas bajas en carbohidratos?"
    ]
  },
  "error": null
}
```

## 🛠️ Dependencias

```kotlin
// En build.gradle.kts
dependencies {
    // Gemini API - Google AI
    implementation("com.google.ai.client.generativeai:generativeai:0.2.2")
    
    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

## 🔒 Seguridad

### ⚠️ **Importante**
- **NUNCA** subas tu API key real a un repositorio público
- Usa variables de entorno o archivos de configuración local
- Considera usar un servicio de proxy para mayor seguridad

### 🔐 **Mejores Prácticas**
1. Mantén tu API key en un archivo local no versionado
2. Usa diferentes API keys para desarrollo y producción
3. Implementa límites de uso en tu aplicación
4. Monitorea el uso de la API regularmente

## 🧪 Testing

### Ejecutar Ejemplos
```kotlin
// Ejecutar todos los ejemplos
GeminiIntegrationExample.runAllExamples()

// Ejemplo específico
GeminiIntegrationExample.basicExample()
```

### Verificar Conexión
```kotlin
val isAvailable = ChatbotFunctions.isGeminiApiAvailable()
println("API disponible: $isAvailable")
```

## 📈 Monitoreo

### Métricas Importantes
- Tiempo de respuesta de la API
- Tasa de éxito de las consultas
- Uso de fallback a respuestas simuladas
- Detección de emergencias

### Logs
```kotlin
// Los logs se generan automáticamente para:
// - Consultas exitosas
// - Errores de API
// - Uso de fallback
// - Alertas de emergencia
```

## 🚨 Solución de Problemas

### Error: "API key no configurada"
- Verifica que hayas reemplazado `YOUR_GEMINI_API_KEY` en `GeminiConfig.kt`
- Asegúrate de que la API key sea válida

### Error: "No se puede conectar con la API"
- Verifica tu conexión a internet
- Confirma que la API key tenga permisos correctos
- Revisa los límites de uso de tu API key

### Respuestas en inglés
- El prompt está configurado para español
- Verifica que el modelo esté respondiendo correctamente
- Considera ajustar el prompt si es necesario

## 🔄 Actualizaciones Futuras

### Próximas Mejoras
1. **Caché de respuestas** para consultas similares
2. **Análisis de sentimientos** en las consultas
3. **Respuestas multilingües** automáticas
4. **Integración con más modelos** de IA
5. **Métricas avanzadas** de uso

### Versiones de Modelo
- `gemini-1.5-flash` (actual) - Rápido y eficiente
- `gemini-1.5-pro` - Más preciso pero más lento
- `gemini-pro` - Versión anterior estable

## 📞 Soporte

Si tienes problemas con la integración:

1. Verifica la configuración de la API key
2. Revisa los logs de la aplicación
3. Ejecuta los ejemplos de prueba
4. Consulta la documentación oficial de Gemini

---

**¡La integración con Gemini está lista para usar! 🎉**

Solo necesitas configurar tu API key y comenzar a disfrutar de respuestas inteligentes generadas por IA.
