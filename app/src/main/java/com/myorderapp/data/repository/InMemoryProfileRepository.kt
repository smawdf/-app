package com.myorderapp.data.repository

import com.myorderapp.domain.model.DietaryPreference
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.ProfileRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryProfileRepository : ProfileRepository {

    private val _profile = MutableStateFlow<Profile?>(
        Profile(
            id = "u1",
            userId = "u1",
            pairId = "",
            nickname = "",
            tastePrefs = DietaryPreference(),
            allergies = emptyList()
        )
    )
    private val _synced = MutableStateFlow(false)
    private var pendingPairCode: String = ""

    override fun getProfile(): Flow<Profile?> = _profile

    override suspend fun saveProfile(profile: Profile) {
        _profile.value = profile
    }

    override suspend fun updateNickname(nickname: String) {
        val current = _profile.value ?: Profile()
        _profile.value = current.copy(nickname = nickname.trim())
    }

    override suspend fun updateAvatar(avatarUrl: String) {
        val current = _profile.value ?: Profile()
        _profile.value = current.copy(avatarUrl = avatarUrl)
    }

    override suspend fun loadProfile() {
        // In-memory already has a default profile
    }

    override suspend fun generatePairCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        pendingPairCode = code
        return code
    }

    override suspend fun joinPair(code: String): Boolean {
        if (code.length != 6) return false
        val current = _profile.value ?: return false
        _profile.value = current.copy(pairId = code.uppercase(), pairedAt = current.pairedAt.ifBlank { Instant.now().toString() })
        pendingPairCode = ""
        return true
    }

    override suspend fun unpair() {
        val current = _profile.value ?: return
        _profile.value = current.copy(pairId = "", pairedAt = "")
        pendingPairCode = ""
    }

    override suspend fun getPairInfo(): PairInfo {
        val p = _profile.value
        return PairInfo(
            partnerName = if (p?.pairId.isNullOrBlank()) "" else "已配对",
            isPaired = !p?.pairId.isNullOrBlank(),
            isOnline = !p?.pairId.isNullOrBlank(),
            pairCode = p?.pairId ?: ""
        )
    }

    override suspend fun touchPresence() {
        val current = _profile.value ?: return
        _profile.value = current.copy(updatedAt = Instant.now().toString())
    }

    override fun isSynced(): Flow<Boolean> = _synced
}
