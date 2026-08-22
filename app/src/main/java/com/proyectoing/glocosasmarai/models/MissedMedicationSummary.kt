package com.proyectoing.glocosasmarai.models

// Esta clase solo sirve para transportar los datos al reporte
data class MissedMedicationSummary(
    val name: String,      // Nombre (viene de la tabla medications)
    val type: String,      // Medicamento o Insulina
    val timestamp: Long    // Fecha y hora del olvido (viene de medication_history)
)