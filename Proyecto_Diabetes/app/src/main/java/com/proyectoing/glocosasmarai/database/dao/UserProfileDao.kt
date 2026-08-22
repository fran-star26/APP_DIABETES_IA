package com.proyectoing.glocosasmarai.database.dao

import androidx.room.*
import com.proyectoing.glocosasmarai.database.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con el perfil del usuario
 */
@Dao
interface UserProfileDao {
    
    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun getUserProfileById(id: String): UserProfileEntity?
    
    @Query("SELECT * FROM user_profile WHERE id = :id")
    fun getUserProfileByIdFlow(id: String): Flow<UserProfileEntity?>
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getCurrentUserProfile(): UserProfileEntity?
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getCurrentUserProfileFlow(): Flow<UserProfileEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfileEntity)
    
    @Update
    suspend fun updateUserProfile(userProfile: UserProfileEntity)
    
    @Delete
    suspend fun deleteUserProfile(userProfile: UserProfileEntity)
    
    @Query("DELETE FROM user_profile WHERE id = :id")
    suspend fun deleteUserProfileById(id: String)
    
    @Query("DELETE FROM user_profile")
    suspend fun deleteAllUserProfiles()
}
