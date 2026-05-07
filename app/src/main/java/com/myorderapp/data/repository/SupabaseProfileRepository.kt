package com.myorderapp.data.repository

import android.content.Context
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseApi
import com.myorderapp.domain.model.DietaryPreference
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SupabaseProfileRepository(
    private val api: SupabaseApi,
    private val session: SessionManager,
    context: Context
) : ProfileRepository {

    private val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val _profile = MutableStateFlow<Profile?>(loadLocalProfile())
    private val _synced = MutableStateFlow(false)

    override fun getProfile(): Flow<Profile?> = _profile.asStateFlow()

    override suspend fun saveProfile(profile: Profile) {
        _profile.value = profile
        saveLocalProfile(profile)
        session.saveNickname(profile.nickname)
        session.saveAvatar(profile.avatarUrl ?: "")
        if (session.isLoggedIn.value) {
            try {
                api.upsertProfile(profile, session.accessToken)
                _synced.value = true
            } catch (_: Exception) { }
        }
    }

    override suspend fun updateNickname(nickname: String) {
        val current = _profile.value ?: loadLocalProfile()
        val updated = current.copy(nickname = nickname.trim())
        _profile.value = updated
        saveLocalProfile(updated)
        if (session.isLoggedIn.value) {
            try { api.upsertProfile(updated, session.accessToken); _synced.value = true } catch (_: Exception) { }
        }
    }

    override suspend fun updateAvatar(avatarUrl: String) {
        val current = _profile.value ?: loadLocalProfile()
        val updated = current.copy(avatarUrl = avatarUrl)
        _profile.value = updated
        saveLocalProfile(updated)
        if (session.isLoggedIn.value) {
            try { api.upsertProfile(updated, session.accessToken); _synced.value = true } catch (_: Exception) { }
        }
    }

    override suspend fun loadProfile() {
        // 先从本地恢复
        val local = loadLocalProfile()
        if (_profile.value == null) _profile.value = local
        // 再尝试云端
        loadFromCloud()
    }

    override suspend fun generatePairCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    override suspend fun joinPair(code: String): Boolean {
        if (code.length != 6) return false
        val current = _profile.value ?: loadLocalProfile()
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
                saveLocalProfile(profiles.first())
                _synced.value = true
            } else {
                // 云端无 Profile — 用本地数据复写
                val local = loadLocalProfile()
                if (local.nickname.isNotBlank() || !local.avatarUrl.isNullOrBlank()) {
                    api.createProfile(local, session.accessToken)
                    _profile.value = local
                    _synced.value = true
                }
            }
        } catch (_: Exception) { }
    }

    // ── 本地 SharedPreferences 持久化 ──

    private fun saveLocalProfile(profile: Profile) {
        prefs.edit()
            .putString("nickname", profile.nickname)
            .putString("avatar_url", profile.avatarUrl ?: "")
            .putString("user_id", profile.userId.ifBlank { session.currentUserId })
            .apply()
    }

    private fun loadLocalProfile(): Profile {
        return Profile(
            userId = prefs.getString("user_id", "") ?: "",
            nickname = prefs.getString("nickname", "") ?: "",
            avatarUrl = prefs.getString("avatar_url", "")?.ifBlank { null },
            tastePrefs = DietaryPreference()
        )
    }
}
