# Estructura del Chatbot - Código Separado

## 📁 Organización de Archivos

El código del chatbot ha sido separado en archivos específicos para mejor organización y mantenimiento:

### 🗂️ Estructura de Directorios

```
app/src/main/java/com/proyectoing/glocosasmarai/
├── chatbot/                          # 📁 Paquete del chatbot
│   ├── ChatbotConfig.kt              # ⚙️ Configuración y constantes
│   ├── ChatbotState.kt               # 🔄 Estado y gestión de datos
│   ├── ChatbotFunctions.kt           # 🛠️ Funciones principales
│   ├── ChatbotUI.kt                  # 🎨 Interfaz de usuario principal
│   ├── ChatbotSuggestions.kt         # 💡 Componente de sugerencias
│   └── ChatbotFollowUpQuestions.kt   # ❓ Componente de preguntas
├── models/                           # 📁 Modelos de datos
│   └── ChatMessage.kt                # 📝 Modelos JSON del chatbot
├── services/                         # 📁 Servicios
│   ├── ChatbotJsonService.kt         # 🔧 Servicio de compresión JSON
│   └── ChatbotStorageService.kt      # 💾 Servicio de almacenamiento
└── examples/                         # 📁 Ejemplos
    └── ChatbotJsonExample.kt         # 📚 Ejemplos de uso
```

## 📋 Descripción de Archivos

### 1. **ChatbotConfig.kt** - Configuración
- **Propósito**: Configuración centralizada del chatbot
- **Contenido**:
  - Constantes de configuración (niveles de glucosa, límites, etc.)
  - Palabras clave para diferentes tipos de consultas
  - Respuestas por defecto
  - Enums para estados y tipos de consulta
- **Uso**: `ChatbotConfig.EMERGENCY_GLUCOSE_LOW`, `ChatbotConfig.getGlucoseStatus()`

### 2. **ChatbotState.kt** - Estado
- **Propósito**: Gestión del estado del chatbot
- **Contenido**:
  - Variables de estado (mensajes, conversaciones, UI)
  - Funciones para actualizar el estado
  - Validaciones y verificaciones
- **Uso**: `val chatbotState = ChatbotState()`

### 3. **ChatbotFunctions.kt** - Funciones Principales
- **Propósito**: Lógica principal del chatbot
- **Contenido**:
  - `getChatbotResponse()` - Genera respuestas
  - `getChatbotResponseWithMetadata()` - Respuesta completa con metadatos
  - `compressConversationToJson()` - Compresión a JSON
  - `processChatbotResponse()` - Procesamiento de respuestas
- **Uso**: `ChatbotFunctions.getChatbotResponse("mensaje", glucosa)`

### 4. **ChatbotUI.kt** - Interfaz Principal
- **Propósito**: Componente principal de la UI del chatbot
- **Contenido**:
  - `ChatbotUI()` - Componente principal
  - `ChatbotHeader()` - Header con controles
  - `ChatMessagesArea()` - Área de mensajes
  - `ChatInputArea()` - Área de entrada
  - `ChatHistorySection()` - Sección de historial
- **Uso**: `<ChatbotUI chatMessages={...} />`

### 5. **ChatbotSuggestions.kt** - Sugerencias
- **Propósito**: Componente para mostrar sugerencias
- **Contenido**:
  - `ChatbotSuggestions()` - Lista de sugerencias
  - `ChatbotSuggestionItem()` - Item individual
  - `getSuggestionIcon()` - Iconos por tipo
- **Uso**: `<ChatbotSuggestions suggestions={suggestions} />`

### 6. **ChatbotFollowUpQuestions.kt** - Preguntas
- **Propósito**: Componente para preguntas de seguimiento
- **Contenido**:
  - `ChatbotFollowUpQuestions()` - Lista de preguntas
  - `ChatbotFollowUpQuestionItem()` - Item individual
- **Uso**: `<ChatbotFollowUpQuestions questions={questions} />`

### 7. **ChatbotJsonService.kt** - Servicio JSON
- **Propósito**: Compresión y parsing de JSON
- **Contenido**:
  - `compressToInputJson()` - Convierte a JSON de entrada
  - `parseChatbotResponse()` - Parsea respuestas JSON
  - `generateSimulatedResponse()` - Genera respuestas simuladas
