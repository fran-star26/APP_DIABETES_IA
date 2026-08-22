package com.proyectoing.glocosasmarai.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.proyectoing.glocosasmarai.MainActivity
import com.proyectoing.glocosasmarai.R

class MealAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. Recuperamos el tipo de comida (ej. "Comida")
        val mealType = intent.getStringExtra("MEAL_TYPE") ?: "Registro"

        // 2. Definimos el mensaje exacto que quieres
        val message = "No has registrado tu $mealType el día de hoy"

        // 3. Llamamos a la función para mostrar SOLO UNA notificación
        showNotification(context, mealType, message)
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "meal_alerts"

        // Crear canal (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Comida",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Tu ícono
            .setContentTitle("GlucosaSmart IA") // Título fijo de la app
            .setContentText(message)            // El mensaje: "No has registrado..."
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Para que quepa todo el texto
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // TRUCO: Usamos title.hashCode() como ID.
        // Así, si llega otra notificación de "Comida", REEMPLAZA a la anterior.
        notificationManager.notify(title.hashCode(), notification)
    }
}