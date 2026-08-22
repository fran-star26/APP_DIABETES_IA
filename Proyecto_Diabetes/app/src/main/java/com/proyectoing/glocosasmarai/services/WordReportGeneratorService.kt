package com.proyectoing.glocosasmarai.services
/**
 *
 *Crea documento para reportes de salud mensuales
 *
 */
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.proyectoing.glocosasmarai.models.GlucoseEntry
import com.proyectoing.glocosasmarai.models.FoodEntry
import com.proyectoing.glocosasmarai.models.EmergencyContact
import com.proyectoing.glocosasmarai.models.Medication
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

private fun isSameMonth(timestamp: Long, year: Int, month: Int): Boolean {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
}


class WordReportGeneratorService(private val context: Context) {
    
    fun generateMonthlyReport(
        glucoseEntries: List<GlucoseEntry>,
        foodEntries: List<FoodEntry>,
        medications: List<Medication>,
        emergencyContacts: List<EmergencyContact>,
        patientName: String = "Usuario",
        patientAge: Int = 0,
        patientDiabetesType: String = "Tipo 2",
        patientWeight: Float? = null,
        patientHeight: Float? = null,
        year: Int, // <-- AÑADE ESTA LÍNEA
        month: Int
    ): File {
        val reportMonthYear = getReportMonthYear(year, month)
        val fileName = "Reporte_Mensual_${reportMonthYear}.html"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        try {
            FileWriter(file).use { writer ->
                // Encabezado HTML
                writer.write("""
                    <!DOCTYPE html>
                    <html lang="es">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Reporte Mensual de Diabetes</title>
                        <style>
                            body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }
                            .header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 20px; margin-bottom: 30px; }
                            .title { font-size: 28px; font-weight: bold; color: #2c3e50; margin-bottom: 10px; }
                            .subtitle { font-size: 18px; color: #7f8c8d; }
                            .section { margin-bottom: 30px; }
                            .section-title { font-size: 20px; font-weight: bold; color: #34495e; border-bottom: 1px solid #bdc3c7; padding-bottom: 5px; margin-bottom: 15px; }
                            table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                            th { background-color: #f2f2f2; font-weight: bold; }
                            .info-table { width: 60%; }
                            .status-critical { color: #e74c3c; font-weight: bold; }
                            .status-high { color: #e67e22; font-weight: bold; }
                            .status-low { color: #f39c12; font-weight: bold; }
                            .status-normal { color: #27ae60; font-weight: bold; }
                            .footer { text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #bdc3c7; color: #7f8c8d; }
                            .recommendation { margin-bottom: 10px; }
                        </style>
                    </head>
                    <body>
                """.trimIndent())
                val filteredGlucose = glucoseEntries.filter { isSameMonth(it.timestamp, year, month) }
                val filteredFood = foodEntries.filter { isSameMonth(it.timestamp, year, month) }
                val filteredMeds = medications.filter { isSameMonth(it.endDate, year, month) }
                // Título principal
                addTitle(writer)
                
                // Información del paciente
                addPatientInfo(writer, patientName, patientAge, patientDiabetesType, patientWeight, patientHeight, year, month)

                // Resumen ejecutivo
                addExecutiveSummary(writer, filteredGlucose, filteredFood) // <-- MODIFICADO

                // Registros de glucosa
                addGlucoseSection(writer, filteredGlucose) // <-- MODIFICADO

                // Registros de comida
                addFoodSection(writer, filteredFood) // <-- MODIFICADO

                // (La sección de Medicamentos que añadimos antes)
                addMedicationSection(writer, filteredMeds) // <-- MODIFICADO

                // Análisis de advertencias
                addWarningsAnalysis(writer, filteredGlucose) // <-- MODIFICADO

                // Contactos de emergencia
                addEmergencyContacts(writer, emergencyContacts) // (No se filtra)

                // Recomendaciones
                addRecommendations(writer, filteredGlucose, filteredFood)
                
                // Pie de página
                addFooter(writer)
                
                // Cerrar HTML
                writer.write("</body></html>")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return file
    }
    
    private fun addTitle(writer: FileWriter) {
        writer.write("""
            <div class="header">
                <div class="title">REPORTE MENSUAL DE DIABETES</div>
                <div class="subtitle">GlucosaSmart IA</div>
            </div>
        """.trimIndent())
    }

    private fun addPatientInfo(
        writer: FileWriter,
        patientName: String,
        patientAge: Int,
        patientDiabetesType: String,
        patientWeight: Float? = null, // <-- NUEVOS PARÁMETROS
        patientHeight: Float? = null, // <-- NUEVOS PARÁMETROS
        year: Int,
        month: Int
    ) {
        // --- LÓGICA DE CÁLCULO DE IMC ---
        val imcResult = if (patientWeight != null && patientHeight != null && patientHeight > 0f) {
            val heightInMeters = patientHeight / 100f // Altura está en cm en tu modelo
            val imc = patientWeight / (heightInMeters * heightInMeters)

            // Llamamos a la función de clasificación del PDF (que ahora está dentro de la otra clase)
            val category = ReportGeneratorService(context).getBMICategory(imc)
            "<tr><td><strong>IMC:</strong></td><td>${"%.1f".format(imc)} (${category})</td></tr>"
        } else ""
        // --- FIN DE LÓGICA ---

        writer.write("""
        <div class="section">
            <div class="section-title">INFORMACIÓN DEL PACIENTE</div>
            <table class="info-table">
                <tr><td><strong>Nombre:</strong></td><td>$patientName</td></tr>
                <tr><td><strong>Edad:</strong></td><td>$patientAge años</td></tr>
                <tr><td><strong>Tipo de Diabetes:</strong></td><td>$patientDiabetesType</td></tr>
                ${if (patientWeight != null) "<tr><td><strong>Peso:</strong></td><td>${"%.1f".format(patientWeight)} kg</td></tr>" else ""}
                ${if (patientHeight != null) "<tr><td><strong>Estatura:</strong></td><td>${"%.2f".format(patientHeight)} m</td></tr>" else ""}
                $imcResult 
                <tr><td><strong>Período del Reporte:</strong></td><td>${getReportMonthYear(year, month)}</td></tr>
                <tr><td><strong>Fecha de Generación:</strong></td><td>${getCurrentDateTime()}</td></tr>
            </table>
        </div>
    """.trimIndent())
    }
    
    private fun addExecutiveSummary(
        writer: FileWriter,
        glucoseEntries: List<GlucoseEntry>,
        foodEntries: List<FoodEntry>
    ) {
        val totalGlucoseReadings = glucoseEntries.size
        val averageGlucose = if (glucoseEntries.isNotEmpty()) {
            glucoseEntries.map { it.value }.average()
        } else 0.0
        
        val highGlucoseCount = glucoseEntries.count { it.value > 180 }
        val lowGlucoseCount = glucoseEntries.count { it.value < 70 }
        val normalGlucoseCount = glucoseEntries.count { it.value in 70..180 }
        val totalMeals = foodEntries.size
        
        writer.write("""
            <div class="section">
                <div class="section-title">RESUMEN EJECUTIVO</div>
                <table class="info-table">
                    <tr><td><strong>Total de lecturas de glucosa:</strong></td><td>$totalGlucoseReadings</td></tr>
                    <tr><td><strong>Promedio de glucosa:</strong></td><td>${String.format("%.1f", averageGlucose)} mg/dL</td></tr>
                    <tr><td><strong>Lecturas altas (>180 mg/dL):</strong></td><td>$highGlucoseCount</td></tr>
                    <tr><td><strong>Lecturas bajas (<70 mg/dL):</strong></td><td>$lowGlucoseCount</td></tr>
                    <tr><td><strong>Lecturas normales (70-180 mg/dL):</strong></td><td>$normalGlucoseCount</td></tr>
                    <tr><td><strong>Total de comidas registradas:</strong></td><td>$totalMeals</td></tr>
                </table>
            </div>
        """.trimIndent())
    }
    
    private fun addGlucoseSection(
        writer: FileWriter,
        glucoseEntries: List<GlucoseEntry>
    ) {
        writer.write("""
            <div class="section">
                <div class="section-title">REGISTROS DE GLUCOSA</div>
        """.trimIndent())
        
        if (glucoseEntries.isEmpty()) {
            writer.write("<p>No hay registros de glucosa para este período.</p>")
        } else {
            writer.write("""
                <table>
                    <tr>
                        <th>Fecha</th>
                        <th>Hora</th>
                        <th>Valor (mg/dL)</th>
                        <th>Tipo</th>
                        <th>Estado</th>
                    </tr>
            """.trimIndent())
            
            // Datos ordenados por fecha (más recientes primero)
            val sortedEntries = glucoseEntries.sortedByDescending { it.timestamp }
            
            sortedEntries.forEach { entry ->
                val dateTime = formatDate(entry.timestamp)
                val date = dateTime.split(" ")[0]
                val time = dateTime.split(" ")[1]
                
                val glucoseValue = entry.value.toString()
                val mealType = if (entry.isBeforeMeal) "Antes comida" else "Después comida"
                
                val status = when {
                    entry.value > 250 -> "CRÍTICO"
                    entry.value > 180 -> "ALTO"
                    entry.value < 70 -> "BAJO"
                    else -> "NORMAL"
                }
                
                val statusClass = when {
                    entry.value > 250 -> "status-critical"
                    entry.value > 180 -> "status-high"
                    entry.value < 70 -> "status-low"
                    else -> "status-normal"
                }
                
                writer.write("""
                    <tr>
                        <td>$date</td>
                        <td>$time</td>
                        <td>$glucoseValue</td>
                        <td>$mealType</td>
                        <td class="$statusClass">$status</td>
                    </tr>
                """.trimIndent())
            }
            
            writer.write("</table>")
        }
        
        writer.write("</div>")
    }
    
    private fun addFoodSection(
        writer: FileWriter,
        foodEntries: List<FoodEntry>
    ) {
        writer.write("""
            <div class="section">
                <div class="section-title">REGISTROS DE COMIDA</div>
        """.trimIndent())
        
        if (foodEntries.isEmpty()) {
            writer.write("<p>No hay registros de comida para este período.</p>")
        } else {
            writer.write("""
                <table>
                    <tr>
                        <th>Fecha</th>
                        <th>Hora</th>
                        <th>Tipo de Comida</th>
                        <th>Descripción</th>
                    </tr>
            """.trimIndent())
            
            // Datos ordenados por fecha (más recientes primero)
            val sortedEntries = foodEntries.sortedByDescending { it.timestamp }
            
            sortedEntries.forEach { entry ->
                val dateTime = formatDate(entry.timestamp)
                val date = dateTime.split(" ")[0]
                val time = dateTime.split(" ")[1]
                
                val mealType = entry.type
                val description = entry.description.ifEmpty { "-" }
                
                writer.write("""
                    <tr>
                        <td>$date</td>
                        <td>$time</td>
                        <td>$mealType</td>
                        <td>$description</td>
                    </tr>
                """.trimIndent())
            }
            
            writer.write("</table>")
        }
        
        writer.write("</div>")
    }

    private fun addMedicationSection(
        writer: FileWriter,
        medications: List<Medication>
    ) {
        writer.write("""
            <div class="section">
                <div class="section-title">REGISTROS DE MEDICAMENTOS</div>
        """.trimIndent())

        if (medications.isEmpty()) {
            writer.write("<p>No hay registros de medicamentos para este período.</p>")
        } else {
            writer.write("""
                <table>
                    <tr>
                        <th>Tipo</th>
                        <th>Nombre</th>
                        <th>Dosis</th>
                        <th>Hora</th>
                        <th>Fecha Fin</th>
                    </tr>
            """.trimIndent())

            // Datos ordenados por fecha de finalización
            val sortedMeds = medications.sortedByDescending { it.endDate }

            sortedMeds.forEach { med ->
                val medType = med.type ?: "Medicina"
                val medName = med.name
                val medDose = med.dose
                val medTime = String.format("%02d:%02d", med.hour, med.minute)
                // Reutilizamos la función formatDate y tomamos solo la fecha
                val medEndDate = formatDate(med.endDate).split(" ").getOrElse(0) { "" }

                writer.write("""
                    <tr>
                        <td>$medType</td>
                        <td>$medName</td>
                        <td>$medDose</td>
                        <td>$medTime</td>
                        <td>$medEndDate</td>
                    </tr>
                """.trimIndent())
            }

            writer.write("</table>")
        }

        writer.write("</div>")
    }
    
    private fun addWarningsAnalysis(
        writer: FileWriter,
        glucoseEntries: List<GlucoseEntry>
    ) {
        writer.write("""
            <div class="section">
                <div class="section-title">ANÁLISIS DE ADVERTENCIAS</div>
        """.trimIndent())
        
        val emergencyAlerts = glucoseEntries.filter { it.value > 250 }
        val warningAlerts = glucoseEntries.filter { it.value < 70 }
        
        if (emergencyAlerts.isEmpty() && warningAlerts.isEmpty()) {
            writer.write("<p>No se registraron advertencias durante este período.</p>")
        } else {
            if (emergencyAlerts.isNotEmpty()) {
                writer.write("<h4>ALERTAS DE EMERGENCIA (Glucosa > 250 mg/dL):</h4><ul>")
                emergencyAlerts.forEach { entry ->
                    writer.write("<li>${formatDate(entry.timestamp)} - ${entry.value} mg/dL - ${entry.notes}</li>")
                }
                writer.write("</ul>")
            }
            
            if (warningAlerts.isNotEmpty()) {
                writer.write("<h4>ADVERTENCIAS (Glucosa < 70 mg/dL):</h4><ul>")
                warningAlerts.forEach { entry ->
                    writer.write("<li>${formatDate(entry.timestamp)} - ${entry.value} mg/dL - ${entry.notes}</li>")
                }
                writer.write("</ul>")
            }
            
            writer.write("<p><strong>Total de alertas: ${emergencyAlerts.size + warningAlerts.size}</strong></p>")
        }
        
        writer.write("</div>")
    }
    
    private fun addEmergencyContacts(
        writer: FileWriter,
        emergencyContacts: List<EmergencyContact>
    ) {
        writer.write("""
            <div class="section">
                <div class="section-title">CONTACTOS DE EMERGENCIA</div>
        """.trimIndent())
        
        if (emergencyContacts.isEmpty()) {
            writer.write("<p>No hay contactos de emergencia registrados.</p>")
        } else {
            writer.write("""
                <table class="info-table">
                    <tr>
                        <th>Nombre</th>
                        <th>Teléfono</th>
                    </tr>
            """.trimIndent())
            
            emergencyContacts.forEach { contact ->
                writer.write("""
                    <tr>
                        <td>${contact.name}</td>
                        <td>${contact.phone}</td>
                    </tr>
                """.trimIndent())
            }
            
            writer.write("</table>")
        }
        
        writer.write("</div>")
    }
    
    private fun addRecommendations(
        writer: FileWriter,
        glucoseEntries: List<GlucoseEntry>,
        foodEntries: List<FoodEntry>
    ) {
        writer.write("""
            <div class="section">
                <div class="section-title">RECOMENDACIONES</div>
        """.trimIndent())
        
        val recommendations = mutableListOf<String>()
        
        // Análisis de glucosa
        if (glucoseEntries.isNotEmpty()) {
            val averageGlucose = glucoseEntries.map { it.value }.average()
            val highGlucoseCount = glucoseEntries.count { it.value > 180 }
            val lowGlucoseCount = glucoseEntries.count { it.value < 70 }
            
            when {
                averageGlucose > 180 -> recommendations.add("Tu glucosa promedio está alta. Considera revisar tu dieta y medicación.")
                averageGlucose < 100 -> recommendations.add("Tu glucosa promedio está baja. Consulta con tu médico sobre ajustes en medicación.")
                else -> recommendations.add("Tu glucosa promedio está en rango normal. ¡Mantén estos buenos hábitos!")
            }
            
            if (highGlucoseCount > glucoseEntries.size * 0.3) {
                recommendations.add("Tienes muchas lecturas altas. Revisa tu alimentación y horarios de medicación.")
            }
            
            if (lowGlucoseCount > 0) {
                recommendations.add("Has tenido episodios de glucosa baja. Ten siempre algo dulce a mano.")
            }
        }
        
        // Análisis de comidas
        if (foodEntries.isNotEmpty()) {
            val mealTypes = foodEntries.groupBy { it.type }
            if (mealTypes.size < 3) {
                recommendations.add("Intenta mantener horarios regulares para tus comidas principales.")
            }
        }
        
        // Recomendaciones generales
        recommendations.add("Continúa monitoreando tu glucosa regularmente.")
        recommendations.add("Mantén una dieta balanceada y horarios regulares.")
        recommendations.add("Realiza actividad física moderada regularmente.")
        recommendations.add("Consulta con tu médico si notas patrones preocupantes.")
        
        writer.write("<ul>")
        recommendations.forEach { recommendation ->
            writer.write("<li class=\"recommendation\">$recommendation</li>")
        }
        writer.write("</ul>")
        
        writer.write("</div>")
    }
    
    private fun addFooter(writer: FileWriter) {
        writer.write("""
            <div class="footer">
                <p>Reporte generado por GlucosaSmart IA - ${getCurrentDateTime()}</p>
            </div>
        """.trimIndent())
    }

    private fun getReportMonthYear(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        val dateFormat = SimpleDateFormat("MMMM_yyyy", Locale("es", "ES"))
        return dateFormat.format(cal.time)
    }
    
    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))
        return dateFormat.format(Date())
    }
    
    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))
        return formatter.format(date)
    }
    
    fun shareWordFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte Mensual de Diabetes")
            putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte mensual de diabetes generado por GlucosaSmart IA. Este archivo HTML se puede abrir en Word o cualquier navegador web.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Compartir Reporte HTML"))
    }

    fun downloadWordFile(file: File): Boolean {
        return try {
            // Crear directorio de descargas si no existe
            val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Copiar archivo a la carpeta de descargas
            val destinationFile = File(downloadsDir, file.name)
            file.copyTo(destinationFile, overwrite = true)

            // Notificar al sistema de archivos que hay un nuevo archivo
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = android.net.Uri.fromFile(destinationFile)
            context.sendBroadcast(intent)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDownloadPath(): String {
        val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
        return downloadsDir.absolutePath
    }
}
