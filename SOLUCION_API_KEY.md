# 🔧 Solución: Configurar API Key de Gemini

## 🚨 **Problema Actual**
Tu chatbot está dando respuestas predeterminadas porque la API key de Gemini no está configurada.

## 🔍 **Verificación del Problema**

### 1. **Estado Actual de la API Key:**
```kotlin
// En GeminiConfig.kt línea 16
const val API_KEY = "YOUR_GEMINI_API_KEY" // ← PROBLEMA: Sigue siendo el valor por defecto
```

### 2. **Indicador Visual en la App:**
Ahora verás en la pantalla principal:
- ❌ **"API no configurada - usando respuestas simuladas"** (si no está configurada)
- ✅ **"API de Gemini configurada"** (si está configurada)

## 🚀 **Solución Paso a Paso**

### **Paso 1: Obtener API Key de Gemini**

1. **Abre Google AI Studio:**
   - Ve a: https://makersuite.google.com/app/apikey
   - Inicia sesión con tu cuenta de Google

2. **Crear API Key:**
   - Haz clic en "Create API Key"
   - Selecciona un proyecto o crea uno nuevo
   - Copia la API key generada (algo como: `AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)

### **Paso 2: Configurar en tu Código**

1. **Abre el archivo:**
   ```
   app/src/main/java/com/proyectoing/glocosasmarai/config/GeminiConfig.kt
   ```

2. **Reemplaza la línea 16:**
   ```kotlin
   // ANTES:
   const val API_KEY = "YOUR_GEMINI_API_KEY"
   
   // DESPUÉS:
   const val API_KEY = "tu_api_key_real_aqui"
   ```

3. **Ejemplo real:**
   ```kotlin
   const val API_KEY = "AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
   ```

### **Paso 3: Verificar la Configuración**

1. **Compila la app** (debería compilar sin errores)
2. **Ejecuta la app**
3. **Verifica el indicador** en la pantalla principal:
   - Si ves ✅ → API configurada correctamente
   - Si ves ❌ → API no configurada

### **Paso 4: Probar el Chatbot**

1. **Escribe una pregunta** en el chatbot
2. **Revisa los logs** en Android Studio:
   - Deberías ver: `🚀 Enviando consulta a Gemini API...`
   - Y luego: `✅ Respuesta recibida de Gemini API`

## 🔍 **Debug Avanzado**

### **Si sigues viendo respuestas predeterminadas:**

1. **Verifica que la API key esté correcta:**
   ```kotlin
   // En GeminiConfig.kt
   fun isApiKeyConfigured(): Boolean {
       return API_KEY != "YOUR_GEMINI_API_KEY" && API_KEY.isNotBlank()
   }
   ```

2. **Revisa los logs de la app:**
   - Busca: `⚠️ API key de Gemini no configurada`
   - O: `🚀 Enviando consulta a Gemini API...`

3. **Prueba la conexión:**
   ```kotlin
   // Puedes agregar esto temporalmente en MainActivity
   LaunchedEffect(Unit) {
       val isAvailable = ChatbotFunctions.isGeminiApiAvailable()
       println("API disponible: $isAvailable")
   }
   ```

## 📊 **Flujo de Verificación**

```
1. Usuario escribe mensaje
        ↓
2. ChatbotFunctions.getChatbotResponse()
        ↓
3. GeminiConfig.isApiKeyConfigured()
        ↓
4. ¿API Key configurada?
   ├─ SÍ: Usar Gemini API → Respuesta real
   └─ NO: Usar respuestas simuladas → Respuesta predeterminada
```

## 🎯 **Resultado Esperado**

### **Antes de configurar:**
- Indicador: ❌ "API no configurada - usando respuestas simuladas"
- Respuestas: Genéricas y predeterminadas
- Logs: `⚠️ API key de Gemini no configurada`

### **Después de configurar:**
- Indicador: ✅ "API de Gemini configurada"
- Respuestas: Personalizadas e inteligentes
- Logs: `🚀 Enviando consulta a Gemini API...`

## 🚨 **Problemas Comunes**

### **Error: "API key no configurada"**
- **Causa:** No reemplazaste `YOUR_GEMINI_API_KEY`
- **Solución:** Configura tu API key real

### **Error: "No se puede conectar con la API"**
- **Causa:** API key inválida o sin conexión
- **Solución:** Verifica la API key y tu conexión

### **Sigue dando respuestas predeterminadas**
- **Causa:** API key no configurada correctamente
- **Solución:** Verifica que la API key sea válida

---

**¡Una vez configurada la API key, tu chatbot usará respuestas reales de Gemini! 🎉**
