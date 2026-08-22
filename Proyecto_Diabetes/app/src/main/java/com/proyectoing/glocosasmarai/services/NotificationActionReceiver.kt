package com.proyectoing.glocosasmarai.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import com.proyectoing.glocosasmarai.models.Medication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.proyectoing.glocosasmarai.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.proyectoing.glocosasmarai.ACTION_SNOOZE"

        const val EXTRA_MED_ID = "EXTRA_MED_ID"
        const val EXTRA_MED_NAME = "EXTRA_MED_NAME"
        const val EXTRA_MED_DOSE = "EXTRA_MED_DOSE"
        const val EXTRA_END_DATE = "EXTRA_END_DATE"
        const val EXTRA_MED_HOUR = "EXTRA_MED_HOUR"
        const val EXTRA_MED_MINUTE = "EXTRA_MED_MINUTE"
        const val EXTRA_MED_TYPE = "EXTRA_MED_TYPE"

        const val SNOOZE_MINUTES = 10
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
        val notificationId = medId.toInt()

        // 1. Cerramos la notificación, sin importar la acción
        notificationManager.cancel(notificationId)

        val pendingResult = goAsync()
        val localStorageService = LocalStorageService(context)
        val scope = CoroutineScope(Dispatchers.IO)
        val scheduler = MedicationAlarmScheduler(context)

        scope.launch {
            try {
                // Reconstruimos el objeto solo para el scheduler
                val medication = Medication(
                    id = medId,
                    name = intent.getStringExtra(EXTRA_MED_NAME) ?: "",
                    dose = intent.getStringExtra(EXTRA_MED_DOSE) ?: "",
                    hour = intent.getIntExtra(EXTRA_MED_HOUR, 0),
                    minute = intent.getIntExtra(EXTRA_MED_MINUTE, 0),
                    endDate = intent.getLongExtra(EXTRA_END_DATE, 0L),
                    type = intent.getStringExtra(EXTRA_MED_TYPE) ?: "Medicamento"
                    // No pasamos timesTaken
                )

                when (intent.action) {
                    ACTION_TAKEN -> {
                        localStorageService.updateMedicationState(medId, "TAKEN") // <-- Lógica simple
                        scheduler.cancelMissedLoop(context, medication) // Cancela el bucle (esto está bien)
                    }
                    ACTION_SNOOZE -> {
                        localStorageService.updateMedicationState(medId, "SNOOZED") // <-- Lógica simple
                        scheduler.snooze(medication, SNOOZE_MINUTES) // Reprograma (esto está bien)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}