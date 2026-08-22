package com.proyectoing.glocosasmarai.services

import android.content.Context
import com.proyectoing.glocosasmarai.models.*
import com.proyectoing.glocosasmarai.repository.DataRepository
import com.proyectoing.glocosasmarai.database.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Servicio de almacenamiento local que maneja todas las operaciones de datos
 */
class LocalStorageService(private val context: Context) {
    
    private val dataRepository = DataRepository(context)
    
    // ==================== GLUCOSE ENTRIES ====================
    
    fun getAllGlucoseEntries(): Flow<List<GlucoseEntry>> {
        return dataRepository.getAllGlucoseEntries()
    }
    
    suspend fun saveGlucoseEntry(glucoseEntry: GlucoseEntry): Long {
        return dataRepository.insertGlucoseEntry(glucoseEntry)
    }
    
    suspend fun updateGlucoseEntry(glucoseEntry: GlucoseEntry) {
        dataRepository.updateGlucoseEntry(glucoseEntry)
    }
    
    suspend fun deleteGlucoseEntry(glucoseEntry: GlucoseEntry) {
        dataRepository.deleteGlucoseEntry(glucoseEntry)
    }
    
    suspend fun getLatestGlucoseEntry(): GlucoseEntry? {
        return dataRepository.getLatestGlucoseEntry()
    }
    
    suspend fun getAverageGlucoseInRange(startTime: Long, endTime: Long): Double? {
        return dataRepository.getAverageGlucoseInRange(startTime, endTime)
    }
    
    // ==================== FOOD ENTRIES ====================
    
    fun getAllFoodEntries(): Flow<List<FoodEntry>> {
        return dataRepository.getAllFoodEntries()
    }
    
    suspend fun saveFoodEntry(foodEntry: FoodEntry): Long {
        return dataRepository.insertFoodEntry(foodEntry)
    }
    
    suspend fun updateFoodEntry(foodEntry: FoodEntry) {
        dataRepository.updateFoodEntry(foodEntry)
    }
    
    suspend fun deleteFoodEntry(foodEntry: FoodEntry) {
        dataRepository.deleteFoodEntry(foodEntry)
    }
    
    suspend fun getLatestFoodEntry(): FoodEntry? {
        return dataRepository.getLatestFoodEntry()
    }
    
    // ==================== EMERGENCY CONTACTS ====================
    
    fun getAllEmergencyContacts(): Flow<List<EmergencyContact>> {
        return dataRepository.getAllEmergencyContacts()
    }
    
    suspend fun saveEmergencyContact(emergencyContact: EmergencyContact): Long {
        return dataRepository.insertEmergencyContact(emergencyContact)
    }
    
    suspend fun updateEmergencyContact(emergencyContact: EmergencyContact) {
        dataRepository.updateEmergencyContact(emergencyContact)
    }
    
    suspend fun deleteEmergencyContact(emergencyContact: EmergencyContact) {
        dataRepository.deleteEmergencyContact(emergencyContact)
    }
    
    suspend fun getPrimaryEmergencyContact(): EmergencyContact? {
        return dataRepository.getPrimaryEmergencyContact()
    }
    
    // ==================== CHAT MESSAGES ====================
    
    fun getAllChatMessages(): Flow<List<ChatMessage>> {
        return dataRepository.getAllChatMessages()
    }
    
    fun getChatMessagesByConversation(conversationId: String): Flow<List<ChatMessage>> {
        return dataRepository.getChatMessagesByConversation(conversationId)
    }

    suspend fun saveChatMessage(chatMessage: ChatMessage): Long { // <-- conversationId ELIMINADO
        return dataRepository.insertChatMessage(chatMessage)
    }
    suspend fun saveChatMessages(chatMessages: List<ChatMessage>) { // <-- conversationId ELIMINADO
        dataRepository.insertChatMessages(chatMessages)
    }
    
    suspend fun deleteChatMessagesByConversation(conversationId: String) {
        dataRepository.deleteChatMessagesByConversation(conversationId)
    }
    
    // ==================== CONVERSATIONS ====================
    
