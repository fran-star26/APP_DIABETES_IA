package com.proyectoing.glocosasmarai.services

import android.content.Context
import android.content.Intent
import android.content.ContentValues
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.proyectoing.glocosasmarai.models.GlucoseEntry
import com.proyectoing.glocosasmarai.models.FoodEntry
import com.proyectoing.glocosasmarai.models.EmergencyContact
import com.proyectoing.glocosasmarai.models.Medication
import com.proyectoing.glocosasmarai.models.MissedMedicationSummary
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

private fun isSameMonth(timestamp: Long, year: Int, month: Int): Boolean {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
}


class ReportGeneratorService(private val context: Context) {

    fun generateMonthlyReport(
        glucoseEntries: List<GlucoseEntry>,
        foodEntries: List<FoodEntry>,
        medications: List<Medication>,
        emergencyContacts: List<EmergencyContact>,
        missedMedications: List<MissedMedicationSummary>,
        patientName: String = "Usuario",
        patientAge: Int = 0,
        patientDiabetesType: String = "Tipo 2",
        patientWeight: Float? = null,
        patientHeight: Float? = null,
        year: Int,
        month: Int
    ): File {
        val reportMonthYear = getReportMonthYear(year, month)
        val fileName = "Reporte_Mensual_${reportMonthYear}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        val document = PdfDocument()
        var currentPage = 1
        var yPos = 50

        try {
            // Crear primera página
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            // Configurar paints para diferentes estilos
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.BLACK
                textSize = 16f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val normalPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }

            val smallPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                isAntiAlias = true
            }

            // Función para crear nueva página
            fun createNewPage() {
                document.finishPage(page)
                currentPage++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50
            }

            // Función para verificar si necesita nueva página
            fun checkNewPage(additionalLines: Int = 1): Boolean {
                return yPos + (additionalLines * 20) > 800
            }

            // Función para dibujar texto con manejo de nueva página
            fun drawText(text: String, paint: Paint, x: Float = 50f) {
                if (checkNewPage()) {
                    createNewPage()
                }
                canvas.drawText(text, x, yPos.toFloat(), paint)
                yPos += 20
            }

            // Título principal
            drawText("REPORTE MENSUAL DE DIABETES", titlePaint)
            yPos += 10
            drawText("GlucosaSmart IA", subtitlePaint)
            yPos += 20

            // Información del paciente
            drawText("INFORMACIÓN DEL PACIENTE", headerPaint)
            yPos += 5
            drawText("Nombre: $patientName", normalPaint)
            drawText("Edad: $patientAge años", normalPaint)
            drawText("Tipo de Diabetes: $patientDiabetesType", normalPaint)
            if (patientWeight != null) {
                drawText("Peso: ${"%.1f".format(patientWeight)} kg", normalPaint)
            }
            if (patientHeight != null) {
                drawText("Estatura: ${"%.2f".format(patientHeight)} m", normalPaint)
            }
            // Cálculo y Categoría del IMC (similar a como lo haces en la app)
            if (patientWeight != null && patientHeight != null && patientHeight > 0f) {
                val heightInMeters = patientHeight // Asumo que guardas height en cm
                val imc = patientWeight / (heightInMeters * heightInMeters)
                val category = getBMICategory(imc) // Usaremos una nueva función auxiliar
                drawText("IMC: ${"%.1f".format(imc)} (${category})", normalPaint)
            }
            drawText("Período del Reporte: $reportMonthYear", normalPaint)
            drawText("Fecha de Generación: ${getCurrentDateTime()}", normalPaint)
            yPos += 10

            val filteredGlucose = glucoseEntries.filter { isSameMonth(it.timestamp, year, month) }
            val filteredFood = foodEntries.filter { isSameMonth(it.timestamp, year, month) }
            val filteredMeds = medications.filter { isSameMonth(it.endDate, year, month) } // Filtramos por fecha fin
            val filteredContacts = emergencyContacts

