package com.myorderapp.domain.repository

import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.PairInvitePreview
import com.myorderapp.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfile(): Flow<Profile?>
    suspend fun saveProfile(profile: Profile)
    suspend fun updateNickname(nickname: String)
    suspend fun updateAvatar(avatarUrl: String)
    suspend fun addCandyCoins(amount: Int): Boolean
    suspend fun addPartnerCandyCoins(amount: Int): Boolean
    suspend fun spendCandyCoins(amount: Int): Boolean
    suspend fun saveSelectedRole(role: String?)
    suspend fun generatePairCode(inviterRole: String): String
    suspend fun previewPairInvite(code: String): PairInvitePreview?
    suspend fun joinPair(code: String): Boolean
    suspend fun unpair()
    suspend fun getPairInfo(): PairInfo
    suspend fun touchPresence()
    fun isSynced(): Flow<Boolean>
    suspend fun loadProfile()
}
