package com.proyectoing.glocosasmarai.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {

            // Necesitamos usar Coroutines para acceder a LocalStorage
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val localStorage = LocalStorageService(context)
                val scheduler = MedicationAlarmScheduler(context)

                // Obtenemos todas las medicinas guardadas
                val medications = localStorage.getAllMedications().first()

                // Las volvemos a programar todas
                medications.forEach { med ->
                    scheduler.schedule(med)
                }
            }
        }
    }
}