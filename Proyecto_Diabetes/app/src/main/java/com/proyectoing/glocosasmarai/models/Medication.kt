package com.proyectoing.glocosasmarai.models

data class Medication(
    val id: Long = 0,
    val name: String,
    val dose: String,
    val hour: Int,
    val minute: Int,
    val endDate: Long,
    val trackingState: String = "PENDING",
    val type: String = "Medicamento", // <-- NUEVO CAMPO: "Medicamento" o "Insulina"
)