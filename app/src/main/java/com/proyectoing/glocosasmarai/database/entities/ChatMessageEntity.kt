package com.proyectoing.glocosasmarai.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Entidad para almacenar mensajes del chatbot en la base de datos local
 */

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val conversationId: String,
    val createdAt: Long = System.currentTimeMillis()
)