            // Resumen ejecutivo
            if (checkNewPage(10)) createNewPage()
            drawText("RESUMEN EJECUTIVO", headerPaint)
            yPos += 5

            val totalGlucoseReadings = filteredGlucose.size // <-- CORREGIDO
            val averageGlucose = if (filteredGlucose.isNotEmpty()) { // <-- CORREGIDO
                filteredGlucose.map { it.value.toDouble() }.average()
            } else 0.0

            val highGlucoseCount = filteredGlucose.count { it.value > 180 } // <-- CORREGIDO
            val lowGlucoseCount = filteredGlucose.count { it.value < 70 } // <-- CORREGIDO
            val normalGlucoseCount = filteredGlucose.count { it.value in 70..180 } // <-- CORREGIDO
            val totalMeals = filteredFood.size
            val totalCarbsMonth = filteredFood.sumOf { it.carbohydrates ?: 0 }
            val totalSugarsMonth = filteredFood.sumOf { it.sugars ?: 0 }

            drawText("Total de lecturas de glucosa: $totalGlucoseReadings", normalPaint)
            drawText("Promedio de glucosa: ${String.format("%.1f", averageGlucose)} mg/dL", normalPaint)
            drawText("Lecturas altas (>180 mg/dL): $highGlucoseCount", normalPaint)
            drawText("Lecturas bajas (<70 mg/dL): $lowGlucoseCount", normalPaint)
            drawText("Lecturas normales (70-180 mg/dL): $normalGlucoseCount", normalPaint)
            drawText("Total de comidas registradas: $totalMeals", normalPaint)
            drawText("Total de comidas registradas: $totalMeals", normalPaint)
            drawText("Total Carbohidratos (Mes): ${totalCarbsMonth}g", normalPaint)
            drawText("Total Azúcares (Mes): ${totalSugarsMonth}g", normalPaint)
            yPos += 10
            // Registros de glucosa
            if (checkNewPage(5)) createNewPage()
            drawText("REGISTROS DE GLUCOSA", headerPaint)
            yPos += 5

            if (glucoseEntries.isEmpty()) {
                drawText("No hay registros de glucosa para este período.", normalPaint)
                yPos += 10
            } else {
                val sortedEntries = glucoseEntries.sortedByDescending { it.timestamp }
                sortedEntries.take(50).forEach { entry -> // Limitar a 50 entradas para evitar problemas de memoria
                    val dateTime = formatDate(entry.timestamp)
                    val parts = dateTime.split(" ")
                    val date = parts.getOrElse(0) { "" }
                    val time = parts.getOrElse(1) { "" }

                    val glucoseValue = entry.value.toString()
                    val mealType = if (entry.isBeforeMeal) "Antes" else "Después"

                    val status = when {
                        entry.value > 250 -> "CRÍTICO"
                        entry.value > 180 -> "ALTO"
                        entry.value < 70 -> "BAJO"
                        else -> "NORMAL"
                    }

                    val line = "$date $time - $glucoseValue mg/dL - $mealType - $status"
                    drawText(line, smallPaint)
                }
                if (glucoseEntries.size > 50) {
                    drawText("... y ${glucoseEntries.size - 50} registros más", smallPaint, 65f)
                }
                yPos += 10
            }

            // Registros de comida
            if (checkNewPage(5)) createNewPage()
            drawText("REGISTROS DE COMIDA", headerPaint)
            yPos += 5

