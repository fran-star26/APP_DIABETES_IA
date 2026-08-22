# 🚀 Ejecutar tu API FastAPI

## 🚨 **Problema Actual**
Tu API FastAPI no está ejecutándose, por eso la aplicación Android no puede conectarse.

## 🔧 **Solución Paso a Paso**

### **Paso 1: Clonar tu repositorio**

```bash
# Ve al directorio donde quieres clonar
cd C:\Users\Daniel\Desktop

# Clona tu repositorio
git clone https://github.com/RodrigoFA216/Fast-API-Base.git

# Entra al directorio
cd Fast-API-Base
```

### **Paso 2: Instalar dependencias**

```bash
# Instalar Python si no lo tienes
# Descarga desde: https://www.python.org/downloads/

# Instalar dependencias
pip install -r requirements.txt

# O si tienes problemas:
pip install fastapi uvicorn python-multipart google-generativeai python-dotenv
```

### **Paso 3: Configurar API key de Gemini**

```bash
# Crear archivo .env
echo GOOGLE_API_KEY=tu_api_key_de_gemini_aqui > .env
```

**Para obtener tu API key de Gemini:**
1. Ve a: https://makersuite.google.com/app/apikey
2. Crea una nueva API key
3. Reemplaza `tu_api_key_de_gemini_aqui` con tu API key real

### **Paso 4: Ejecutar la API**

```bash
# Ejecutar la API
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

**Deberías ver algo como:**
```
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
INFO:     Started reloader process
INFO:     Started server process
INFO:     Waiting for application startup.
INFO:     Application startup complete.
```

### **Paso 5: Verificar que funciona**

1. **Abre tu navegador:**
   - `http://localhost:8000/docs` - Documentación Swagger
   - `http://localhost:8000/health` - Estado de la API

2. **Prueba el endpoint de chat:**
   - Ve a `http://localhost:8000/docs`
   - Busca el endpoint `POST /ai/chat`
   - Haz clic en "Try it out"
   - Escribe un mensaje como "Hola"
   - Haz clic en "Execute"

## 🔍 **Verificación en Android**

Una vez que tu API esté ejecutándose:

1. **Compila y ejecuta tu app Android**
2. **Verifica el indicador** - debería mostrar:
   - ✅ "API FastAPI disponible - usando tu servidor"
3. **Prueba el chatbot** - debería dar respuestas reales

## 🚨 **Problemas Comunes**

### **Error: "No module named 'fastapi'"**
```bash
pip install fastapi uvicorn
```

### **Error: "Port 8000 already in use"**
```bash
# Usar otro puerto
uvicorn main:app --reload --host 0.0.0.0 --port 8001

# O matar el proceso en puerto 8000
# Windows
netstat -ano | findstr :8000
taskkill /PID <PID> /F
```

### **Error: "API key not configured"**
- Verifica que el archivo `.env` existe
- Verifica que la API key esté correcta
- Reinicia la API después de cambiar el `.env`

### **Android no se conecta**
- Verifica que la API esté ejecutándose
- Verifica que no haya firewall bloqueando
- Para dispositivo físico, cambia la IP en `FastApiService.kt`

## 📱 **Configuración para Dispositivo Físico**

Si usas un dispositivo físico en lugar del emulador:

1. **Encuentra tu IP local:**
   ```bash
   ipconfig
   # Busca "Dirección IPv4" (algo como 192.168.1.100)
   ```

2. **Actualiza FastApiService.kt:**
   ```kotlin
   // Cambiar esta línea:
   private val baseUrl = "http://10.0.2.2:8000" // Para emulador
   
   // Por esta:
   private val baseUrl = "http://192.168.1.100:8000" // Tu IP local
   ```

## 🎯 **Resultado Esperado**

### **Cuando la API esté funcionando:**
- Navegador: `http://localhost:8000/docs` muestra la documentación
- Android: Indicador muestra ✅ "API FastAPI disponible"
- Chatbot: Da respuestas reales de Gemini

### **Logs de la API:**
```
INFO:     Uvicorn running on http://0.0.0.0:8000
INFO:     127.0.0.1:XXXXX - "POST /ai/chat HTTP/1.1" 200 OK
```

---

**¡Una vez que ejecutes tu API FastAPI, tu aplicación Android se conectará automáticamente! 🎉**
