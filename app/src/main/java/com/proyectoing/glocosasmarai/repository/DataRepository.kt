package com.proyectoing.glocosasmarai.repository
/**
 * 
 *Este documento es el repositorio de datos central de la aplicación. 
 *Su función es ser la única fuente de verdad para todos los datos que se guardan en la base de datos local, como un controlador de acceso a la base de datos.
 *
 */
import android.content.Context
import com.proyectoing.glocosasmarai.database.AppDatabase
import com.proyectoing.glocosasmarai.database.dao.*
import com.proyectoing.glocosasmarai.database.entities.*
import com.proyectoing.glocosasmarai.models.*
import com.proyectoing.glocosasmarai.database.entities.MedicationEntity
import com.proyectoing.glocosasmarai.models.Medication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first


/**
 * Repositorio principal que maneja todas las operaciones de datos de la aplicación
 */
class DataRepository(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    
    // DAOs
    private val glucoseEntryDao = database.glucoseEntryDao()
    private val foodEntryDao = database.foodEntryDao()
    private val emergencyContactDao = database.emergencyContactDao()
    private val chatMessageDao = database.chatMessageDao()
    private val conversationDao = database.conversationDao()
    private val userProfileDao = database.userProfileDao()
    private val appSettingsDao = database.appSettingsDao()
    private val medicationDao = database.medicationDao()
    
    // ==================== GLUCOSE ENTRIES ====================
    
    fun getAllGlucoseEntries(): Flow<List<GlucoseEntry>> {
        return glucoseEntryDao.getAllGlucoseEntries().map { entities ->
            entities.map { it.toGlucoseEntry() }
        }
    }
    
    suspend fun insertGlucoseEntry(glucoseEntry: GlucoseEntry): Long {
        return glucoseEntryDao.insertGlucoseEntry(glucoseEntry.toEntity())
    }
    
    suspend fun updateGlucoseEntry(glucoseEntry: GlucoseEntry) {
        glucoseEntryDao.updateGlucoseEntry(glucoseEntry.toEntity())
    }
    
    suspend fun deleteGlucoseEntry(glucoseEntry: GlucoseEntry) {
        glucoseEntryDao.deleteGlucoseEntry(glucoseEntry.toEntity())
    }
    
    suspend fun getLatestGlucoseEntry(): GlucoseEntry? {
        return glucoseEntryDao.getLatestGlucoseEntry()?.toGlucoseEntry()
    }
    
    suspend fun getAverageGlucoseInRange(startTime: Long, endTime: Long): Double? {
        return glucoseEntryDao.getAverageGlucoseInRange(startTime, endTime)
    }
    
    // ==================== FOOD ENTRIES ====================
    
    fun getAllFoodEntries(): Flow<List<FoodEntry>> {
        return foodEntryDao.getAllFoodEntries().map { entities ->
            entities.map { it.toFoodEntry() }
        }
    }
    
    suspend fun insertFoodEntry(foodEntry: FoodEntry): Long {
        return foodEntryDao.insertFoodEntry(foodEntry.toEntity())
    }
    
    suspend fun updateFoodEntry(foodEntry: FoodEntry) {
        foodEntryDao.updateFoodEntry(foodEntry.toEntity())
    }
    
    suspend fun deleteFoodEntry(foodEntry: FoodEntry) {
        foodEntryDao.deleteFoodEntry(foodEntry.toEntity())
    }
    
    suspend fun getLatestFoodEntry(): FoodEntry? {
        return foodEntryDao.getLatestFoodEntry()?.toFoodEntry()
    }
    
    // ==================== EMERGENCY CONTACTS ====================
    
    fun getAllEmergencyContacts(): Flow<List<EmergencyContact>> {
        return emergencyContactDao.getAllEmergencyContacts().map { entities ->
            entities.map { it.toEmergencyContact() }
        }
    }
    
    suspend fun insertEmergencyContact(emergencyContact: EmergencyContact): Long {
        return emergencyContactDao.insertEmergencyContact(emergencyContact.toEntity())
    }
    
    suspend fun updateEmergencyContact(emergencyContact: EmergencyContact) {
        emergencyContactDao.updateEmergencyContact(emergencyContact.toEntity())
    }
    
    suspend fun deleteEmergencyContact(emergencyContact: EmergencyContact) {
        emergencyContactDao.deleteEmergencyContact(emergencyContact.toEntity())
    }
    
    suspend fun getPrimaryEmergencyContact(): EmergencyContact? {
        return emergencyContactDao.getPrimaryEmergencyContact()?.toEmergencyContact()
    }
    
    // ==================== CHAT MESSAGES ====================
    
    fun getAllChatMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllChatMessages().map { entities ->
            entities.map { it.toChatMessage() }
        }
    }
    
    fun getChatMessagesByConversation(conversationId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getChatMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toChatMessage() }
        }
    }
    
    suspend fun insertChatMessage(chatMessage: ChatMessage): Long {
        return chatMessageDao.insertChatMessage(chatMessage.toEntity())
    }
    
    suspend fun insertChatMessages(chatMessages: List<ChatMessage>) {
        chatMessageDao.insertChatMessages(chatMessages.map { it.toEntity() })
    }
    
    suspend fun deleteChatMessagesByConversation(conversationId: String) {
        chatMessageDao.deleteChatMessagesByConversation(conversationId)
    }
    
    // ==================== CONVERSATIONS ====================
    
    fun getAllConversations(): Flow<List<SavedConversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { entity ->
                // Obtener mensajes de la conversación
                val messages = chatMessageDao.getChatMessagesByConversationSync(entity.id)
                entity.toSavedConversation(messages.map { it.toChatMessage() })
            }
        }
    }
    
    suspend fun insertConversation(conversation: SavedConversation) {
        conversationDao.insertConversation(conversation.toEntity())
        // Insertar mensajes de la conversación
        insertChatMessages(conversation.messages)
    }
    
    suspend fun updateConversation(conversation: SavedConversation) {
        conversationDao.updateConversation(conversation.toEntity())
        // Actualizar mensajes
        deleteChatMessagesByConversation(conversation.id)
        insertChatMessages(conversation.messages)
    }
    
    suspend fun deleteConversation(conversation: SavedConversation) {
        conversationDao.deleteConversation(conversation.toEntity())
        deleteChatMessagesByConversation(conversation.id)
    }

    // ==================== MEDICATIONS ====================

    fun getAllMedications(): Flow<List<Medication>> {
        return medicationDao.getAllMedications().map { entities ->
            entities.map { it.toMedication() }
        }
    }

    suspend fun insertMedication(medication: Medication): Long {
        return medicationDao.insertMedication(medication.toEntity())
    }

    suspend fun deleteMedication(medication: Medication) {
        medicationDao.deleteMedication(medication.toEntity())
    }

    suspend fun deleteAllMedications() {
        medicationDao.deleteAllMedications()
    }

    suspend fun updateMedicationState(id: Long, newState: String) { // <-- 1. Solo 2 parámetros
        medicationDao.updateState(id, newState) // <-- 2. Llama a la función DAO de 2 parámetros
    }
    // ==================== USER PROFILE ====================
    
    fun getCurrentUserProfile(): Flow<UserProfile?> {
        return userProfileDao.getCurrentUserProfileFlow().map { entity ->
            entity?.toUserProfile()
        }
    }
    
    suspend fun insertUserProfile(userProfile: UserProfile) {
        userProfileDao.insertUserProfile(userProfile.toEntity())
    }
    
    suspend fun updateUserProfile(userProfile: UserProfile) {
        userProfileDao.updateUserProfile(userProfile.toEntity())
    }
    
    // ==================== APP SETTINGS ====================
    
    fun getAllSettings(): Flow<Map<String, String>> {
        return appSettingsDao.getAllSettings().map { entities ->
            entities.associate { it.key to it.value }
        }
    }
    
    suspend fun getSetting(key: String): String? {
        return appSettingsDao.getSettingValue(key)
    }
    
    suspend fun setSetting(key: String, value: String, type: String = "string", description: String? = null) {
        val setting = AppSettingsEntity(
            key = key,
            value = value,
            type = type,
            description = description
        )
        appSettingsDao.insertSetting(setting)
    }
    
    suspend fun deleteSetting(key: String) {
        appSettingsDao.deleteSettingByKey(key)
    }
    
    // ==================== UTILITY METHODS ====================
    
    suspend fun clearAllData() {
        glucoseEntryDao.deleteAllGlucoseEntries()
        foodEntryDao.deleteAllFoodEntries()
        emergencyContactDao.deleteAllEmergencyContacts()
        chatMessageDao.deleteAllChatMessages()
        conversationDao.deleteAllConversations()
        userProfileDao.deleteAllUserProfiles()
        appSettingsDao.deleteAllSettings()
    }
    
    suspend fun exportAllData(): Map<String, Any?> {
        val glucose = glucoseEntryDao.getAllGlucoseEntries().first().map { it.toGlucoseEntry() }
        val foods = foodEntryDao.getAllFoodEntries().first().map { it.toFoodEntry() }
        val contacts = emergencyContactDao.getAllEmergencyContacts().first().map { it.toEmergencyContact() }
        val conversations = conversationDao.getAllConversations().first().map { entity ->
            val messages = chatMessageDao.getChatMessagesByConversationSync(entity.id).map { it.toChatMessage() }
            entity.toSavedConversation(messages)
        }
        val userProfile = userProfileDao.getCurrentUserProfileFlow().first()?.toUserProfile()
        val settings = appSettingsDao.getAllSettings().first().associate { it.key to it.value }

        return mapOf(
            "glucoseEntries" to glucose,
            "foodEntries" to foods,
            "emergencyContacts" to contacts,
            "conversations" to conversations,
            "userProfile" to userProfile,
            "settings" to settings
        )
    }
}

