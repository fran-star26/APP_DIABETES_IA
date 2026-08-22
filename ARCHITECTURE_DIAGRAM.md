# Arquitectura de Integración con Gemini

## 📊 Diagrama de Flujo

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   MainActivity  │    │  ChatbotFunctions│    │ GeminiApiService│
│                 │    │                  │    │                 │
│  - UI Components│───▶│  - getChatbotResponse()│───▶│  - sendChatbotQuery()│
│  - User Input   │    │  - getChatbotResponseWithMetadata()│    │  - buildGeminiPrompt()│
│  - Chat Messages│    │  - generateContextualResponse()│    │  - parseGeminiResponse()│
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │ ChatbotJsonService│    │   Gemini API    │
                       │                  │    │                 │
                       │  - compressToInputJson()│    │  - gemini-1.5-flash│
                       │  - parseChatbotResponse()│    │  - Real AI Responses│
                       │  - extractResponseMessage()│    │  - JSON Format     │
                       └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │   Fallback       │    │   Response      │
                       │   (Simulated)    │    │   Processing    │
                       │                  │    │                 │
                       │  - generateSimulatedResponse()│    │  - Parse JSON     │
                       │  - Error Handling│    │  - Extract Data │
                       └──────────────────┘    └─────────────────┘
```

## 🔄 Flujo de Datos Detallado

### 1. **Entrada del Usuario**
```
Usuario escribe mensaje → MainActivity → ChatbotFunctions
```

### 2. **Procesamiento de Contexto**
```
ChatbotFunctions → ChatbotJsonService.compressToInputJson()
↓
Crea JSON con:
- Mensaje del usuario
- Nivel de glucosa
- Historial de conversación
- Contexto médico
```

### 3. **Envío a Gemini**
```
ChatbotJsonService → GeminiApiService.sendChatbotQuery()
↓
GeminiApiService → API de Gemini (gemini-1.5-flash)
↓
Prompt especializado en diabetes
```

### 4. **Procesamiento de Respuesta**
```
API de Gemini → Respuesta JSON
↓
GeminiApiService.parseGeminiResponse()
↓
ChatbotOutput con:
- Respuesta principal
- Sugerencias
- Preguntas de seguimiento
- Alertas de emergencia
```

### 5. **Fallback en Caso de Error**
```
Si API falla → ChatbotJsonService.generateSimulatedResponse()
↓
Respuesta simulada como respaldo
```

## 📁 Estructura de Archivos

```
app/src/main/java/com/proyectoing/glocosasmarai/
├── config/
│   └── GeminiConfig.kt              # Configuración de API key y parámetros
├── services/
│   ├── GeminiApiService.kt          # Servicio principal de Gemini
│   └── ChatbotJsonService.kt        # Servicio de compresión JSON
├── chatbot/
│   └── ChatbotFunctions.kt          # Funciones principales del chatbot
├── models/
│   └── ChatMessage.kt               # Modelos de datos JSON
└── examples/
    └── GeminiIntegrationExample.kt  # Ejemplos de uso
```

## 🔧 Componentes Principales

### **GeminiApiService**
- **Responsabilidad**: Comunicación con la API de Gemini
- **Funciones**:
  - `sendChatbotQuery()` - Envía consulta a Gemini
  - `buildGeminiPrompt()` - Construye prompt especializado
  - `parseGeminiResponse()` - Procesa respuesta JSON
  - `isApiAvailable()` - Verifica disponibilidad

### **ChatbotFunctions**
- **Responsabilidad**: Lógica principal del chatbot
- **Funciones**:
  - `getChatbotResponse()` - Respuesta simple
  - `getChatbotResponseWithMetadata()` - Respuesta completa
  - `generateContextualResponse()` - Respuesta contextual
  - `isGeminiApiAvailable()` - Verificación de API

### **ChatbotJsonService**
- **Responsabilidad**: Manejo de JSON
- **Funciones**:
  - `compressToInputJson()` - Crea JSON de entrada
  - `parseChatbotResponse()` - Parsea respuesta
  - `generateSimulatedResponse()` - Fallback

### **GeminiConfig**
- **Responsabilidad**: Configuración centralizada
- **Propiedades**:
  - `API_KEY` - Clave de API
  - `MODEL_NAME` - Nombre del modelo
  - `TEMPERATURE` - Parámetros de generación

## 🚀 Flujo de Ejecución

### **Caso Exitoso**
1. Usuario envía mensaje
2. Se crea JSON de entrada
3. Se envía a Gemini
4. Se recibe respuesta JSON
5. Se procesa y muestra al usuario

### **Caso de Error**
1. Usuario envía mensaje
2. Se crea JSON de entrada
3. Falla la conexión con Gemini
4. Se activa fallback
5. Se genera respuesta simulada
6. Se muestra al usuario

## 📊 Tipos de Respuesta

### **Respuesta Simple**
```kotlin
val response = ChatbotFunctions.getChatbotResponse(
    userInput = "¿Qué puedo comer?",
    glucoseLevel = 180
)
// Retorna: String
```

### **Respuesta Completa**
```kotlin
val fullResponse = ChatbotFunctions.getChatbotResponseWithMetadata(
    userInput = "¿Qué puedo comer?",
    glucoseLevel = 180
)
// Retorna: ChatbotOutput
```

### **Respuesta Contextual**
```kotlin
val contextualResponse = ChatbotFunctions.generateContextualResponse(
    userInput = "No me siento bien",
    glucoseLevel = 65,
    lastMeal = "Hace 4 horas",
    medicationTaken = true
)
// Retorna: ChatbotOutput con contexto completo
```

## 🔒 Seguridad y Configuración

### **Configuración de API Key**
```kotlin
// En GeminiConfig.kt
const val API_KEY = "tu_api_key_aqui"
```

### **Verificación de Configuración**
```kotlin
if (GeminiConfig.isApiKeyConfigured()) {
    // API key configurada correctamente
} else {
    // Mostrar error de configuración
}
```

## 📈 Ventajas de la Nueva Arquitectura

### ✅ **Respuestas Inteligentes**
- IA especializada en diabetes
- Contexto completo del paciente
- Sugerencias personalizadas

### ✅ **Robustez**
- Fallback automático
- Manejo de errores
- Disponibilidad garantizada

### ✅ **Flexibilidad**
- Fácil cambio de modelo
- Configuración centralizada
- Extensible para futuras mejoras

### ✅ **Mantenibilidad**
- Código separado por responsabilidades
- Fácil testing
- Documentación completa

---

**¡La integración con Gemini está completamente implementada y lista para usar! 🎉**
