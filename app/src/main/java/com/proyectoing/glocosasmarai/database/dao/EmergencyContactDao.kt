package com.proyectoing.glocosasmarai.database.dao

import androidx.room.*
import com.proyectoing.glocosasmarai.database.entities.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con contactos de emergencia
 */
@Dao
interface EmergencyContactDao {
    
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, name ASC")
    fun getAllEmergencyContacts(): Flow<List<EmergencyContactEntity>>
    
    @Query("SELECT * FROM emergency_contacts WHERE id = :id")
    suspend fun getEmergencyContactById(id: Long): EmergencyContactEntity?
    
    @Query("SELECT * FROM emergency_contacts WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryEmergencyContact(): EmergencyContactEntity?
    
    @Query("SELECT * FROM emergency_contacts WHERE relationship = :relationship ORDER BY name ASC")
    fun getEmergencyContactsByRelationship(relationship: String): Flow<List<EmergencyContactEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContact(emergencyContact: EmergencyContactEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContacts(emergencyContacts: List<EmergencyContactEntity>)
    
    @Update
    suspend fun updateEmergencyContact(emergencyContact: EmergencyContactEntity)
    
    @Delete
    suspend fun deleteEmergencyContact(emergencyContact: EmergencyContactEntity)
    
    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteEmergencyContactById(id: Long)
    
    @Query("DELETE FROM emergency_contacts")
    suspend fun deleteAllEmergencyContacts()
    
    @Query("UPDATE emergency_contacts SET isPrimary = 0")
    suspend fun clearPrimaryContacts()
    
    @Query("UPDATE emergency_contacts SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimaryContact(id: Long)
}
