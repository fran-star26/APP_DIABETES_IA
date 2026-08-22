package com.proyectoing.glocosasmarai.database.dao

import androidx.room.*
import com.proyectoing.glocosasmarai.database.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE id = :id")
    suspend fun getChatMessageById(id: Long): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getChatMessagesByConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getChatMessagesByConversationSync(conversationId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getChatMessagesByDateRange(startTime: Long, endTime: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE isUser = :isUser ORDER BY timestamp DESC")
    fun getChatMessagesByUser(isUser: Boolean): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun getMessageCountByConversation(conversationId: String): Int

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageByConversation(conversationId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(chatMessage: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessages(chatMessages: List<ChatMessageEntity>)

    @Update
    suspend fun updateChatMessage(chatMessage: ChatMessageEntity)

    @Delete
    suspend fun deleteChatMessage(chatMessage: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteChatMessageById(id: Long)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteChatMessagesByConversation(conversationId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessages()
}