    fun getAllConversations(): Flow<List<SavedConversation>> {
        return dataRepository.getAllConversations()
    }
    
    suspend fun saveConversation(conversation: SavedConversation) {
        dataRepository.insertConversation(conversation)
    }
    
    suspend fun updateConversation(conversation: SavedConversation) {
        dataRepository.updateConversation(conversation)
    }
    
    suspend fun deleteConversation(conversation: SavedConversation) {
        dataRepository.deleteConversation(conversation)
    }
    // ==================== MEDICATIONS ====================

    /**
     * Obtiene todos los recordatorios de medicamentos desde el repositorio
     */
    fun getAllMedications(): Flow<List<Medication>> {
        return dataRepository.getAllMedications()
    }

    /**
     * Guarda un solo recordatorio de medicamento en el repositorio
     * (ESTA ES LA FUNCIÓN QUE TE FALTA Y CAUSA EL ERROR)
     */
    suspend fun saveMedication(medication: Medication): Long {
        return dataRepository.insertMedication(medication)
    }

    /**
     * Borra un solo recordatorio de medicamento del repositorio
     */
    suspend fun deleteMedication(medication: Medication) {
        dataRepository.deleteMedication(medication)
    }

    /**
     * Borra TODOS los recordatorios de medicamentos
     */
    suspend fun deleteAllMedications() {
        dataRepository.deleteAllMedications()
    }

