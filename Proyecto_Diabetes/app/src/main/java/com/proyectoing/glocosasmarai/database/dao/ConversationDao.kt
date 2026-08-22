package com.proyectoing.glocosasmarai.database.dao

import androidx.room.*
import com.proyectoing.glocosasmarai.database.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con conversaciones del chatbot
 */
@Dao
interface ConversationDao {
    
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?
    
    @Query("SELECT * FROM conversations WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY lastMessageTimestamp DESC")
    fun getConversationsByDateRange(startTime: Long, endTime: Long): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC LIMIT :limit")
    fun getRecentConversations(limit: Int): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE title LIKE :searchQuery ORDER BY lastMessageTimestamp DESC")
    fun searchConversations(searchQuery: String): Flow<List<ConversationEntity>>
    
    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)
    
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
    
    @Query("DELETE FROM conversations WHERE lastMessageTimestamp < :cutoffTime")
    suspend fun deleteOldConversations(cutoffTime: Long)
}
