package com.proyectoing.glocosasmarai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyectoing.glocosasmarai.services.ReportFormat
import com.proyectoing.glocosasmarai.services.UnifiedReportGeneratorService
import com.proyectoing.glocosasmarai.models.Medication
import java.io.File
import java.util.*
import java.util.Calendar

class TestReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestReportScreen()
        }
    }
}

@Composable
fun TestReportScreen() {
    val context = LocalContext.current
    val reportGenerator = remember { UnifiedReportGeneratorService(context) }
    var selectedFormat by remember { mutableStateOf(ReportFormat.PDF) }
    var isGenerating by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    
    // Datos de prueba
    val testGlucoseEntries = listOf(
        com.proyectoing.glocosasmarai.models.GlucoseEntry(
            id = 1,
            value = 120,
            timestamp = System.currentTimeMillis() - 86400000,
            isBeforeMeal = true,
            notes = "Antes del desayuno"
        ),
        com.proyectoing.glocosasmarai.models.GlucoseEntry(
            id = 2,
            value = 180,
            timestamp = System.currentTimeMillis() - 43200000,
            isBeforeMeal = false,
            notes = "Después del almuerzo"
        ),
        com.proyectoing.glocosasmarai.models.GlucoseEntry(
            id = 3,
            value = 95,
            timestamp = System.currentTimeMillis(),
            isBeforeMeal = true,
            notes = "Antes de la cena"
        )
    )
    
    val testFoodEntries = listOf(
        com.proyectoing.glocosasmarai.models.FoodEntry(
            id = 1,
            type = "Desayuno",
            description = "Avena con frutas y leche",
            timestamp = System.currentTimeMillis() - 86400000,
            calories = null,
            carbohydrates = null,
            notes = null
        ),
        com.proyectoing.glocosasmarai.models.FoodEntry(
            id = 2,
            type = "Almuerzo",
            description = "Pollo con arroz y ensalada",
            timestamp = System.currentTimeMillis() - 43200000,
            calories = null,
            carbohydrates = null,
            notes = null
        )
    )
    
    val testEmergencyContacts = listOf(
        com.proyectoing.glocosasmarai.models.EmergencyContact(
            id = 1,
            name = "Dr. García",
            phone = "+34 123 456 789"
        ),
        com.proyectoing.glocosasmarai.models.EmergencyContact(
            id = 2,
            name = "María (Esposa)",
            phone = "+34 987 654 321"
        )
    )

    val testMedications = listOf(
        Medication(
            id = 1,
            name = "Metformina",
            dose = "500mg",
            hour = 8,
            minute = 0,
            endDate = System.currentTimeMillis() + 86400000L * 30, // 30 días desde ahora
            type = "Medicamento",
            trackingState = "PENDING"
        ),
        Medication(
            id = 2,
            name = "Insulina",
            dose = "10 unidades",
            hour = 21,
            minute = 0,
            endDate = System.currentTimeMillis() + 86400000L * 60, // 60 días desde ahora
            type = "Insulina",
            trackingState = "PENDING"
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Prueba de Generación de Reportes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Selector de formato
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedFormat == ReportFormat.PDF,
                    onClick = { selectedFormat = ReportFormat.PDF }
                )
                Text(
                    text = "PDF",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedFormat == ReportFormat.WORD,
                    onClick = { selectedFormat = ReportFormat.WORD }
                )
                Text(
                    text = "HTML",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botón para generar reporte
        Button(
            onClick = {
                isGenerating = true
                statusMessage = "Generando reporte..."
                
                try {
                    val cal = Calendar.getInstance()
                    val currentYear = cal.get(Calendar.YEAR)
                    val currentMonth = cal.get(Calendar.MONTH)
                    val file = reportGenerator.generateMonthlyReport(
                        format = selectedFormat,
                        glucoseEntries = testGlucoseEntries,
                        foodEntries = testFoodEntries,
                        medications = testMedications,
                        emergencyContacts = testEmergencyContacts,
                        patientName = "Juan Pérez",
                        patientAge = 45,
                        patientDiabetesType = "Tipo 2",
                        year = currentYear, // <-- AÑADE ESTA LÍNEA
                        month = currentMonth
                    )
                    
                    generatedFile = file
                    isGenerating = false
                    showSuccessDialog = true
                    statusMessage = "Reporte generado exitosamente: ${file.name}"
                    
                } catch (e: Exception) {
                    isGenerating = false
                    statusMessage = "Error: ${e.message}"
                    e.printStackTrace()
                }
            },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generando...")
            } else {
                Text("Generar Reporte ${selectedFormat.name}")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mensaje de estado
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                modifier = Modifier.padding(16.dp),
                color = if (statusMessage.startsWith("Error")) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Información del reporte
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Datos de Prueba",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text("📊 Total de lecturas de glucosa: ${testGlucoseEntries.size}")
                Text("🍽️ Total de comidas registradas: ${testFoodEntries.size}")
                Text("💊 Total de medicamentos: ${testMedications.size}")
                Text("📞 Contactos de emergencia: ${testEmergencyContacts.size}")
                Text("📅 Período: ${getCurrentMonthYear()}")
                Text("📄 Formato: ${selectedFormat.name}")
            }
        }
    }
    
    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { 
                Text(
                    text = "Reporte Generado",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Column {
                    Text("El reporte se ha generado exitosamente en formato ${selectedFormat.name}.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¿Qué deseas hacer con el reporte?",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            generatedFile?.let { file ->
                                try {
                                    reportGenerator.shareReport(file, selectedFormat)
                                } catch (e: Exception) {
                                    statusMessage = "Error al compartir: ${e.message}"
                                }
                            }
                            showSuccessDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📤 Compartir Reporte")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            generatedFile?.let { file ->
                                try {
                                    val success = reportGenerator.downloadReport(file, selectedFormat)
                                    if (success) {
                                        val downloadPath = reportGenerator.getDownloadPath(selectedFormat)
                                        statusMessage = "✅ Reporte descargado en: $downloadPath"
                                    } else {
                                        statusMessage = "❌ Error al descargar el reporte"
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "❌ Error al descargar: ${e.message}"
                                }
                            }
                            showSuccessDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📥 Descargar Reporte")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSuccessDialog = false }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

private fun getCurrentMonthYear(): String {
    val dateFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("es", "ES"))
    return dateFormat.format(java.util.Date())
}
