package com.proyectoing.glocosasmarai.workers // Ajusta el paquete si es necesario

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proyectoing.glocosasmarai.R
import com.proyectoing.glocosasmarai.services.LocalStorageService
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class MealReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val mealType = inputData.getString("MEAL_TYPE") ?: return Result.failure()
        val mealName = inputData.getString("MEAL_NAME") ?: "Comida"

        // 1. Revisar la base de datos
        val context = applicationContext
        val localStorageService = LocalStorageService(context)

        // Obtenemos los registros de hoy
        val today = LocalDate.now()
        val allFoodEntries = localStorageService.getAllFoodEntries().first()

        val hasEaten = allFoodEntries.any { entry ->
            val entryDate = java.time.Instant.ofEpochMilli(entry.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            // Coincide la fecha de hoy Y el tipo de comida (Desayuno, Comida, Cena)
            entryDate == today && entry.type.equals(mealName, ignoreCase = true)
        }

        // 2. Si NO ha comido, enviamos la notificación
        if (!hasEaten) {
            sendNotification(context, mealName)
        }

        return Result.success()
    }

    private fun sendNotification(context: Context, mealName: String) {
        val channelId = "meal_reminders_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si es necesario (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Comida",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener un icono válido
            .setContentTitle("¡Hora del $mealName!")
            .setContentText("No has registrado tu $mealName hoy. Es importante para tu control.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(mealName.hashCode(), notification)
    }
}