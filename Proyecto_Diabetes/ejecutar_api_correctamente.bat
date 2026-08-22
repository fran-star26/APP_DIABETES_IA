@echo off
echo 🚀 Ejecutando API FastAPI correctamente para Android...
echo.

REM Verificar si Python está instalado
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Python no está instalado
    echo Por favor instala Python desde: https://www.python.org/downloads/
    pause
    exit /b 1
)

REM Verificar si el directorio de la API existe
if not exist "Fast-API-Base" (
    echo 📥 Clonando repositorio...
    git clone https://github.com/RodrigoFA216/Fast-API-Base.git
    if errorlevel 1 (
        echo ❌ Error al clonar el repositorio
        pause
        exit /b 1
    )
)

REM Entrar al directorio
cd Fast-API-Base

REM Verificar si requirements.txt existe
if not exist "requirements.txt" (
    echo ❌ requirements.txt no encontrado
    pause
    exit /b 1
)

REM Instalar dependencias
echo 📦 Instalando dependencias...
pip install -r requirements.txt
if errorlevel 1 (
    echo ❌ Error al instalar dependencias
    pause
    exit /b 1
)

REM Verificar si .env existe
if not exist ".env" (
    echo ⚠️ Archivo .env no encontrado
    echo Creando archivo .env...
    echo GOOGLE_API_KEY=tu_api_key_de_gemini_aqui > .env
    echo.
    echo 🔑 IMPORTANTE: Configura tu API key de Gemini en el archivo .env
    echo 1. Ve a: https://makersuite.google.com/app/apikey
    echo 2. Crea una API key
    echo 3. Reemplaza 'tu_api_key_de_gemini_aqui' en el archivo .env
    echo.
    pause
)

REM Ejecutar la API en todas las interfaces
echo 🚀 Iniciando API FastAPI en todas las interfaces...
echo.
echo ✅ La API estará disponible en:
echo    - http://localhost:8000 (navegador)
echo    - http://192.168.1.89:8000 (Android)
echo    - http://0.0.0.0:8000 (todas las interfaces)
echo.
echo ✅ Documentación: http://localhost:8000/docs
echo.
echo Presiona Ctrl+C para detener la API
echo.

REM Ejecutar con --host 0.0.0.0 para que escuche en todas las interfaces
uvicorn main:app --reload --host 0.0.0.0 --port 8000
