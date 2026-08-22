# GlucosaSmart IA - Aplicación de Control de Diabetes

## Descripción

GlucosaSmart IA es una aplicación móvil desarrollada en Android que ayuda a los pacientes con diabetes a monitorear y controlar sus niveles de glucosa, registrar comidas, y generar reportes profesionales para compartir con sus médicos.

## Características Principales

### 📊 Monitoreo de Glucosa
- Registro de lecturas de glucosa
- Clasificación automática (Normal, Alto, Bajo, Crítico)
- Notas y observaciones
- Historial completo

### 🍽️ Registro de Comidas
- Tipos de comida (Desayuno, Almuerzo, Cena, Snack)
- Descripciones detalladas
- Horarios de registro

### 📞 Contactos de Emergencia
- Gestión de contactos médicos
- Números de emergencia
- Acceso rápido

### 📄 Generación de Reportes
- **Reportes en PDF**: Formato profesional con iText7
- **Reportes en HTML**: Compatible con Word y navegadores
- Análisis automático de datos
- Recomendaciones personalizadas
- Compartir por email, WhatsApp, etc.

## Configuración del Proyecto

### Requisitos
- Android Studio Hedgehog | 2023.1.1 o superior
- Android SDK 26+ (API 26)
- Kotlin 1.9.0+
- Gradle 8.0+

### Instalación

1. **Clonar el repositorio**
   ```bash
   git clone [URL_DEL_REPOSITORIO]
   cd Proyecto_Diabetes
   ```

2. **Abrir en Android Studio**
   - Abrir Android Studio
   - Seleccionar "Open an existing project"
   - Navegar a la carpeta del proyecto y seleccionarla

3. **Sincronizar dependencias**
   - Esperar a que Gradle sincronice automáticamente
   - O hacer clic en "Sync Now" si aparece la notificación

4. **Configurar dispositivo/emulador**
   - Conectar un dispositivo Android (API 26+)
   - O crear un emulador con API 26+

### Dependencias Principales

```kotlin
// Compose UI
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// PDF Generation
implementation("com.itextpdf:itext7-core:7.2.5")
implementation("com.itextpdf:kernel:7.2.5")
implementation("com.itextpdf:io:7.2.5")
implementation("com.itextpdf:layout:7.2.5")

// Charts
implementation("com.patrykandpatrick.vico:compose:1.13.1")
```

## Uso de la Aplicación

### Generación de Reportes

La aplicación incluye un sistema completo de generación de reportes:

#### 1. Reportes PDF
- Formato profesional
- Tablas bien estructuradas
- Colores y tipografías
- Listo para imprimir

#### 2. Reportes HTML
- Compatible con Microsoft Word
- Se puede abrir en navegadores
- Formato editable
- Sin dependencias adicionales

### Cómo Probar los Reportes

1. **Ejecutar la aplicación**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Abrir la actividad de prueba**
   - En el dispositivo/emulador, buscar "Prueba de Reportes"
   - O usar adb:
   ```bash
   adb shell am start -n com.proyectoing.glocosasmarai/.TestReportActivity
   ```

3. **Generar reporte**
   - Seleccionar formato (PDF o HTML)
   - Hacer clic en "Generar Reporte"
   - Esperar a que se complete
   - Hacer clic en "Compartir" para ver el resultado

### Estructura de los Reportes

Los reportes incluyen:

1. **Información del Paciente**
   - Nombre, edad, tipo de diabetes
   - Período del reporte
   - Fecha de generación

2. **Resumen Ejecutivo**
   - Total de lecturas de glucosa
   - Promedio de glucosa
   - Distribución de lecturas (altas, bajas, normales)
   - Total de comidas registradas

3. **Registros Detallados**
   - Tabla completa de lecturas de glucosa
   - Registros de comidas
   - Fechas, horas y valores

4. **Análisis de Advertencias**
   - Alertas de emergencia (>250 mg/dL)
   - Advertencias (<70 mg/dL)
   - Total de alertas

5. **Contactos de Emergencia**
   - Lista de contactos configurados

6. **Recomendaciones**
   - Basadas en el análisis de datos
   - Consejos personalizados
   - Recomendaciones generales

## Configuración de Permisos

La aplicación requiere los siguientes permisos:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### FileProvider

El FileProvider está configurado para compartir archivos:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

## Solución de Problemas

### Error de Compilación con iText7
Si aparecen errores de compilación relacionados con iText7:

1. **Verificar minSdk**
   - Asegurar que `minSdk = 26` en `app/build.gradle.kts`

2. **Sincronizar dependencias**
   ```bash
   ./gradlew clean build
   ```

3. **Invalidar caché**
   - En Android Studio: File → Invalidate Caches and Restart

### Error de Permisos
Si no se pueden generar reportes:

1. **Verificar permisos en tiempo de ejecución**
   - La app solicitará permisos automáticamente

2. **Verificar FileProvider**
   - Asegurar que `file_paths.xml` esté configurado

### Error al Compartir
Si no se puede compartir el reporte:

1. **Verificar apps instaladas**
   - Email, WhatsApp, Drive, etc.

2. **Verificar permisos**
   - Almacenamiento y compartición

## Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/proyectoing/glocosasmarai/
│   │   ├── services/
│   │   │   ├── ReportGeneratorService.kt      # Generación PDF
│   │   │   ├── WordReportGeneratorService.kt  # Generación HTML
│   │   │   └── UnifiedReportGeneratorService.kt # Servicio unificado
│   │   ├── ui/components/
│   │   │   └── ReportGeneratorExample.kt     # Componente UI
│   │   ├── MainActivity.kt
│   │   └── TestReportActivity.kt              # Actividad de prueba
│   ├── res/
│   │   └── xml/
│   │       └── file_paths.xml                 # Configuración FileProvider
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

## Contribuir

1. Fork el proyecto
2. Crear una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abrir un Pull Request

## Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## Contacto

Para preguntas o soporte:
- Email: [tu-email@ejemplo.com]
- GitHub Issues: [URL_DEL_REPOSITORIO]/issues

---

**GlucosaSmart IA** - Control inteligente de diabetes 📱💙 