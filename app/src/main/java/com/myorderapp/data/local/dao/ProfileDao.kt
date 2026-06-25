package com.myorderapp.data.local.dao

import androidx.room.*
import com.myorderapp.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId")
    fun getProfile(userId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE userId = :userId")
    suspend fun getProfileOnce(userId: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE userId = :userId")
    suspend fun delete(userId: String)
}
