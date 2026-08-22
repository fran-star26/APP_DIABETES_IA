# 🔑 Configurar API Key de Gemini

## ⚠️ **Problema Actual**
Tu chatbot está dando respuestas aleatorias porque la API key de Gemini no está configurada. El sistema está usando respuestas simuladas como fallback.

## 🚀 **Solución: Configurar API Key**

### Paso 1: Obtener API Key de Gemini

1. **Ve a Google AI Studio:**
   - Abre: https://makersuite.google.com/app/apikey
   - Inicia sesión con tu cuenta de Google

2. **Crear nueva API Key:**
   - Haz clic en "Create API Key"
   - Selecciona un proyecto o crea uno nuevo
   - Copia la API key generada

### Paso 2: Configurar en tu Código

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

3. **Ejemplo:**
   ```kotlin
   const val API_KEY = "AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
   ```

### Paso 3: Verificar Configuración

Después de configurar la API key, tu chatbot usará la API real de Gemini en lugar de respuestas simuladas.

## 🔍 **Cómo Verificar que Funciona**

### Antes de Configurar (Respuestas Aleatorias):
- "¿Qué puedo comer?" → Respuesta genérica simulada
- "Mi glucosa está en 180" → Respuesta básica predefinida

### Después de Configurar (API Real de Gemini):
- "¿Qué puedo comer?" → Respuesta personalizada basada en tu contexto
- "Mi glucosa está en 180" → Consejos específicos para ese nivel de glucosa

## 🛠️ **Código de Verificación**

Puedes agregar este código temporal para verificar si la API está configurada:

```kotlin
// En MainActivity.kt, en la función HomeScreen
LaunchedEffect(Unit) {
    val isConfigured = GeminiConfig.isApiKeyConfigured()
    println("API Key configurada: $isConfigured")
    
    if (isConfigured) {
        val isAvailable = ChatbotFunctions.isGeminiApiAvailable()
        println("API de Gemini disponible: $isAvailable")
    }
}
```

## 🔒 **Seguridad**

### ⚠️ **IMPORTANTE:**
- **NUNCA** subas tu API key real a un repositorio público
- Mantén tu API key privada y segura
- Considera usar variables de entorno para producción

### 🛡️ **Mejores Prácticas:**
1. Usa diferentes API keys para desarrollo y producción
2. Implementa límites de uso en tu aplicación
3. Monitorea el uso de la API regularmente

## 🚨 **Solución de Problemas**

### Error: "API key no configurada"
- Verifica que hayas reemplazado `YOUR_GEMINI_API_KEY`
- Asegúrate de que la API key sea válida

### Error: "No se puede conectar con la API"
- Verifica tu conexión a internet
- Confirma que la API key tenga permisos correctos
- Revisa los límites de uso de tu API key

### Sigue dando respuestas aleatorias
- Verifica que la API key esté configurada correctamente
- Revisa los logs de la aplicación
- Asegúrate de que la compilación sea exitosa

## 📞 **Soporte**

Si tienes problemas:
1. Verifica la configuración de la API key
2. Revisa los logs de la aplicación
3. Consulta la documentación oficial de Gemini

---

**¡Una vez configurada la API key, tu chatbot usará respuestas reales e inteligentes de Gemini! 🎉**
