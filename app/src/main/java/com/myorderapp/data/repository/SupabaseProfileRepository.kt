package com.myorderapp.data.repository

import android.content.Context
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.domain.model.DietaryPreference
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.ProfileRepository
import io.github.jan.supabase.postgrest.from
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseProfileRepository(
    private val session: SessionManager,
    context: Context
) : ProfileRepository {

    private val client = SupabaseClientProvider.client
    private val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow<Profile?>(loadLocalProfile())
    private val _synced = MutableStateFlow(false)

    override fun getProfile(): Flow<Profile?> = _profile.asStateFlow()

    override suspend fun saveProfile(profile: Profile) {
        val normalized = normalizeProfile(profile).copy(updatedAt = Instant.now().toString())
        _profile.value = normalized
        saveLocalProfile(normalized)
        session.saveNickname(normalized.nickname)
        session.saveAvatar(normalized.avatarUrl ?: "")
        if (session.isLoggedIn.value) {
            try {
                client.from("profiles").upsert(normalized) { select() }
                _synced.value = true
            } catch (_: Exception) { }
        }
    }

    override suspend fun updateNickname(nickname: String) {
        val current = _profile.value ?: loadLocalProfile()
        val updated = current.copy(nickname = nickname.trim(), updatedAt = Instant.now().toString())
        _profile.value = updated
        saveLocalProfile(updated)
        if (session.isLoggedIn.value) {
            try { client.from("profiles").upsert(updated) { select() }; _synced.value = true } catch (_: Exception) { }
        }
    }

    override suspend fun updateAvatar(avatarUrl: String) {
        val current = _profile.value ?: loadLocalProfile()
        val updated = current.copy(avatarUrl = avatarUrl, updatedAt = Instant.now().toString())
        _profile.value = updated
        saveLocalProfile(updated)
        if (session.isLoggedIn.value) {
            try { client.from("profiles").upsert(updated) { select() }; _synced.value = true } catch (_: Exception) { }
        }
    }

    override suspend fun loadProfile() {
        val local = loadLocalProfile()
        if (_profile.value == null) _profile.value = local
        loadFromCloud()
    }

    override suspend fun generatePairCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        val current = _profile.value ?: loadLocalProfile()
        saveProfile(current.copy(pairId = code, pairedAt = current.pairedAt.ifBlank { Instant.now().toString() }))
        session.setPairId(code)
        return code
    }

    override suspend fun joinPair(code: String): Boolean {
        if (code.length != 6) return false
        val current = _profile.value ?: loadLocalProfile()
        saveProfile(current.copy(pairId = code.uppercase(), pairedAt = current.pairedAt.ifBlank { Instant.now().toString() }))
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
        var partnerUpdatedAt = ""
        if (session.isLoggedIn.value) {
            try {
                val partnerProfiles = client.from("profiles").select {
                    filter { eq("pair_id", pairId) }
                }.decodeList<Profile>()
                val partner = partnerProfiles.firstOrNull { it.userId != session.currentUserId }
                partnerName = partner?.nickname ?: ""
                partnerUpdatedAt = partner?.updatedAt ?: ""
            } catch (_: Exception) { }
        }
        return PairInfo(
            partnerName = partnerName.ifBlank { "已配对" },
            isPaired = true,
            isOnline = partnerUpdatedAt.isRecentlySeen(),
            pairCode = pairId
        )
    }

    override suspend fun touchPresence() {
        val current = _profile.value ?: loadLocalProfile()
        val updated = normalizeProfile(current).copy(updatedAt = Instant.now().toString())
        _profile.value = updated
        saveLocalProfile(updated)
        if (session.isLoggedIn.value) {
            try {
                client.from("profiles").update(
                    mapOf("updated_at" to updated.updatedAt)
                ) {
                    filter { eq("user_id", session.currentUserId) }
                }
                _synced.value = true
            } catch (_: Exception) { }
        }
    }

    override suspend fun unpair() {
        val current = _profile.value ?: return
        val defaultId = "00000000-0000-0000-0000-000000000000"
        saveProfile(current.copy(pairId = defaultId, pairedAt = ""))
        session.setPairId(defaultId)
        if (session.isLoggedIn.value) {
            try {
                client.from("profiles").update(
                    mapOf("pair_id" to defaultId)
                ) {
                    filter { eq("user_id", session.currentUserId) }
                }
            } catch (_: Exception) { }
        }
    }

    override fun isSynced(): Flow<Boolean> = _synced.asStateFlow()

    suspend fun checkSessionValid(): Boolean {
        if (!session.isLoggedIn.value) return true
        try {
            val profiles = client.from("profiles").select {
                filter { eq("user_id", session.currentUserId) }
            }.decodeList<Profile>()
            val cloudSessionId = profiles.firstOrNull()?.sessionId ?: ""
            return cloudSessionId.isBlank() || cloudSessionId == session.currentSessionId
        } catch (_: Exception) {
            return true
        }
    }

    suspend fun loadFromCloud() {
        if (!session.isLoggedIn.value) return
        try {
            val profiles = client.from("profiles").select {
                filter { eq("user_id", session.currentUserId) }
            }.decodeList<Profile>()
            if (profiles.isNotEmpty()) {
                val p = profiles.first()
                _profile.value = p
                saveLocalProfile(p)
                session.saveNickname(p.nickname)
                session.saveAvatar(p.avatarUrl ?: "")
                _synced.value = true
            } else {
                val local = normalizeProfile(loadLocalProfile())
                if (local.nickname.isNotBlank() || !local.avatarUrl.isNullOrBlank()) {
                    client.from("profiles").insert(local) { select() }
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
            .putString("pair_id", profile.pairId.ifBlank { session.currentPairId })
            .putString("paired_at", profile.pairedAt)
            .putString("created_at", profile.createdAt)
            .putString("updated_at", profile.updatedAt)
            .apply()
    }

    private fun loadLocalProfile(): Profile {
        return Profile(
            userId = prefs.getString("user_id", "") ?: "",
            pairId = prefs.getString("pair_id", "") ?: "",
            nickname = prefs.getString("nickname", "") ?: "",
            avatarUrl = prefs.getString("avatar_url", "")?.ifBlank { null },
            tastePrefs = DietaryPreference(),
            createdAt = prefs.getString("created_at", "") ?: "",
            updatedAt = prefs.getString("updated_at", "") ?: "",
            pairedAt = prefs.getString("paired_at", "") ?: ""
        )
    }

    private fun normalizeProfile(profile: Profile): Profile {
        val defaultPairId = "00000000-0000-0000-0000-000000000000"
        return profile.copy(
            userId = profile.userId.ifBlank { session.currentUserId },
            pairId = profile.pairId.ifBlank {
                session.currentPairId.ifBlank { defaultPairId }
            },
            createdAt = profile.createdAt.ifBlank { Instant.now().toString() }
        )
    }

    private fun String.isRecentlySeen(): Boolean {
        val seenAt = runCatching { Instant.parse(this) }.getOrNull() ?: return false
        return Duration.between(seenAt, Instant.now()).abs() <= Duration.ofMinutes(5)
    }
}
