# Funcionalidad de Compresión JSON del Chatbot

## Descripción

Se ha implementado una funcionalidad completa para comprimir las conversaciones del chatbot a formato JSON, tanto para entrada como para salida, permitiendo una comunicación estructurada y rica en contexto.

## Características Implementadas

### 1. Modelos de Datos (`ChatMessage.kt`)

- **ChatMessage**: Representa un mensaje individual en la conversación
- **ConversationHistoryMessage**: Formato para el historial en JSON
- **ChatContext**: Contexto de la conversación (glucosa, última comida, medicación, historial)
- **ChatbotInput**: JSON de entrada al chatbot
- **ChatbotSuggestion**: Sugerencias del chatbot con tipo y prioridad
- **ChatbotResponseData**: Datos de respuesta del chatbot
- **ChatbotOutput**: JSON de salida del chatbot

### 2. Servicio de Compresión (`ChatbotJsonService.kt`)

#### Funciones Principales:

- **`compressToInputJson()`**: Convierte conversación a JSON de entrada
- **`parseChatbotResponse()`**: Parsea respuesta JSON del chatbot
- **`generateSimulatedResponse()`**: Genera respuestas simuladas en formato JSON
- **`extractResponseMessage()`**: Extrae mensaje de respuesta
- **`extractSuggestions()`**: Extrae sugerencias
- **`extractFollowUpQuestions()`**: Extrae preguntas de seguimiento
- **`hasEmergencyAlert()`**: Verifica alertas de emergencia

### 3. Integración en la UI (`MainActivity.kt`)

- Funciones actualizadas para usar el nuevo servicio JSON
- Componentes para mostrar sugerencias y preguntas de seguimiento
- Integración con el contexto de glucosa del usuario

## Formatos JSON

### JSON de Entrada (Salida de la App)

```json
{
  "user_id": "user_12345",
  "message": "¿Qué puedo comer si mi glucosa está en 180?",
  "timestamp": 1704067200000,
  "context": {
    "glucose_level": 180,
    "last_meal": "Hace 3 horas",
    "medication_taken": true,
    "conversation_history": [
      {
        "role": "user",
        "content": "Hola",
        "timestamp": 1704067100000
      },
      {
        "role": "assistant", 
        "content": "¡Hola! ¿Cómo estás hoy?",
        "timestamp": 1704067150000
      }
    ]
  }
}
```

### JSON de Salida (Entrada a la App)

```json
{
  "success": true,
  "message": "Respuesta generada exitosamente",
  "data": {
    "response": "Con 180 mg/dL, te recomiendo alimentos bajos en carbohidratos como vegetales verdes, proteínas magras y grasas saludables. Evita frutas muy dulces y carbohidratos simples.",
    "intent": "diet_advice",
    "confidence": 0.92,
    "emergency_alert": false,
    "suggestions": [
      {
        "type": "diet",
        "message": "Consume espinacas, brócoli o pollo a la plancha",
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

## Uso en la Aplicación

### 1. Generar JSON de Entrada

```kotlin
val chatbotService = ChatbotJsonService()

val inputJson = chatbotService.compressToInputJson(
    userId = "user_12345",
    userMessage = "¿Qué puedo comer?",
    chatMessages = chatMessages,
    glucoseLevel = 180,
    lastMeal = "Hace 3 horas",
    medicationTaken = true
)
```

### 2. Procesar Respuesta JSON

```kotlin
val chatbotOutput = chatbotService.parseChatbotResponse(jsonResponse)
val responseMessage = chatbotService.extractResponseMessage(chatbotOutput)
val suggestions = chatbotService.extractSuggestions(chatbotOutput)
val followUpQuestions = chatbotService.extractFollowUpQuestions(chatbotOutput)
```

### 3. Mostrar Sugerencias en UI

```kotlin
ChatbotSuggestions(
    suggestions = suggestions,
    onSuggestionClick = { suggestion ->
        // Manejar clic en sugerencia
    }
)
```

### 4. Mostrar Preguntas de Seguimiento

```kotlin
ChatbotFollowUpQuestions(
    questions = followUpQuestions,
    onQuestionClick = { question ->
        // Enviar pregunta como nuevo mensaje
    }
)
```

## Tipos de Sugerencias

- **diet**: Consejos de alimentación
- **exercise**: Recomendaciones de ejercicio
- **medication**: Información sobre medicación
- **emergency**: Alertas de emergencia
- **monitoring**: Consejos de monitoreo

## Prioridades de Sugerencias

- **high**: Crítico, requiere atención inmediata
- **medium**: Importante, debe considerarse
- **low**: Informativo, opcional

## Intents del Chatbot

- **diet_advice**: Consejos de alimentación
- **glucose_high**: Glucosa alta
- **glucose_low**: Glucosa baja
- **glucose_normal**: Glucosa normal
- **exercise_advice**: Consejos de ejercicio
- **medication_advice**: Consejos de medicación
- **emergency**: Situación de emergencia
- **general_inquiry**: Consulta general

## Alertas de Emergencia

El sistema detecta automáticamente situaciones de emergencia:
- Glucosa < 70 mg/dL (hipoglucemia)
- Glucosa > 250 mg/dL (hiperglucemia severa)
- Síntomas de emergencia mencionados

## Dependencias Agregadas

```kotlin
// En build.gradle.kts
plugins {
    kotlin("plugin.serialization") version "1.9.10"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

## Ejemplo Completo

Ver `ChatbotJsonExample.kt` para ejemplos detallados de uso.

## Beneficios

1. **Contexto Rico**: Incluye información de glucosa, medicación y historial
2. **Respuestas Estructuradas**: Sugerencias y preguntas de seguimiento organizadas
3. **Alertas de Emergencia**: Detección automática de situaciones críticas
4. **Extensibilidad**: Fácil agregar nuevos tipos de sugerencias e intents
5. **Compatibilidad**: Formato JSON estándar para integración con APIs externas

## Próximos Pasos

1. Integrar con API real de IA
2. Agregar más tipos de sugerencias
3. Implementar persistencia de conversaciones
4. Agregar análisis de sentimientos
5. Implementar recordatorios automáticos