            if (foodEntries.isEmpty()) {
                drawText("No hay registros de comida para este período.", normalPaint)
                yPos += 10
            } else {
                val sortedEntries = foodEntries.sortedByDescending { it.timestamp }
                sortedEntries.take(30).forEach { entry -> // Limitar a 30 entradas
                    val dateTime = formatDate(entry.timestamp)
                    val parts = dateTime.split(" ")
                    val date = parts.getOrElse(0) { "" }
                    val time = parts.getOrElse(1) { "" }

                    val mealType = entry.type
                    val description = if (entry.description.isNotEmpty()) entry.description else "-"

                    // --- CONSTRUCCIÓN DE LA INFO NUTRICIONAL ---
                    // Creamos una lista solo con los datos que existan (no nulos)
                    val nutritionalInfo = ArrayList<String>()

                    entry.calories?.let { nutritionalInfo.add("$it kcal") }
                    entry.carbohydrates?.let { nutritionalInfo.add("Carbohidratos:${it}g") } // C = Carbs
                    entry.sugars?.let { nutritionalInfo.add("Azucares:${it}g") }       // A = Azúcar

                    // Unimos todo con comas. Ej: "(400 kcal, C:30g, A:10g)"
                    val nutritionalString = if (nutritionalInfo.isNotEmpty()) {
                        "(${nutritionalInfo.joinToString(", ")})"
                    } else {
                        ""
                    }
                    // -------------------------------------------

                    val line = "$date $time - $mealType - $description $nutritionalString"
                    drawText(line, smallPaint)
                }
                if (foodEntries.size > 30) {
                    drawText("... y ${foodEntries.size - 30} registros más", smallPaint, 65f)
                }
                yPos += 10
            }

            // Registros de medicamentos
            if (checkNewPage(5)) createNewPage()
            drawText("REGISTROS DE MEDICAMENTOS", headerPaint)
            yPos += 5

            if (medications.isEmpty()) {
                drawText("No hay registros de medicamentos para este período.", normalPaint)
                yPos += 10
            } else {
                val sortedMeds = medications.sortedByDescending { it.endDate }
                sortedMeds.take(30).forEach { med -> // Limitar a 30 medicamentos
                    val medType = med.type ?: "Medicina"
                    val medName = med.name
                    val medDose = med.dose
                    val medTime = String.format("%02d:%02d", med.hour, med.minute)
                    // Reutilizamos la función formatDate y tomamos solo la fecha
                    val medEndDate = formatDate(med.endDate).split(" ").getOrElse(0) { "" }

                    val line = "• $medType - $medName ($medDose) - $medTime - Hasta: $medEndDate"
                    drawText(line, smallPaint, 65f)
                }
                if (medications.size > 30) {
                    drawText("... y ${medications.size - 30} registros más", smallPaint, 65f)
                }
                yPos += 10
            }
            // --- INICIO DE NUEVA SECCIÓN: MEDICAMENTOS OLVIDADOS ---
            if (checkNewPage(5)) createNewPage()
            drawText("CUMPLIMIENTO DE TRATAMIENTO (OLVIDOS)", headerPaint)
            yPos += 5

            if (missedMedications.isEmpty()) {
                drawText("¡Excelente! No se registraron olvidos en este período.", normalPaint)
                yPos += 10
            } else {
                // Ordenar por fecha (del más antiguo al más reciente o viceversa)
                val sortedMissed = missedMedications.sortedBy { it.timestamp }

                sortedMissed.forEach { item ->
                    // Usamos tu función formatDate que ya devuelve "dd/MM/yyyy HH:mm"
                    val dateTime = formatDate(item.timestamp)

                    // Diferenciar texto según si es Insulina o Pastilla
                    val actionText = if (item.type == "Insulina") "No aplicada" else "No tomada"

                    val line = "• $dateTime - ${item.name} - $actionText"
                    drawText(line, smallPaint, 65f)
                }

                yPos += 5
                drawText("Total de incidentes: ${missedMedications.size}", normalPaint)
                yPos += 10
            }

            // Análisis de advertencias
            if (checkNewPage(5)) createNewPage()
            drawText("ANÁLISIS DE ADVERTENCIAS", headerPaint)
            yPos += 5

            val emergencyAlerts = glucoseEntries.filter { it.value > 250 }
            val warningAlerts = glucoseEntries.filter { it.value < 70 }