- **Uso**: `val service = ChatbotJsonService()`

### 8. **ChatbotStorageService.kt** - Almacenamiento
- **Propósito**: Guardar y cargar conversaciones JSON
- **Contenido**:
  - `saveInputJson()` - Guarda JSON de entrada
  - `saveOutputJson()` - Guarda JSON de salida
  - `loadInputJson()` - Carga JSON de entrada
  - `getAllInputJsonFiles()` - Lista archivos
- **Uso**: `val storage = ChatbotStorageService(context)`

## 💾 Almacenamiento de JSON

### 📍 Ubicación de Archivos
Los archivos JSON se almacenan en:
```
/storage/emulated/0/Android/data/com.proyectoing.glocosasmarai/files/chatbot_conversations/
```

### 📄 Tipos de Archivos
- **Entrada**: `input_userId_timestamp.json`
- **Salida**: `output_userId_timestamp.json`

### 🔧 Funciones de Almacenamiento
```kotlin
// Guardar conversación completa
val (inputFile, outputFile) = storage.saveCompleteConversation(
    userId = "user123",
    userMessage = "¿Qué puedo comer?",
    chatMessages = messages,
    chatbotOutput = response
)

// Cargar archivos
val inputJson = storage.loadInputJson(inputFile)
val outputJson = storage.loadOutputJson(outputFile)

// Listar archivos por usuario
val userFiles = storage.getJsonFilesByUser("user123")
```

## 🎯 Ventajas de la Separación

### ✅ **Mantenibilidad**
- Cada archivo tiene una responsabilidad específica
- Fácil localizar y modificar funcionalidades
- Código más legible y organizado

### ✅ **Reutilización**
- Componentes UI reutilizables
- Servicios independientes
- Configuración centralizada

### ✅ **Escalabilidad**
- Fácil agregar nuevas funcionalidades
- Separación clara de responsabilidades
- Testing más sencillo

### ✅ **Colaboración**
- Diferentes desarrolladores pueden trabajar en archivos específicos
- Menos conflictos de merge
- Código más modular

## 🚀 Uso en MainActivity.kt

### Antes (Todo en un archivo):
```kotlin
// 200+ líneas de código del chatbot mezclado con otras funcionalidades
```

### Después (Código separado):
```kotlin
// Solo una línea para usar el chatbot
ChatbotUI(
    chatMessages = chatMessages,
    currentConversationId = currentConversationId,
    glucoseEntries = glucoseEntries,
    onChatMessagesChange = onChatMessagesChange,
    onCurrentConversationIdChange = onCurrentConversationIdChange,
    onSavedConversationsChange = onSavedConversationsChange,
    savedConversations = savedConversations
)
```

## 📊 Estadísticas de Separación

| Archivo | Líneas | Responsabilidad |
|---------|--------|-----------------|
| `ChatbotUI.kt` | ~300 | Interfaz de usuario |
| `ChatbotFunctions.kt` | ~80 | Lógica principal |
| `ChatbotStorageService.kt` | ~200 | Almacenamiento |
| `ChatbotSuggestions.kt` | ~60 | Componente sugerencias |
| `ChatbotFollowUpQuestions.kt` | ~50 | Componente preguntas |
| `ChatbotState.kt` | ~150 | Gestión de estado |
| `ChatbotConfig.kt` | ~100 | Configuración |
| **Total** | **~940** | **Chatbot completo** |

## 🔄 Flujo de Datos

```
Usuario → ChatbotUI → ChatbotFunctions → ChatbotJsonService → JSON
                ↓
        ChatbotStorageService → Archivos JSON
                ↓
        ChatbotSuggestions/FollowUpQuestions → UI
```

## 📝 Próximos Pasos

1. **Testing**: Crear tests unitarios para cada componente
2. **Documentación**: Agregar documentación JSDoc/KDoc
3. **Optimización**: Implementar lazy loading para conversaciones
4. **Persistencia**: Agregar base de datos para conversaciones
5. **Analytics**: Implementar tracking de uso del chatbot

---

**¡El código del chatbot ahora está perfectamente organizado y es mucho más fácil de mantener y extender!** 🎉
