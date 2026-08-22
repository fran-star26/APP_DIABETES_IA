# 🚀 Configurar tu API FastAPI con Gemini

## 📋 **Integración Completada**

He integrado tu API FastAPI del repositorio [Fast-API-Base](https://github.com/RodrigoFA216/Fast-API-Base/tree/main) con tu aplicación Android.

## 🔧 **Lo que se implementó:**

### 1. **Servicio FastApiService**
- Conecta con tu API FastAPI en `http://10.0.2.2:8000`
- Usa el endpoint `/ai/chat` para enviar mensajes
- Maneja contexto completo (glucosa, historial, medicación)

### 2. **Sistema de Fallback Inteligente**
```
1. Intenta con tu API FastAPI (prioridad alta)
2. Si falla, intenta con Gemini directo
3. Si falla todo, usa respuestas simuladas
```

### 3. **Indicador Visual**
- ✅ "API FastAPI disponible - usando tu servidor"
- ⚠️ "API FastAPI no disponible - usando Gemini directo"
- ❌ "APIs no disponibles - usando respuestas simuladas"

## 🚀 **Para que funcione:**

### **Paso 1: Ejecutar tu API FastAPI**

1. **Clona tu repositorio:**
   ```bash
   git clone https://github.com/RodrigoFA216/Fast-API-Base.git
   cd Fast-API-Base
   ```

2. **Instala dependencias:**
   ```bash
   pip install -r requirements.txt
   ```

3. **Configura tu API key de Gemini:**
   ```bash
   # Crea archivo .env
   echo "GOOGLE_API_KEY=tu_api_key_de_gemini" > .env
   ```

4. **Ejecuta la API:**
   ```bash
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```

### **Paso 2: Verificar que funciona**

1. **Abre en el navegador:**
   - `http://localhost:8000/docs` - Documentación Swagger
   - `http://localhost:8000/health` - Estado de la API

2. **Prueba el endpoint de chat:**
   ```bash
   curl -X POST "http://localhost:8000/ai/chat" \
     -F "message=¿Qué puedo comer si mi glucosa está en 180?"
   ```

### **Paso 3: Configurar la IP para dispositivo físico**

Si usas un dispositivo físico en lugar del emulador:

1. **Encuentra tu IP local:**
   ```bash
   # Windows
   ipconfig
   
   # Linux/Mac
   ifconfig
   ```

2. **Actualiza FastApiService.kt:**
   ```kotlin
   // Cambiar esta línea:
   private val baseUrl = "http://10.0.2.2:8000" // Para emulador
   
   // Por esta:
   private val baseUrl = "http://TU_IP_LOCAL:8000" // Para dispositivo físico
   ```

## 📊 **Flujo de Datos Implementado**

```
Usuario escribe mensaje
        ↓
MainActivity.kt
        ↓
ChatbotFunctions.getChatbotResponse()
        ↓
FastApiService.isApiAvailable()
        ↓
┌─────────────────────────────────────┐
│ ¿API FastAPI disponible?           │
├─────────────────────────────────────┤
│ SÍ: Enviar a tu API FastAPI        │
│ NO: Intentar Gemini directo        │
│     Si falla: Respuestas simuladas │
└─────────────────────────────────────┘
        ↓
Tu API FastAPI → Gemini → Respuesta
```

## 🔍 **Verificación del Estado**

### **En la App Android:**
- Verás un indicador en la pantalla principal
- Los logs mostrarán qué API se está usando

### **En los Logs:**
```
🚀 Intentando conectar con API FastAPI...
✅ API FastAPI disponible - usando tu API
✅ Respuesta recibida de tu API FastAPI
```

## 🛠️ **Configuración Avanzada**

### **Para Desarrollo:**
```kotlin
// En FastApiService.kt
private val baseUrl = "http://10.0.2.2:8000" // Emulador Android
```

### **Para Producción:**
```kotlin
// En FastApiService.kt
private val baseUrl = "https://tu-dominio.com/api" // Tu servidor en producción
```

### **Configurar CORS en tu API FastAPI:**
```python
# En main.py de tu API
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # En desarrollo
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

## 🚨 **Solución de Problemas**

### **Error: "API FastAPI no disponible"**
- Verifica que tu API esté ejecutándose en el puerto 8000
- Verifica la IP en `FastApiService.kt`
- Revisa los logs de tu API FastAPI

### **Error: "Connection refused"**
- Verifica que el puerto 8000 esté abierto
- Verifica que no haya firewall bloqueando
- Para dispositivo físico, verifica la IP local

### **Respuestas genéricas**
- Verifica que tu API FastAPI esté configurada con Gemini
- Revisa los logs de tu API para errores
- Verifica que la API key de Gemini esté configurada

## 🎯 **Ventajas de usar tu API FastAPI**

### ✅ **Control Total**
- Tienes control completo sobre la lógica
- Puedes agregar funcionalidades adicionales
- Manejo de contexto más sofisticado

### ✅ **Mejor Rendimiento**
- Tu servidor maneja la API key de Gemini
- No necesitas configurar API keys en Android
- Caché y optimizaciones en el servidor

### ✅ **Escalabilidad**
- Puedes agregar más funcionalidades
- Manejo de múltiples usuarios
- Logs y monitoreo centralizados

## 📞 **Soporte**

Si tienes problemas:

1. **Verifica que tu API FastAPI esté ejecutándose**
2. **Revisa los logs de tu API**
3. **Verifica la configuración de red**
4. **Revisa los logs de Android Studio**

---

**¡Tu aplicación Android ahora usa tu API FastAPI con Gemini! 🎉**

La integración está completa y lista para usar. Solo necesitas ejecutar tu API FastAPI y configurar la API key de Gemini en el servidor.
