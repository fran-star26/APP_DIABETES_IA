# Sistema de Almacenamiento Local

## 📱 Descripción

Se ha implementado un sistema completo de almacenamiento local que guarda **TODOS** los datos del usuario de forma persistente en el dispositivo. Los datos se almacenan usando Room Database (SQLite) y se mantienen incluso si la aplicación se cierra o se reinicia el dispositivo.

## 🗄️ Base de Datos Local

### **Ubicación de la Base de Datos**
```
/data/data/com.proyectoing.glocosasmarai/databases/glucosa_smart_database
```

### **Tecnología Utilizada**
- **Room Database** (SQLite)
- **Kotlin Coroutines** para operaciones asíncronas
- **Flow** para observación reactiva de datos
- **Type Converters** para tipos complejos

## 📊 Datos Almacenados

### 1. **Registros de Glucosa** (`glucose_entries`)
- Valor de glucosa (mg/dL)
- Fecha y hora
- Antes/después de comida
- Notas adicionales
- Timestamps de creación y actualización

### 2. **Registros de Comida** (`food_entries`)
- Tipo de comida (Desayuno, Almuerzo, Cena, Snack)
- Descripción
- Fecha y hora
- Calorías (opcional)
- Carbohidratos (opcional)
- Notas adicionales

### 3. **Contactos de Emergencia** (`emergency_contacts`)
- Nombre
- Teléfono
- Relación (Médico, Familiar, Amigo, etc.)
- Contacto principal
- Timestamps de creación y actualización

### 4. **Mensajes del Chatbot** (`chat_messages`)
- Texto del mensaje
- Es usuario o bot
- Timestamp
- ID de conversación
- Timestamp de creación

### 5. **Conversaciones** (`conversations`)
- ID único
- Título de la conversación
- Timestamp de creación
- Número de mensajes
- Timestamp del último mensaje

### 6. **Perfil del Usuario** (`user_profile`)
- Nombre
- Edad
- Tipo de diabetes
- Peso y altura
- Fecha de diagnóstico
- Información del médico
- Medicación
- Notas personales

### 7. **Configuraciones de la App** (`app_settings`)
- Clave-valor
- Tipo de dato
- Descripción
- Timestamp de actualización

## 🔧 Arquitectura del Sistema

### **Estructura de Archivos**
```
app/src/main/java/com/proyectoing/glocosasmarai/
├── database/
│   ├── entities/           # Entidades de la base de datos
│   │   ├── GlucoseEntryEntity.kt
│   │   ├── FoodEntryEntity.kt
│   │   ├── EmergencyContactEntity.kt
│   │   ├── ChatMessageEntity.kt
│   │   ├── ConversationEntity.kt
│   │   ├── UserProfileEntity.kt
│   │   └── AppSettingsEntity.kt
│   ├── dao/               # Data Access Objects
│   │   ├── GlucoseEntryDao.kt
│   │   ├── FoodEntryDao.kt
│   │   ├── EmergencyContactDao.kt
│   │   ├── ChatMessageDao.kt
│   │   ├── ConversationDao.kt
│   │   ├── UserProfileDao.kt
│   │   └── AppSettingsDao.kt
│   ├── AppDatabase.kt     # Base de datos principal
│   └── Converters.kt      # Convertidores de tipos
├── repository/
│   └── DataRepository.kt  # Repositorio principal
└── services/
    └── LocalStorageService.kt  # Servicio de almacenamiento
```

## 🚀 Funcionalidades Implementadas

### **✅ Operaciones CRUD Completas**
- **Create**: Insertar nuevos registros
- **Read**: Leer datos existentes
- **Update**: Actualizar registros
- **Delete**: Eliminar registros

### **✅ Consultas Avanzadas**
- Filtros por fecha
- Búsquedas por tipo
- Estadísticas y promedios
- Consultas por rango de tiempo

### **✅ Observación Reactiva**
- Flow para actualizaciones en tiempo real
- UI se actualiza automáticamente
- Sincronización entre pantallas

### **✅ Persistencia Total**
- Datos se mantienen entre sesiones
- Resistentes a cierres inesperados
- Backup automático en el dispositivo

## 📱 Uso en la Aplicación

