package com.myorderapp.data.repository

import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseApi
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseProfileRepository(
    private val api: SupabaseApi,
    private val session: SessionManager
) : ProfileRepository {

    private val _profile = MutableStateFlow<Profile?>(null)
    private val _synced = MutableStateFlow(false)

    override fun getProfile(): Flow<Profile?> = _profile.asStateFlow()

    override suspend fun saveProfile(profile: Profile) {
        _profile.value = profile
        if (session.isLoggedIn.value) {
            try {
                api.upsertProfile(profile, session.accessToken)
                _synced.value = true
            } catch (_: Exception) { }
        }
    }

    override suspend fun updateNickname(nickname: String) {
        val current = _profile.value ?: return
        val updated = current.copy(nickname = nickname.trim())
        saveProfile(updated)
    }

    override suspend fun updateAvatar(avatarUrl: String) {
        val current = _profile.value ?: return
        saveProfile(current.copy(avatarUrl = avatarUrl))
    }

    override suspend fun generatePairCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    override suspend fun joinPair(code: String): Boolean {
        if (code.length != 6) return false
        val current = _profile.value ?: return false
        saveProfile(current.copy(pairId = code.uppercase()))
        session.setPairId(code.uppercase())
        return true
    }

    override suspend fun getPairInfo(): PairInfo {
        val p = _profile.value ?: return PairInfo()
        val pairId = p.pairId
        if (pairId.isBlank() || pairId == "00000000-0000-0000-0000-000000000000") {
            return PairInfo(isPaired = false, isOnline = session.isLoggedIn.value)
        }

        var partnerName = ""
        if (session.isLoggedIn.value) {
            try {
                val partnerProfiles = api.getProfilesByPairId(pairId, session.accessToken)
                partnerName = partnerProfiles
                    .firstOrNull { it.userId != session.currentUserId }
                    ?.nickname ?: ""
            } catch (_: Exception) { }
        }

        return PairInfo(
            partnerName = partnerName.ifBlank { "已配对" },
            isPaired = true,
            isOnline = session.isLoggedIn.value,
            pairCode = pairId
        )
    }

    override suspend fun unpair() {
        val current = _profile.value ?: return
        val defaultId = "00000000-0000-0000-0000-000000000000"
        saveProfile(current.copy(pairId = defaultId))
        session.setPairId(defaultId)
    }

    override fun isSynced(): Flow<Boolean> = _synced.asStateFlow()

    suspend fun loadFromCloud() {
        if (!session.isLoggedIn.value) return
        try {
            val profiles = api.getProfile(session.currentUserId, session.accessToken)
            if (profiles.isNotEmpty()) {
                _profile.value = profiles.first()
                _synced.value = true
            }
        } catch (_: Exception) { }
    }
}
