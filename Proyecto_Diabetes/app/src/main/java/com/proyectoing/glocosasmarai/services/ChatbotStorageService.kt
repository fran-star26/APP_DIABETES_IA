package com.proyectoing.glocosasmarai.services

import android.content.Context
import android.os.Environment
import com.proyectoing.glocosasmarai.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Servicio para almacenar y recuperar conversaciones del chatbot en formato JSON
 */
class ChatbotStorageService(private val context: Context) {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    /**
     * Directorio donde se almacenan los archivos JSON del chatbot
     */
    private val chatbotStorageDir: File by lazy {
        val externalDir = File(context.getExternalFilesDir(null), "chatbot_conversations")
        if (!externalDir.exists()) {
            externalDir.mkdirs()
        }
        externalDir
    }
    
    /**
     * Guarda una conversación comprimida a JSON de entrada
     */
    fun saveInputJson(
        userId: String,
        userMessage: String,
        chatMessages: List<ChatMessage>,
        glucoseLevel: Int? = null,
        lastMeal: String? = null,
        medicationTaken: Boolean? = null
    ): File {
        val chatbotService = ChatbotJsonService()
        val inputJson = chatbotService.compressToInputJson(
            userId = userId,
            userMessage = userMessage,
            chatMessages = chatMessages,
            glucoseLevel = glucoseLevel,
            lastMeal = lastMeal,
            medicationTaken = medicationTaken
        )
        
        val timestamp = dateFormat.format(Date())
        val fileName = "input_${userId}_${timestamp}.json"
        val file = File(chatbotStorageDir, fileName)
        
        file.writeText(inputJson)
        return file
    }
    
    /**
     * Guarda una respuesta del chatbot en formato JSON
     */
    fun saveOutputJson(
        userId: String,
        chatbotOutput: ChatbotOutput
    ): File {
        val outputJson = json.encodeToString(chatbotOutput)
        
        val timestamp = dateFormat.format(Date())
        val fileName = "output_${userId}_${timestamp}.json"
        val file = File(chatbotStorageDir, fileName)
        
        file.writeText(outputJson)
        return file
    }
    
    /**
     * Guarda una conversación completa (entrada y salida)
     */
    fun saveCompleteConversation(
        userId: String,
        userMessage: String,
        chatMessages: List<ChatMessage>,
        chatbotOutput: ChatbotOutput,
        glucoseLevel: Int? = null,
        lastMeal: String? = null,
        medicationTaken: Boolean? = null
    ): Pair<File, File> {
        val inputFile = saveInputJson(
            userId = userId,
            userMessage = userMessage,
            chatMessages = chatMessages,
            glucoseLevel = glucoseLevel,
            lastMeal = lastMeal,
            medicationTaken = medicationTaken
        )
        
        val outputFile = saveOutputJson(
            userId = userId,
            chatbotOutput = chatbotOutput
        )
        
        return Pair(inputFile, outputFile)
    }
    
    /**
     * Carga un archivo JSON de entrada
     */
    fun loadInputJson(file: File): ChatbotInput? {
        return try {
            val jsonString = file.readText()
            json.decodeFromString<ChatbotInput>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Carga un archivo JSON de salida
     */
    fun loadOutputJson(file: File): ChatbotOutput? {
        return try {
            val jsonString = file.readText()
            json.decodeFromString<ChatbotOutput>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Obtiene todos los archivos JSON de entrada
     */
    fun getAllInputJsonFiles(): List<File> {
        return chatbotStorageDir.listFiles { file ->
            file.name.startsWith("input_") && file.name.endsWith(".json")
        }?.toList() ?: emptyList()
    }
    
    /**
     * Obtiene todos los archivos JSON de salida
     */
    fun getAllOutputJsonFiles(): List<File> {
        return chatbotStorageDir.listFiles { file ->
            file.name.startsWith("output_") && file.name.endsWith(".json")
        }?.toList() ?: emptyList()
    }
    
    /**
     * Obtiene archivos JSON por usuario
     */
    fun getJsonFilesByUser(userId: String): List<File> {
        return chatbotStorageDir.listFiles { file ->
            file.name.contains("_${userId}_") && file.name.endsWith(".json")
        }?.toList() ?: emptyList()
    }
    
    /**
     * Elimina un archivo JSON
     */
    fun deleteJsonFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Elimina todos los archivos JSON del chatbot
     */
    fun clearAllJsonFiles(): Boolean {
        return try {
            chatbotStorageDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".json")) {
                    file.delete()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Obtiene el tamaño total de almacenamiento usado
     */
    fun getStorageSize(): Long {
        return try {
            chatbotStorageDir.listFiles()?.sumOf { file ->
                if (file.name.endsWith(".json")) file.length() else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Obtiene el número total de archivos JSON
     */
    fun getJsonFileCount(): Int {
        return try {
            chatbotStorageDir.listFiles()?.count { file ->
                file.name.endsWith(".json")
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Obtiene la ruta del directorio de almacenamiento
     */
    fun getStoragePath(): String {
        return chatbotStorageDir.absolutePath
    }
}
