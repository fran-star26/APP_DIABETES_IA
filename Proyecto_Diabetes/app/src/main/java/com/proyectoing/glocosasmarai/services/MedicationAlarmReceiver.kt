package com.proyectoing.glocosasmarai.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.PendingIntent
import android.app.AlarmManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import com.proyectoing.glocosasmarai.R
import com.proyectoing.glocosasmarai.models.Medication // ¡Importante!
import java.util.Calendar

class MedicationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_MEDICATION_NOTIFICATION = "ACTION_SHOW_MEDICATION_NOTIFICATION"
        const val ACTION_SHOW_MISSED_REMINDER = "ACTION_SHOW_MISSED_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Leer TODOS los datos
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
                // 2. Comprobar fecha de finalización (sin cambios)
                val today = Calendar.getInstance()
                val endCalendar = Calendar.getInstance().apply { timeInMillis = endDate }
                today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0)

                if (today.after(endCalendar)) {
                    return@launch // El tratamiento terminó
                }

                // 3. Reconstruir el objeto (¡Volvemos a esto!)
                val medication = Medication(medId, medName, medDose, medHour, medMinute, endDate, "PENDING", medType)

                when (intent.action) {
                    ACTION_SHOW_MEDICATION_NOTIFICATION -> {
                        // ... (lógica de reprogramación, sin cambios) ...

                        // CORRECCIÓN: Volvemos a la lógica simple
                        localStorageService.updateMedicationState(medication.id, "MISSED")

                        // ... (El resto de la lógica: scheduleMissedReminder, showNotification) ...
                    }
                    ACTION_SHOW_MISSED_REMINDER -> {
                        // ... (lógica de mostrar notificación y reprogramar, sin cambios) ...
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun scheduleMissedReminder(context: Context, med: Medication) {
        // Usamos el scheduler para mantener la lógica centralizada
        // (Aunque también podríamos hacerlo manual)
        val scheduler = MedicationAlarmScheduler(context)
        scheduler.snooze(med, 10) // "snooze" ahora programa la alarma "missed"
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

        // --- LÓGICA DEL TEXTO DINÁMICO ---
        val contentTitle: String
        val contentText: String
        val actionText: String

        if (medication.type == "Insulina") {
            contentTitle = "Recordatorio de Insulina"
            contentText = "¿Te aplicaste tu insulina (${medication.dose})?"
            actionText = "Apliqué"
        } else {
            contentTitle = "Recordatorio de Medicamento"
            contentText = "¿Tomaste tu ${medication.name} (${medication.dose})?"
            actionText = "Tomé"
        }

        // --- ACCIÓN "TOMÉ" (CORREGIDA) ---
        // Ahora debe pasar TODOS los extras para que el ActionReceiver pueda cancelar el bucle
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

        // --- ACCIÓN "RETRASAR" (CORREGIDA) ---
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

        // 4. Construir la notificación
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // <-- Cambia esto por tu ícono
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, actionText, takenPendingIntent)
            .addAction(0, "Retrasar 10 min", snoozePendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}