            if (emergencyAlerts.isEmpty() && warningAlerts.isEmpty()) {
                drawText("No se registraron advertencias durante este período.", normalPaint)
                yPos += 10
            } else {
                if (emergencyAlerts.isNotEmpty()) {
                    if (checkNewPage(3)) createNewPage()
                    drawText("ALERTAS DE EMERGENCIA (Glucosa > 250 mg/dL):", normalPaint)
                    yPos += 5
                    emergencyAlerts.take(20).forEach { entry -> // Limitar a 20 alertas
                        val line = "• ${formatDate(entry.timestamp)} - ${entry.value} mg/dL - ${entry.notes ?: ""}"
                        drawText(line, smallPaint, 65f)
                    }
                    if (emergencyAlerts.size > 20) {
                        drawText("... y ${emergencyAlerts.size - 20} alertas más", smallPaint, 65f)
                    }
                    yPos += 5
                }

                if (warningAlerts.isNotEmpty()) {
                    if (checkNewPage(3)) createNewPage()
                    drawText("ADVERTENCIAS (Glucosa < 70 mg/dL):", normalPaint)
                    yPos += 5
                    warningAlerts.take(20).forEach { entry -> // Limitar a 20 advertencias
                        val line = "• ${formatDate(entry.timestamp)} - ${entry.value} mg/dL - ${entry.notes ?: ""}"
                        drawText(line, smallPaint, 65f)
                    }
                    if (warningAlerts.size > 20) {
                        drawText("... y ${warningAlerts.size - 20} advertencias más", smallPaint, 65f)
                    }
                    yPos += 5
                }

                drawText("Total de alertas: ${emergencyAlerts.size + warningAlerts.size}", normalPaint)
                yPos += 10
            }

                         // Sección eliminada: Contactos de emergencia
             // Sección eliminada: Recomendaciones

            // Pie de página
            if (checkNewPage(3)) createNewPage()
            yPos += 10
            drawText("=".repeat(50), normalPaint)
            yPos += 5
            drawText("Reporte generado por GlucosaSmart IA - ${getCurrentDateTime()}", smallPaint)

            document.finishPage(page)

            // Escribir el archivo
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Si hay error, crear un archivo de texto simple como respaldo
            try {
                // --- Reemplaza getCurrentMonthYear() con la llamada a la nueva función ---
                // Usamos el mes y año actual si no tenemos los de los parámetros.
                val cal = Calendar.getInstance()
                val currentMonthYear = getReportMonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))

                val backupFile = File(context.getExternalFilesDir(null), "Reporte_Error_${currentMonthYear}.txt")
                backupFile.writeText("Error generando PDF: ${e.message}\n\nReporte generado el: ${getCurrentDateTime()}")
                return backupFile
            } catch (backupError: Exception) {
                backupError.printStackTrace()
            }
        } finally {
            document.close()
        }

        return file
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

    fun sharePdfFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte Mensual de Diabetes")
            putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte mensual de diabetes generado por GlucosaSmart IA.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Compartir Reporte PDF"))
    }

    fun downloadPdfFile(file: File): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri).use { outputStream ->
                    FileInputStream(file).use { inputStream ->
                        inputStream.copyTo(outputStream!!)
                    }
                }
                // Opcional: Borrar el archivo privado después de copiarlo
                file.delete()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDownloadPath(): String {
        // Devuelve la ruta pública estándar de Descargas
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    }

    fun getBMICategory(imc: Float): String {
        return when {
            imc < 18.5f -> "Bajo peso"
            imc < 25f -> "Peso normal"
            imc < 30f -> "Sobrepeso"
            imc < 35f -> "Obesidad Clase 1"
            imc < 40f -> "Obesidad Clase 2"
            else -> "Obesidad Clase 3"
        }
    }
    fun openPdfFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Iniciar la actividad (el visor de PDF)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Manejar error si no hay un visor de PDF instalado
            Toast.makeText(context, "No se encontró una app para abrir PDF", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}