// ==================== EXTENSION FUNCTIONS ====================

// GlucoseEntry conversions
private fun GlucoseEntry.toEntity(): GlucoseEntryEntity {
    return GlucoseEntryEntity(
        id = this.id,
        value = this.value,
        timestamp = this.timestamp,
        isBeforeMeal = this.isBeforeMeal,
        notes = this.notes
    )
}

private fun GlucoseEntryEntity.toGlucoseEntry(): GlucoseEntry {
    return GlucoseEntry(
        id = this.id,
        value = this.value,
        timestamp = this.timestamp,
        isBeforeMeal = this.isBeforeMeal,
        notes = this.notes
    )
}

// FoodEntry conversions
private fun FoodEntry.toEntity(): FoodEntryEntity {
    return FoodEntryEntity(
        id = this.id,
        type = this.type,
        description = this.description,
        timestamp = this.timestamp,
        calories = this.calories,
        carbohydrates = this.carbohydrates,
        sugars = this.sugars,
        notes = this.notes
    )
}

private fun FoodEntryEntity.toFoodEntry(): FoodEntry {
    return FoodEntry(
        id = this.id,
        type = this.type,
        description = this.description,
        timestamp = this.timestamp,
        calories = this.calories,
        carbohydrates = this.carbohydrates,
        sugars = this.sugars,
        notes = this.notes
    )
}

