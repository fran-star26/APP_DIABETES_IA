# Generación de Reportes en PDF y Word

## Descripción

Se han implementado servicios para generar reportes mensuales de diabetes en formato PDF y Word (.docx) con toda la información registrada en la aplicación GlucosaSmart IA.

## Servicios Disponibles

### 1. ReportGeneratorService (PDF)
Genera reportes en formato PDF usando la biblioteca iText7.

**Características:**
- Formato profesional con tablas y estilos
- Información del paciente
- Resumen ejecutivo con estadísticas
- Registros de glucosa detallados
- Registros de comida
- Análisis de advertencias
- Contactos de emergencia
- Recomendaciones personalizadas

### 2. WordReportGeneratorService (HTML)
Genera reportes en formato HTML que se pueden abrir en Word o navegadores web.

**Características:**
- Mismas secciones que el PDF
- Formato HTML con estilos CSS
- Compatible con Microsoft Word (se puede abrir y guardar como .docx)
- Se puede abrir en cualquier navegador web
- No requiere dependencias adicionales

### 3. UnifiedReportGeneratorService
Servicio unificado que permite elegir entre PDF y Word.

## Uso Básico

```kotlin
// Crear instancia del servicio
val reportGenerator = UnifiedReportGeneratorService(context)

// Generar reporte en PDF
val pdfFile = reportGenerator.generateMonthlyReport(
    format = ReportFormat.PDF,
    glucoseEntries = glucoseEntries,
    foodEntries = foodEntries,
    emergencyContacts = emergencyContacts,
    patientName = "Juan Pérez",
    patientAge = 45,
    patientDiabetesType = "Tipo 2"
)

// Generar reporte en HTML (compatible con Word)
val htmlFile = reportGenerator.generateMonthlyReport(
    format = ReportFormat.WORD,
    glucoseEntries = glucoseEntries,
    foodEntries = foodEntries,
    emergencyContacts = emergencyContacts,
    patientName = "Juan Pérez",
    patientAge = 45,
    patientDiabetesType = "Tipo 2"
)

// Compartir reporte
reportGenerator.shareReport(pdfFile, ReportFormat.PDF)
```

## Uso en Compose

```kotlin
@Composable
fun ReportScreen() {
    val context = LocalContext.current
    val reportGenerator = remember { UnifiedReportGeneratorService(context) }
    var selectedFormat by remember { mutableStateOf(ReportFormat.PDF) }
    
    Column {
        // Selector de formato
        Row {
            RadioButton(
                selected = selectedFormat == ReportFormat.PDF,
                onClick = { selectedFormat = ReportFormat.PDF }
            )
            Text("PDF")
            
            RadioButton(
                selected = selectedFormat == ReportFormat.WORD,
                onClick = { selectedFormat = ReportFormat.WORD }
            )
            Text("Word")
        }
        
        // Botón para generar
        Button(
            onClick = {
                val file = reportGenerator.generateMonthlyReport(
                    format = selectedFormat,
                    glucoseEntries = glucoseEntries,
                    foodEntries = foodEntries,
                    emergencyContacts = emergencyContacts
                )
                reportGenerator.shareReport(file, selectedFormat)
            }
        ) {
            Text("Generar Reporte ${selectedFormat.name}")
        }
    }
}
```

## Dependencias Requeridas

### Para PDF (iText7)
```kotlin
implementation("com.itextpdf:itext7-core:7.2.5")
implementation("com.itextpdf:kernel:7.2.5")
implementation("com.itextpdf:io:7.2.5")
implementation("com.itextpdf:layout:7.2.5")
implementation("com.itextpdf:font-asian:7.2.5")
```

### Para HTML (Word compatible)
```kotlin
// No se requieren dependencias adicionales
// El servicio genera HTML que se puede abrir en Word
```

## Estructura del Reporte

### 1. Información del Paciente
- Nombre
- Edad
- Tipo de Diabetes
- Período del Reporte
- Fecha de Generación

### 2. Resumen Ejecutivo
- Total de lecturas de glucosa
- Promedio de glucosa
- Lecturas altas, bajas y normales
- Total de comidas registradas

### 3. Registros de Glucosa
- Fecha y hora
- Valor en mg/dL
- Tipo (antes/después de comida)
- Estado (Normal, Alto, Bajo, Crítico)

### 4. Registros de Comida
- Fecha y hora
- Tipo de comida
- Descripción

### 5. Análisis de Advertencias
- Alertas de emergencia (>250 mg/dL)
- Advertencias (<70 mg/dL)
- Total de alertas

### 6. Contactos de Emergencia
- Nombre
- Teléfono

### 7. Recomendaciones
- Basadas en el análisis de datos
- Recomendaciones generales de salud

## Permisos Requeridos

Asegúrate de tener los permisos necesarios en el `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

Y configurar el FileProvider:

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

## Archivo file_paths.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-files-path name="reports" path="." />
</paths>
```

## Características Adicionales

### Personalización
- Los reportes se pueden personalizar con información específica del paciente
- Se pueden agregar más secciones según necesidades
- Los estilos se pueden modificar fácilmente

### Compartir
- Los reportes se pueden compartir por email, WhatsApp, etc.
- Se guardan en el almacenamiento externo de la app
- Formato profesional listo para imprimir

### Compatibilidad
- PDF: Compatible con todos los lectores de PDF
- HTML: Compatible con Microsoft Word (se puede abrir y guardar como .docx), navegadores web, etc.

## Ejemplo de Uso Completo

```kotlin
class ReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val reportGenerator = UnifiedReportGeneratorService(this)
            
            ReportGeneratorExample(
                glucoseEntries = getGlucoseEntries(),
                foodEntries = getFoodEntries(),
                emergencyContacts = getEmergencyContacts(),
                patientName = "María García",
                patientAge = 52,
                patientDiabetesType = "Tipo 1"
            )
        }
    }
}
```

## Notas Importantes

1. **Rendimiento**: La generación de PDF es más rápida que HTML
2. **Tamaño**: Los archivos PDF suelen ser más pequeños
3. **Edición**: Los archivos HTML se pueden editar en Word, los PDF no
4. **Compatibilidad**: PDF es más universal, HTML es mejor para edición
5. **Almacenamiento**: Los archivos se guardan en el directorio de la app

## Solución de Problemas

### Error de permisos
- Verificar que los permisos estén declarados en el manifest
- Solicitar permisos en tiempo de ejecución si es necesario

### Error de FileProvider
- Verificar que el file_paths.xml esté configurado correctamente
- Asegurar que el authority coincida con el package name

### Error de dependencias
- Verificar que todas las dependencias estén en el build.gradle
- Sincronizar el proyecto después de agregar dependencias 