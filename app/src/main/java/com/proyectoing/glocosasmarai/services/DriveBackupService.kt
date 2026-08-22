package com.proyectoing.glocosasmarai.services

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.proyectoing.glocosasmarai.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import com.proyectoing.glocosasmarai.models.GlucoseEntry
import com.proyectoing.glocosasmarai.models.FoodEntry
import com.proyectoing.glocosasmarai.models.Medication
import com.proyectoing.glocosasmarai.models.EmergencyContact
import com.proyectoing.glocosasmarai.models.UserProfile

@Serializable
data class BackupData(
    val glucoseEntries: List<GlucoseEntry> = emptyList(),
    val foodEntries: List<FoodEntry> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val userProfile: UserProfile? = null,
    val backupDate: Long = System.currentTimeMillis()
)

class DriveBackupService(private val context: Context) {

    private val BACKUP_FILE_NAME = "glocosasmart_backup.json"
    private val json = Json { ignoreUnknownKeys = true }

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("GlocosaSmart").build()
    }

    // Subir backup al Drive
    suspend fun uploadBackup(data: BackupData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService() ?: return@withContext Result.failure(
                Exception("No hay sesión de Google activa")
            )

            val jsonContent = json.encodeToString(data)
            val contentStream = ByteArrayContent.fromString("application/json", jsonContent)

            // Buscar si ya existe un backup previo
            val existingFileId = findBackupFileId(drive)

            if (existingFileId != null) {
                // Actualizar el existente
                drive.files().update(existingFileId, null, contentStream).execute()
            } else {
                // Crear nuevo en appDataFolder
                val fileMetadata = File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                drive.files().create(fileMetadata, contentStream)
                    .setFields("id").execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Descargar backup del Drive
    suspend fun downloadBackup(): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService() ?: return@withContext Result.failure(
                Exception("No hay sesión de Google activa")
            )

            val fileId = findBackupFileId(drive) ?: return@withContext Result.failure(
                Exception("No se encontró ningún backup")
            )

            val content = drive.files().get(fileId)
                .executeMediaAsInputStream()
                .bufferedReader()
                .readText()

            Result.success(json.decodeFromString(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findBackupFileId(drive: Drive): String? {
        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id)")
            .execute()
        return result.files?.firstOrNull()?.id
    }
}