    suspend fun updateMedicationState(id: Long, newState: String) { // <-- 1. Solo 2 parámetros
        try {
            println("DEBUG: Updating medication $id to state $newState")
            dataRepository.updateMedicationState(id, newState) // <-- 2. Llama con 2 parámetros
            println("DEBUG: Update successful")
        } catch (e: Exception) {
            println("DEBUG: Error updating medication: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun logMedicationAction(medicationId: Long, status: String, dose: String) {
        // 1. Crear el objeto de historial
        val historyEntry = com.proyectoing.glocosasmarai.database.entities.MedicationHistoryEntity(
            medicationId = medicationId,
            timestamp = System.currentTimeMillis(),
            status = status,
            doseTaken = dose
        )

        // 2. Obtener la base de datos usando el context (que ahora sí funciona)
        val db = com.proyectoing.glocosasmarai.database.AppDatabase.getDatabase(context)

        // 3. Guardar en la tabla de historial
        db.medicationHistoryDao().insertLog(historyEntry)

        // 4. Actualizar el estado visual (esto ya lo tenías)
        updateMedicationState(medicationId, status)
    }

    fun getMissedMedicationsReport(month: Int, year: Int): Flow<List<MissedMedicationSummary>> {
        val calendar = java.util.Calendar.getInstance()

        // Configurar inicio del mes
        calendar.set(year, month, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis

        // Configurar fin del mes
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        val endDate = calendar.timeInMillis

        // Llamar a la base de datos
        val db = com.proyectoing.glocosasmarai.database.AppDatabase.getDatabase(context)
        return db.medicationHistoryDao().getMissedMedicationsInDateRange(startDate, endDate)
    }
    // ==================== USER PROFILE ====================
    
    fun getCurrentUserProfile(): Flow<UserProfile?> {
        return dataRepository.getCurrentUserProfile()
    }
    
    suspend fun saveUserProfile(userProfile: UserProfile) {
        dataRepository.insertUserProfile(userProfile)
    }
    
    suspend fun updateUserProfile(userProfile: UserProfile) {
        dataRepository.updateUserProfile(userProfile)
    }
    
    // ==================== APP SETTINGS ====================
    
    fun getAllSettings(): Flow<Map<String, String>> {
        return dataRepository.getAllSettings()
    }
    
    suspend fun getSetting(key: String): String? {
        return dataRepository.getSetting(key)
    }
    
    suspend fun setSetting(key: String, value: String, type: String = "string", description: String? = null) {
        dataRepository.setSetting(key, value, type, description)
    }
    
    suspend fun deleteSetting(key: String) {
        dataRepository.deleteSetting(key)
    }
    // ==================== HORARIOS DE COMIDA (NUEVO) ====================

    /**
     * Guarda la hora de una comida específica.
     * @param mealType: "breakfast", "lunch", o "dinner"
     * @param time: La hora en formato "HH:mm"
     */
    suspend fun saveMealTime(mealType: String, time: String) {
        // Usamos la tabla de Settings existente
        setSetting("${mealType}_time", time, "time", "Horario de $mealType")
    }

    /**
     * Obtiene la hora guardada o devuelve un valor por defecto si no existe.
     */
    suspend fun getMealTime(mealType: String): String {
        // Valores por defecto: Desayuno 08:00, Comida 14:00, Cena 20:00
        val defaultTime = when(mealType) {
            "breakfast" -> "08:00"
            "lunch" -> "14:00"
            "dinner" -> "20:00"
            else -> "09:00"
        }
        return getSetting("${mealType}_time") ?: defaultTime
    }
    
    // ==================== UTILITY METHODS ====================
    
    suspend fun clearAllData() {
        dataRepository.clearAllData()
    }
    
    suspend fun exportAllData(): Map<String, Any?> {
        return dataRepository.exportAllData()
    }
    
    /**
     * Inicializa configuraciones por defecto
     */
    suspend fun initializeDefaultSettings() {
        val defaultSettings = mapOf(
            "theme" to "system",
            "language" to "es",
            "notifications_enabled" to "true",
            "glucose_reminders" to "true",
            "medication_reminders" to "true",
            "backup_enabled" to "true",
            "auto_sync" to "false",
            "data_retention_days" to "365"
        )
        
        defaultSettings.forEach { (key, value) ->
            if (getSetting(key) == null) {
                setSetting(key, value)
            }
        }
    }
    
    /**
     * Obtiene estadísticas del usuario
     */
    suspend fun getUserStatistics(): UserStatistics {
        val glucoseEntries = getAllGlucoseEntries().first()
        val foodEntries = getAllFoodEntries().first()
        val conversations = getAllConversations().first()
        val userProfile = getCurrentUserProfile().first()
        
        val currentTime = System.currentTimeMillis()
        val oneWeekAgo = currentTime - (7 * 24 * 60 * 60 * 1000L)
        
        val recentGlucoseEntries = glucoseEntries.filter { it.timestamp >= oneWeekAgo }
        val recentFoodEntries = foodEntries.filter { it.timestamp >= oneWeekAgo }
        
        val averageGlucose = if (recentGlucoseEntries.isNotEmpty()) {
            recentGlucoseEntries.map { it.value }.average()
        } else null
        
        val lowGlucoseCount = recentGlucoseEntries.count { it.value < 70 }
        val highGlucoseCount = recentGlucoseEntries.count { it.value > 180 }
        
        return UserStatistics(
            totalGlucoseEntries = glucoseEntries.size,
            totalFoodEntries = foodEntries.size,
            totalConversations = conversations.size,
            recentGlucoseEntries = recentGlucoseEntries.size,
            recentFoodEntries = recentFoodEntries.size,
            averageGlucose = averageGlucose,
            lowGlucoseCount = lowGlucoseCount,
            highGlucoseCount = highGlucoseCount,
            userProfileComplete = userProfile?.isComplete() ?: false,
            daysSinceFirstEntry = if (glucoseEntries.isNotEmpty()) {
                val firstEntry = glucoseEntries.minByOrNull { it.timestamp }
                if (firstEntry != null) {
                    (currentTime - firstEntry.timestamp) / (24 * 60 * 60 * 1000L)
                } else 0L
            } else 0L
        )
    }
}

/**
 * Estadísticas del usuario
 */
data class UserStatistics(
    val totalGlucoseEntries: Int,
    val totalFoodEntries: Int,
    val totalConversations: Int,
    val recentGlucoseEntries: Int,
    val recentFoodEntries: Int,
    val averageGlucose: Double?,
    val lowGlucoseCount: Int,
    val highGlucoseCount: Int,
    val userProfileComplete: Boolean,
    val daysSinceFirstEntry: Long
)
