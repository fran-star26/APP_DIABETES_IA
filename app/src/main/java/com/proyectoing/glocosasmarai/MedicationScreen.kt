package com.proyectoing.glocosasmarai

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.proyectoing.glocosasmarai.models.Medication
import com.proyectoing.glocosasmarai.services.MedicationAlarmScheduler
import java.text.SimpleDateFormat
import java.util.*
import com.proyectoing.glocosasmarai.services.LocalStorageService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localStorageService = remember { LocalStorageService(context) }
    val scheduler = remember { MedicationAlarmScheduler(context) }

    val medicationsFlow = remember { localStorageService.getAllMedications() }
    val currentMedications by medicationsFlow.collectAsState(initial = emptyList())

    var medName by rememberSaveable { mutableStateOf("") }
    var medDose by rememberSaveable { mutableStateOf("") }
    var medType by rememberSaveable { mutableStateOf("Medicamento") }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedEndDate by rememberSaveable { mutableStateOf<Long?>(null) }

    val todayUTC = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val dateSelectionRule = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return utcTimeMillis >= todayUTC
        }
    }

    val timeState = rememberTimePickerState()
    val dateState = rememberDatePickerState(selectableDates = dateSelectionRule)

    var showEditDialog by remember { mutableStateOf(false) }
    var medicationToEdit by remember { mutableStateOf<Medication?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "Se necesitan permisos de notificación para los recordatorios", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Mis Medicamentos e Insulina",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tipo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = Color(0xFF2196F3),
                        selectedLabelColor = Color.White
                    )
                    FilterChip(
                        selected = medType == "Medicamento",
                        onClick = { medType = "Medicamento" },
                        label = { Text("💊 Medicamento") },
                        colors = chipColors
                    )
                    FilterChip(
                        selected = medType == "Insulina",
                        onClick = { medType = "Insulina" },
                        label = { Text("💉 Insulina") },
                        colors = chipColors
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (medType != "Insulina") {
                OutlinedTextField(
                    value = medName,
                    onValueChange = { medName = it },
                    label = { Text("Nombre del medicamento") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = medDose,
                onValueChange = { medDose = it },
                label = {
                    Text(
                        if (medType == "Insulina") "Unidades de insulina"
                        else "Dosis"
                    )
                },
                placeholder = {
                    Text(
                        if (medType == "Insulina") "Ej: 10, 15, 20 unidades"
                        else "Ej: 1 pastilla, 500mg, etc."
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = "Hora")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(String.format("%02d:%02d", timeState.hour, timeState.minute))
                }
                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF48FB1),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Fecha fin")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedEndDate != null)
                            formatTimestamp(selectedEndDate!!, "dd/MM/yy")
                        else
                            "Fecha Fin"
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val isInsulin = medType == "Insulina"
                    val nameToSave = if (isInsulin) "Insulina" else medName
                    val isValid = medDose.isNotBlank() && selectedEndDate != null && (isInsulin || medName.isNotBlank())

                    if (isValid) {
                        val newMedication = Medication(
                            id = System.currentTimeMillis(),
                            name = nameToSave,
                            dose = medDose,
                            hour = timeState.hour,
                            minute = timeState.minute,
                            endDate = selectedEndDate!!,
                            trackingState = "PENDING",
                            type = medType
                        )

                        scope.launch {
                            localStorageService.saveMedication(newMedication)
                            scheduler.schedule(newMedication)

                            medName = ""
                            medDose = ""
                            selectedEndDate = null

                            Toast.makeText(context, "Recordatorio guardado correctamente", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text(
                    if (medType == "Insulina") "Guardar Recordatorio de Insulina"
                    else "Guardar Recordatorio"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recordatorios Activos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(currentMedications) { med ->
            val cardColor = when (med.trackingState) {
                "TAKEN" -> Color(0xFFC8E6C9)
                "SNOOZED", "MISSED" -> Color(0xFFFFCDD2)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val contentColor = when (med.trackingState) {
                "TAKEN" -> Color(0xFF2E7D32)
                "SNOOZED", "MISSED" -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardColor,
                    contentColor = contentColor
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (med.type == "Insulina") "💉 " else "💊 ",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = if (med.type == "Insulina") "${med.dose} unidades" else med.dose,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Todos los días a las ${String.format("%02d:%02d", med.hour, med.minute)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Hasta: ${formatTimestamp(med.endDate, "dd/MM/yyyy")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tipo: ${med.type}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Row {
                            IconButton(onClick = {
                                medicationToEdit = med
                                showEditDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(onClick = {
                                scheduler.cancel(med)
                                scope.launch {
                                    localStorageService.deleteMedication(med)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    localStorageService.logMedicationAction(
                                        medicationId = med.id,
                                        status = "TAKEN",
                                        dose = med.dose
                                    )
                                }
                                scheduler.cancel(med)
                            },
                            enabled = med.trackingState != "TAKEN",
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (med.type == "Insulina") "Apliqué"
                                else "Tomé"
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    localStorageService.updateMedicationState(med.id, "SNOOZED")
                                }
                                scheduler.snooze(med, 10)
                            },
                            enabled = med.trackingState != "TAKEN",
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retrasar")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Estado hoy: ${
                            when (med.trackingState) {
                                "TAKEN" -> if (med.type == "Insulina") "✅ APLICADA" else "✅ TOMADA"
                                "SNOOZED" -> "⏰ RETRASADA"
                                "MISSED" -> if (med.type == "Insulina") "❌ NO APLICADA" else "❌ NO TOMADA"
                                "PENDING" -> "⏳ PENDIENTE"
                                else -> med.trackingState
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { showTimePicker = false },
            timeState = timeState
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    selectedEndDate = dateState.selectedDateMillis?.minus(java.util.TimeZone.getDefault().rawOffset)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showEditDialog && medicationToEdit != null) {
        EditMedicationDialog(
            medication = medicationToEdit!!,
            onDismiss = {
                showEditDialog = false
                medicationToEdit = null
            },
            onSave = { updatedMed ->
                scope.launch {
                    localStorageService.saveMedication(updatedMed)
                }
                scheduler.cancel(medicationToEdit!!)
                scheduler.schedule(updatedMed)
                showEditDialog = false
                medicationToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicationDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onSave: (Medication) -> Unit
) {
    var medName by rememberSaveable { mutableStateOf(medication.name) }
    var medDose by rememberSaveable { mutableStateOf(medication.dose) }
    var medType by rememberSaveable { mutableStateOf(medication.type ?: "Medicamento") }
    var selectedEndDate by rememberSaveable { mutableStateOf(medication.endDate) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val timeState = rememberTimePickerState(
        initialHour = medication.hour,
        initialMinute = medication.minute
    )

    val todayUTC = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val dateSelectionRule = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return utcTimeMillis >= todayUTC
        }
    }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = medication.endDate,
        selectableDates = dateSelectionRule
    )

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ${if (medType == "Insulina") "Insulina" else "Medicamento"}") },
        text = {
            Column {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tipo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val chipColors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = Color(0xFF2196F3),
                            selectedLabelColor = Color.White
                        )
                        FilterChip(
                            selected = medType == "Medicamento",
                            onClick = { medType = "Medicamento" },
                            label = { Text("💊 Medicamento") },
                            colors = chipColors
                        )
                        FilterChip(
                            selected = medType == "Insulina",
                            onClick = { medType = "Insulina" },
                            label = { Text("💉 Insulina") },
                            colors = chipColors
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (medType != "Insulina") {
                    OutlinedTextField(
                        value = medName,
                        onValueChange = { medName = it },
                        label = { Text("Nombre del medicamento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = medDose,
                    onValueChange = { medDose = it },
                    label = {
                        Text(
                            if (medType == "Insulina") "Unidades de insulina"
                            else "Dosis"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Hora")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(String.format("%02d:%02d", timeState.hour, timeState.minute))
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF48FB1),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Fecha fin")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(formatTimestamp(selectedEndDate, "dd/MM/yy"))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val isInsulin = (medType == "Insulina")
                    val nameToSave = if (isInsulin) "Insulina" else medName
                    val isValid = medDose.isNotBlank() && (isInsulin || medName.isNotBlank())

                    if (isValid) {
                        val updatedMedication = medication.copy(
                            name = nameToSave,
                            dose = medDose,
                            hour = timeState.hour,
                            minute = timeState.minute,
                            endDate = selectedEndDate,
                            type = medType
                        )
                        onSave(updatedMedication)
                    } else {
                        Toast.makeText(context, "Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { showTimePicker = false },
            timeState = timeState
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    selectedEndDate = dateState.selectedDateMillis?.minus(java.util.TimeZone.getDefault().rawOffset) ?: selectedEndDate
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    timeState: TimePickerState
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Hora") },
        text = {
            TimePicker(state = timeState, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun formatTimestamp(timestamp: Long, pattern: String): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(date)
}