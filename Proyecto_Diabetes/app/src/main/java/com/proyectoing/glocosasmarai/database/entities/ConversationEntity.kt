package com.proyectoing.glocosasmarai.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para almacenar conversaciones del chatbot en la base de datos local
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val timestamp: Long,
    val messageCount: Int = 0,
    val lastMessageTimestamp: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
