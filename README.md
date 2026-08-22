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