### **Inicialización**
```kotlin
val localStorageService = LocalStorageService(context)
localStorageService.initializeDefaultSettings()
```

### **Guardar Datos**
```kotlin
// Guardar registro de glucosa
localStorageService.saveGlucoseEntry(glucoseEntry)

// Guardar registro de comida
localStorageService.saveFoodEntry(foodEntry)

// Guardar contacto de emergencia
localStorageService.saveEmergencyContact(contact)

// Guardar conversación del chatbot
localStorageService.saveConversation(conversation)
```

### **Leer Datos**
```kotlin
// Obtener todos los registros de glucosa
val glucoseEntries = localStorageService.getAllGlucoseEntries()

// Obtener perfil del usuario
val userProfile = localStorageService.getCurrentUserProfile()

// Obtener conversaciones
val conversations = localStorageService.getAllConversations()
```

### **Configuraciones**
```kotlin
// Guardar configuración
localStorageService.setSetting("theme", "dark", "string", "Tema de la aplicación")

// Leer configuración
val theme = localStorageService.getSetting("theme")
```

## 📊 Estadísticas del Usuario

El sistema proporciona estadísticas completas:

```kotlin
val statistics = localStorageService.getUserStatistics()
// Incluye:
// - Total de registros de glucosa
// - Total de registros de comida
// - Total de conversaciones
// - Promedio de glucosa
// - Conteo de glucosa alta/baja
// - Días desde el primer registro
// - Estado del perfil del usuario
```

## 🔄 Sincronización Automática

### **Al Iniciar la App**
1. Se cargan todos los datos desde la base de datos local
2. Se inicializan configuraciones por defecto
3. Se actualiza la UI con los datos existentes

### **Al Modificar Datos**
1. Se actualiza el estado local inmediatamente
2. Se guarda en la base de datos en segundo plano
3. Se notifica a otros componentes si es necesario

### **Al Cerrar la App**
- Todos los datos se mantienen automáticamente
- No se requiere acción adicional del usuario

## 🛡️ Seguridad y Privacidad

### **Datos Locales**
- ✅ Todos los datos se almacenan localmente
- ✅ No se envían a servidores externos
- ✅ Control total del usuario sobre sus datos

### **Acceso a Datos**
- ✅ Solo la aplicación puede acceder a los datos
- ✅ Protegido por permisos de Android
- ✅ Encriptación automática de SQLite

## 📈 Rendimiento

### **Optimizaciones**
- Consultas eficientes con índices
- Operaciones asíncronas con coroutines
- Caché en memoria para datos frecuentes
- Lazy loading para grandes volúmenes

### **Escalabilidad**
- Soporta miles de registros
- Consultas optimizadas por fecha
- Limpieza automática de datos antiguos (opcional)

## 🔧 Configuraciones por Defecto

El sistema inicializa automáticamente:

```kotlin
val defaultSettings = mapOf(
    "theme" to "system",
    "language" to "es",
    "notifications_enabled" to "true",
    "glucose_reminders" to "true",
    "medication_reminders" to "true",
    "backup_enabled" to "true",
    "auto_sync" to "false",
    "data_retention_days" to "365"
)
```

## 🚨 Funciones de Emergencia

### **Limpieza de Datos**
```kotlin
// Eliminar todos los datos (solo para desarrollo)
localStorageService.clearAllData()
```

### **Exportación de Datos**
```kotlin
// Exportar todos los datos para backup
val allData = localStorageService.exportAllData()
```

## 📱 Compatibilidad

- ✅ **Android 8.0+** (API 26+)
- ✅ **Room Database** 2.6.1
- ✅ **Kotlin Coroutines** 1.7.3
- ✅ **Compose** compatible

## 🎯 Beneficios

### **Para el Usuario**
- ✅ Datos siempre disponibles
- ✅ Funciona sin conexión a internet
- ✅ Privacidad total
- ✅ Rendimiento rápido

### **Para el Desarrollo**
- ✅ Código organizado y mantenible
- ✅ Fácil testing
- ✅ Escalable y extensible
- ✅ Documentación completa

---

**¡Todos los datos del usuario ahora se guardan automáticamente de forma local y persistente!** 🎉
