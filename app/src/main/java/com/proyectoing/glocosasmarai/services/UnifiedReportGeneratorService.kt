package com.proyectoing.glocosasmarai.services

import android.content.Context
import com.proyectoing.glocosasmarai.models.EmergencyContact
import com.proyectoing.glocosasmarai.models.FoodEntry
import com.proyectoing.glocosasmarai.models.GlucoseEntry
import com.proyectoing.glocosasmarai.models.Medication
// 1. IMPORTACIÓN NUEVA NECESARIA
import com.proyectoing.glocosasmarai.models.MissedMedicationSummary
import java.io.File

enum class ReportFormat {
    PDF,
    WORD
}

class UnifiedReportGeneratorService(private val context: Context) {

    private val pdfGenerator = ReportGeneratorService(context)
    private val wordGenerator = WordReportGeneratorService(context)

    fun generateMonthlyReport(
        format: ReportFormat,
        glucoseEntries: List<GlucoseEntry>,
        foodEntries: List<FoodEntry>,
        medications: List<Medication>,
        emergencyContacts: List<EmergencyContact>,
        // 2. AÑADIMOS EL PARÁMETRO QUE FALTABA AQUÍ
        missedMedications: List<MissedMedicationSummary>,
        patientName: String = "Usuario",
        patientAge: Int = 0,
        patientDiabetesType: String = "Tipo 2",
        patientWeight: Float? = null,
        patientHeight: Float? = null,
        year: Int,
        month: Int
    ): File {
        return when (format) {
            ReportFormat.PDF -> {
                // 3. AHORA SÍ COINCIDEN LOS TIPOS
                pdfGenerator.generateMonthlyReport(
                    glucoseEntries = glucoseEntries,
                    foodEntries = foodEntries,
                    medications = medications,
                    emergencyContacts = emergencyContacts,
                    missedMedications = missedMedications, // <-- Pasamos la lista nueva
                    patientName = patientName,
                    patientAge = patientAge,
                    patientDiabetesType = patientDiabetesType,
                    patientWeight = patientWeight,
                    patientHeight = patientHeight,
                    year = year,
                    month = month
                )
            }
            ReportFormat.WORD -> {
                // NOTA: Como no modificamos el WordGenerator, no le pasamos la lista nueva
                // para evitar errores allí.
                wordGenerator.generateMonthlyReport(
                    glucoseEntries,
                    foodEntries,
                    medications,
                    emergencyContacts,
                    patientName,
                    patientAge,
                    patientDiabetesType,
                    patientWeight,
                    patientHeight,
                    year,
                    month
                )
            }
        }
    }

    // ... (El resto de funciones shareReport, downloadReport, etc. se quedan igual) ...
    fun shareReport(file: File, format: ReportFormat) {
        when (format) {
            ReportFormat.PDF -> pdfGenerator.sharePdfFile(file)
            ReportFormat.WORD -> wordGenerator.shareWordFile(file)
        }
    }

    fun downloadReport(file: File, format: ReportFormat): Boolean {
        return when (format) {
            ReportFormat.PDF -> pdfGenerator.downloadPdfFile(file)
            ReportFormat.WORD -> wordGenerator.downloadWordFile(file)
        }
    }

    fun getDownloadPath(format: ReportFormat): String {
        return when (format) {
            ReportFormat.PDF -> pdfGenerator.getDownloadPath()
            ReportFormat.WORD -> wordGenerator.getDownloadPath()
        }
    }

    fun getReportFileName(format: ReportFormat): String {
        val monthYear = getCurrentMonthYear()
        return when (format) {
            ReportFormat.PDF -> "Reporte_Mensual_${monthYear}.pdf"
            ReportFormat.WORD -> "Reporte_Mensual_${monthYear}.html"
        }
    }

    private fun getCurrentMonthYear(): String {
        val dateFormat = java.text.SimpleDateFormat("MMMM_yyyy", java.util.Locale("es", "ES"))
        return dateFormat.format(java.util.Date())
    }
}