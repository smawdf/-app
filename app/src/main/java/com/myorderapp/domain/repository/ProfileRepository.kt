package com.myorderapp.domain.repository

import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfile(): Flow<Profile?>
    suspend fun saveProfile(profile: Profile)
    suspend fun updateNickname(nickname: String)
    suspend fun updateAvatar(avatarUrl: String)
    suspend fun generatePairCode(): String
    suspend fun joinPair(code: String): Boolean
    suspend fun unpair()
    suspend fun getPairInfo(): PairInfo
    fun isSynced(): Flow<Boolean>
    suspend fun loadProfile()
}
