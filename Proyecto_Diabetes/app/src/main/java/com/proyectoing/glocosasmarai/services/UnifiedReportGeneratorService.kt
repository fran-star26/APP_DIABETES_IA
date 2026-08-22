package com.proyectoing.glocosasmarai.services

import android.content.Context
import com.proyectoing.glocosasmarai.models.EmergencyContact
import com.proyectoing.glocosasmarai.models.FoodEntry
import com.proyectoing.glocosasmarai.models.GlucoseEntry
import com.proyectoing.glocosasmarai.models.Medication
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
        patientName: String = "Usuario",
        patientAge: Int = 0,
        patientDiabetesType: String = "Tipo 2",
        patientWeight: Float? = null, // <-- AÑADE ESTA LÍNEA
        patientHeight: Float? = null,
        year: Int,   // <-- ESTO DEBE ESTAR AQUÍ
        month: Int
    ): File {
        return when (format) {
            ReportFormat.PDF -> {
                pdfGenerator.generateMonthlyReport(
                    glucoseEntries,
                    foodEntries,
                    medications,
                    emergencyContacts,
                    patientName,
                    patientAge,
                    patientDiabetesType,
                    patientWeight, // <-- AÑADE ESTA LÍNEA
                    patientHeight,
                    year, // <-- AÑADE ESTA LÍNEA
                    month
                )
            }
            ReportFormat.WORD -> {
                wordGenerator.generateMonthlyReport(
                    glucoseEntries,
                    foodEntries,
                    medications,
                    emergencyContacts,
                    patientName,
                    patientAge,
                    patientDiabetesType,
                    patientWeight, // <-- AÑADE ESTA LÍNEA
                    patientHeight,
                    year,
                    month
                )
            }
        }
    }
    
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