// EmergencyContact conversions
private fun EmergencyContact.toEntity(): EmergencyContactEntity {
    return EmergencyContactEntity(
        id = this.id,
        name = this.name,
        phone = this.phone,
        relationship = this.relationship,
        isPrimary = this.isPrimary
    )
}

private fun EmergencyContactEntity.toEmergencyContact(): EmergencyContact {
    return EmergencyContact(
        id = this.id,
        name = this.name,
        phone = this.phone,
        relationship = this.relationship,
        isPrimary = this.isPrimary
    )
}
// Medication conversions
private fun Medication.toEntity(): MedicationEntity {
    return MedicationEntity(
        id = this.id,
        name = this.name,
        dose = this.dose,
        hour = this.hour,
        minute = this.minute,
        endDate = this.endDate,
        trackingState = this.trackingState,
        type = this.type // <-- AÑADE ESTA LÍNEA
    )
}

private fun MedicationEntity.toMedication(): Medication {
    return Medication(
        id = this.id,
        name = this.name,
        dose = this.dose,
        hour = this.hour,
        minute = this.minute,
        endDate = this.endDate,
        trackingState = this.trackingState,
        type = this.type // <-- AÑADE ESTA LÍNEA
    )
}
// ChatMessage conversions
private fun ChatMessage.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = 0, // Auto-generated
        text = this.text,
        isUser = this.isUser,
        timestamp = this.timestamp,
        conversationId = "" // Se debe establecer desde el contexto
    )
}

private fun ChatMessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        text = this.text,
        isUser = this.isUser,
        timestamp = this.timestamp
    )
}

// SavedConversation conversions
private fun SavedConversation.toEntity(): ConversationEntity {
    return ConversationEntity(
        id = this.id,
        title = this.title,
        timestamp = this.timestamp,
        messageCount = this.messages.size,
        lastMessageTimestamp = this.messages.maxByOrNull { it.timestamp }?.timestamp ?: this.timestamp
    )
}

private fun ConversationEntity.toSavedConversation(messages: List<ChatMessage>): SavedConversation {
    return SavedConversation(
        id = this.id,
        title = this.title,
        messages = messages,
        timestamp = this.timestamp
    )
}

// UserProfile conversions
private fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        id = this.id,
        name = this.name,
        age = this.age,
        diabetesType = this.diabetesType,
        weight = this.weight,
        height = this.height,
        diagnosisDate = this.diagnosisDate,
        doctorName = this.doctorName,
        doctorPhone = this.doctorPhone,
        medication = this.medication,
        notes = this.notes
    )
}

private fun UserProfileEntity.toUserProfile(): UserProfile {
    return UserProfile(
        id = this.id,
        name = this.name,
        age = this.age,
        diabetesType = this.diabetesType,
        weight = this.weight,
        height = this.height,
        diagnosisDate = this.diagnosisDate,
        doctorName = this.doctorName,
        doctorPhone = this.doctorPhone,
        medication = this.medication,
        notes = this.notes
    )
}
