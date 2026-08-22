package com.proyectoing.glocosasmarai.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.proyectoing.glocosasmarai.models.Medication
import java.util.Calendar

class MedicationAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Programa la alarma principal para la hora del medicamento.
     */
    fun schedule(med: Medication) {
        val intent = createBaseIntent(context, med).apply {
            action = MedicationAlarmReceiver.ACTION_SHOW_MEDICATION_NOTIFICATION
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            med.id.toInt(), // ID Principal
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, med.hour)
            set(Calendar.MINUTE, med.minute)
            set(Calendar.SECOND, 0)
            // Si la hora ya pasó hoy, programarla para mañana
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    /**
     * Cancela TODAS las alarmas asociadas a este medicamento (principal Y bucle).
     * Esta es la función que se debe llamar al ELIMINAR un recordatorio.
     */
    fun cancel(med: Medication) {
        // 1. Cancelar la alarma principal (ID = med.id)
        val mainIntent = createBaseIntent(context, med).apply {
            action = MedicationAlarmReceiver.ACTION_SHOW_MEDICATION_NOTIFICATION
        }
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            med.id.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(mainPendingIntent)

        // 2. Cancelar la alarma del bucle de 10 min (ID = med.id + 1000)
        cancelMissedLoop(context, med)
    }

    /**
     * Programa un recordatorio de 10 minutos (Snooze).
     * Se une al bucle de "MISSED".
     */
    fun snooze(medication: Medication, delayMinutes: Int) {
        val snoozeTimeMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000)

        // Usa la acción MISSED para que entre en el bucle de 10 min
        val intent = createBaseIntent(context, medication).apply {
            action = MedicationAlarmReceiver.ACTION_SHOW_MISSED_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id.toInt() + 1000, // ID del bucle "missed"
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            snoozeTimeMillis,
            pendingIntent
        )
    }

    /**
     * Función centralizada para cancelar SÓLO el bucle de 10 minutos.
     * Esta es la que debe llamar el botón "Tomé".
     */
    fun cancelMissedLoop(context: Context, med: Medication) {
        val missedIntent = createBaseIntent(context, med).apply {
            action = MedicationAlarmReceiver.ACTION_SHOW_MISSED_REMINDER
        }
        val missedPendingIntent = PendingIntent.getBroadcast(
            context,
            med.id.toInt() + 1000, // ID del bucle "missed"
            missedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(missedPendingIntent)
    }

    /**
     * Crea un Intent base IDÉNTICO con todos los extras necesarios.
     * Esto es crucial para que la cancelación funcione.
     */
    private fun createBaseIntent(context: Context, med: Medication): Intent {
        return Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra("EXTRA_MED_ID", med.id)
            putExtra("EXTRA_MED_NAME", med.name)
            putExtra("EXTRA_MED_DOSE", med.dose)
            putExtra("EXTRA_END_DATE", med.endDate)
            putExtra("EXTRA_MED_HOUR", med.hour)
            putExtra("EXTRA_MED_MINUTE", med.minute)
            putExtra("EXTRA_MED_TYPE", med.type)
        }
    }
}