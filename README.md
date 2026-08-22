# 🩸 GlucosaSmart IA 

<div align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white" />
  <img alt="FastAPI" src="https://img.shields.io/badge/FastAPI-005571?style=for-the-badge&logo=fastapi" />
  <img alt="Gemini AI" src="https://img.shields.io/badge/Gemini_AI-4285F4?style=for-the-badge&logo=google&logoColor=white" />
  <img alt="SQLite" src="https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white" />
</div>

<br>

**GlucosaSmart IA** es una aplicación móvil nativa desarrollada para Android que ayuda a los pacientes con diabetes a tomar el control de su salud. Combina un registro detallado de métricas médicas con el poder de un asistente virtual impulsado por inteligencia artificial (Google Gemini), capaz de analizar información nutricional y ofrecer recomendaciones personalizadas.

---

## ✨ Características Principales

### 🤖 Asistente Virtual Inteligente
- Chatbot integrado y alimentado por **Google Gemini 1.5 Flash**.
- Análisis nutricional instantáneo a partir de descripciones de comida (cálculo aproximado de calorías, carbohidratos y azúcares).
- Recomendaciones de emergencia (ej. protocolo de la regla del 15 para hipoglucemia).

### 📊 Monitoreo de Glucosa y Comidas
- Registro diario de lecturas de glucosa con clasificación visual de riesgo (Normal, Alto, Bajo, Crítico).
- Clasificación de comidas (Desayuno, Almuerzo, Cena, Snack).
- Historial completo almacenado de forma segura y local en el dispositivo.

### 📄 Generación de Reportes Médicos
- Exportación automática a formatos **PDF** y **HTML** utilizando *iText7*.
- Resúmenes ejecutivos con promedios, alertas generadas y estadísticas de distribución.
- Interfaz integrada para compartir el reporte rápidamente por correo o WhatsApp con el médico tratante.

### 📞 Prevención y Emergencias
- Registro de contactos médicos y números de emergencia accesibles a un toque.

---

## 🛠️ Tecnologías y Arquitectura

Este proyecto está dividido en dos capas principales:

1. **Frontend (Android/Kotlin):** Construido con **Jetpack Compose** para una UI reactiva y moderna, utilizando `Room` para persistencia de datos local, alarmas programadas con `WorkManager`, y `Coil` para manejo de imágenes.
2. **Backend (Python/FastAPI):** Una API RESTful ligera que funciona como puente entre la aplicación móvil y los modelos de lenguaje de Google, garantizando respuestas rápidas y estructuradas.

---

## 🚀 Configuración e Instalación

### Requisitos Previos
- **Android Studio** (Hedgehog 2023.1.1 o superior).
- **SDK de Android:** API 26+ (Android 8.0 Oreo).
- Cuenta de **Google Cloud / AI Studio** para obtener una API Key de Gemini.

### 1. Clonar el repositorio
```bash
git clone [https://github.com/fran-star26/APP_DIABETES_IA.git](https://github.com/fran-star26/APP_DIABETES_IA.git)
cd APP_DIABETES_IA
```
### 2. Configurar Variables de Entorno (Crucial)
Por seguridad, las claves de las APIs no están incluidas en este repositorio. Debes configurar tus propias credenciales localmente:

En Android Studio:
Abre el archivo **local.properties** (créalo en la raíz si no existe) y agrega la ruta a tu servidor backend local:
```bash
BACKEND_URL="http://TU_IP_LOCAL:8000"
```
En el Backend (FastAPI):
Crea un archivo **.env** en la raíz de tu servidor Python e inserta tu clave de Gemini:
```bash
GOOGLE_API_KEY="AIzaSyTuClaveDeGoogleAqui"
```

### 3. Ejecutar el Proyecto
- Sincroniza Gradle en Android Studio (**Sync Now**).
- Selecciona tu emulador o dispositivo físico y presiona Run (**Shift + F10**).
- Asegúrate de levantar el servidor de FastAPI en tu terminal: **uvicorn main:app --host 0.0.0.0 --port 8000**.

## 🏗️ Estructura del Proyecto (Android)
```bash
app/src/main/java/com/proyectoing/glocosasmarai/
├── chatbot/          # Lógica de integración con la IA y estructuración de JSONs
├── config/           # Lectura de variables seguras desde BuildConfig
├── database/         # Entidades de Room y DAOs
├── models/           # Clases de datos (Glucosa, Comida, Perfil, etc.)
├── services/         # Generación de PDFs (iText7), Autenticación, Drive Backup y red (OkHttp)
├── ui/               # Pantallas construidas 100% en Jetpack Compose
├── workers/          # Tareas en segundo plano (Recordatorios de medicación y comidas)
└── MainActivity.kt   # Punto de entrada de la app
```

## 🔧 Solución de Problemas Frecuentes
- Error 500 al consultar a la IA: Verifica que tu archivo **.env** exista en el backend, que la API Key sea válida y que tu proyecto de Google Cloud no tenga restricciones de cuota o facturación.
- La app no se conecta al servidor: Si pruebas en un dispositivo físico, asegúrate de que tu celular y tu computadora estén en la misma red Wi-Fi y que hayas puesto la IPv4 correcta en **local.properties**.
- Error de compilación con iText7: Asegúrate de que tu **minSdk** esté fijado en 26 dentro de **app/build.gradle.kts** y limpia el proyecto (**Build -> Clean Project**).

## 🤝 Contribuciones
Este proyecto nació como un trabajo de tesis de ingeniería. ¡Las sugerencias y mejoras son bienvenidas!

1. Haz un Fork del repositorio.
2. Crea una rama para tu función (**git checkout -b feature/NuevaFuncion**).
3. Sube tus cambios (**git commit -m 'Añadir NuevaFuncion'**).
4. Haz push a tu rama (**git push origin feature/NuevaFuncion**).
5. Abre un Pull Request.

## 📜 Licencia
Este proyecto está bajo la Licencia **MIT**. Consulta el archivo **LICENSE** para más detalles.
