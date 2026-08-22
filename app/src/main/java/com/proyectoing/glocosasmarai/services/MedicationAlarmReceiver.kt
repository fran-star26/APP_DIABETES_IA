package com.proyectoing.glocosasmarai.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.proyectoing.glocosasmarai.R
import com.proyectoing.glocosasmarai.models.Medication
import java.util.Calendar

class MedicationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_MEDICATION_NOTIFICATION = "ACTION_SHOW_MEDICATION_NOTIFICATION"
        const val ACTION_SHOW_MISSED_REMINDER = "ACTION_SHOW_MISSED_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Extraer datos del Intent
        val medId = intent.getLongExtra("EXTRA_MED_ID", 0L)
        val medName = intent.getStringExtra("EXTRA_MED_NAME") ?: "Medicamento"
        val medDose = intent.getStringExtra("EXTRA_MED_DOSE") ?: ""
        val endDate = intent.getLongExtra("EXTRA_END_DATE", 0L)
        val medHour = intent.getIntExtra("EXTRA_MED_HOUR", 12)
        val medMinute = intent.getIntExtra("EXTRA_MED_MINUTE", 0)
        val medType = intent.getStringExtra("EXTRA_MED_TYPE") ?: "Medicamento"

        val pendingResult = goAsync()
        val localStorageService = LocalStorageService(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                // 2. Verificar si el tratamiento ya terminó
                val today = Calendar.getInstance()
                val endCalendar = Calendar.getInstance().apply { timeInMillis = endDate }
                // Normalizamos a medianoche para la comparación
                today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0)
                endCalendar.set(Calendar.HOUR_OF_DAY, 0); endCalendar.set(Calendar.MINUTE, 0); endCalendar.set(Calendar.SECOND, 0)

                if (today.after(endCalendar)) {
                    return@launch
                }

                // 3. Crear objeto temporal para la notificación
                val medication = Medication(medId, medName, medDose, medHour, medMinute, endDate, "PENDING", medType)

                // 4. Lógica de acciones: SOLO mostrar y marcar como perdido
                // Se eliminó scheduleMissedReminder para evitar la duplicidad inmediata
                when (intent.action) {
                    ACTION_SHOW_MEDICATION_NOTIFICATION, ACTION_SHOW_MISSED_REMINDER -> {
                        localStorageService.updateMedicationState(medication.id, "MISSED")
                        showNotification(context, medication)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, medication: Medication) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_alerts"
        val notificationId = medication.id.toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Recordatorios de Medicamentos", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Configuración de textos según tipo (Insulina vs Pastillas)
        val isInsulin = medication.type == "Insulina"
        val contentTitle = if (isInsulin) "Recordatorio de Insulina" else "Recordatorio de Medicamento"
        val contentText = if (isInsulin) "¿Te aplicaste tu insulina (${medication.dose})?" else "¿Tomaste tu ${medication.name} (${medication.dose})?"
        val actionText = if (isInsulin) "Apliqué" else "Tomé"

        // Acción: TOMÉ / APLIQUÉ
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKEN
            putExtra(NotificationActionReceiver.EXTRA_MED_ID, medication.id)
            putExtra(NotificationActionReceiver.EXTRA_MED_NAME, medication.name)
            putExtra(NotificationActionReceiver.EXTRA_MED_DOSE, medication.dose)
            putExtra(NotificationActionReceiver.EXTRA_END_DATE, medication.endDate)
            putExtra(NotificationActionReceiver.EXTRA_MED_HOUR, medication.hour)
            putExtra(NotificationActionReceiver.EXTRA_MED_MINUTE, medication.minute)
            putExtra(NotificationActionReceiver.EXTRA_MED_TYPE, medication.type)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context, notificationId, takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción: RETRASAR (Snooze)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_MED_ID, medication.id)
            putExtra(NotificationActionReceiver.EXTRA_MED_NAME, medication.name)
            putExtra(NotificationActionReceiver.EXTRA_MED_DOSE, medication.dose)
            putExtra(NotificationActionReceiver.EXTRA_END_DATE, medication.endDate)
            putExtra(NotificationActionReceiver.EXTRA_MED_HOUR, medication.hour)
            putExtra(NotificationActionReceiver.EXTRA_MED_MINUTE, medication.minute)
            putExtra(NotificationActionReceiver.EXTRA_MED_TYPE, medication.type)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(0, actionText, takenPendingIntent)
            .addAction(0, "Retrasar 10 min", snoozePendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}