package com.myorderapp.data.repository

import android.content.Context
import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.domain.model.CandyCoinRecord
import com.myorderapp.domain.model.DietaryPreference
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.PairInvitePreview
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.model.ROLE_CARETAKER
import com.myorderapp.domain.model.ROLE_EATER
import com.myorderapp.domain.repository.ProfileRepository
import com.myorderapp.domain.repository.CandyCoinLedgerRepository
import io.github.jan.supabase.postgrest.from
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseProfileRepository(
    private val session: SessionManager,
    context: Context,
    private val candyCoinLedgerRepository: CandyCoinLedgerRepository,
    private val cloudErrorLogger: CloudErrorLogger? = null
) : ProfileRepository {

    private companion object {
        const val KEY_PENDING_PAIR_CODE = "pending_pair_code"
        const val DEFAULT_PAIR_ID = "00000000-0000-0000-0000-000000000000"
        val DEVICE_SESSION_TIMEOUT: Duration = Duration.ofDays(30)
    }

    private val client = SupabaseClientProvider.client
    private val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow<Profile?>(loadLocalProfile())
    private val _synced = MutableStateFlow(false)

    override fun getProfile(): Flow<Profile?> = _profile.asStateFlow()

    override suspend fun saveProfile(profile: Profile) {
        val normalized = normalizeProfile(profile).copy(
            avatarUrl = profile.avatarUrl?.takeIfCloudAvatarUrl(),
            updatedAt = Instant.now().toString()
        )
        _profile.value = normalized
        saveLocalProfile(normalized)
        session.saveNickname(normalized.nickname)
        session.saveAvatar(normalized.avatarUrl ?: "")
        if (session.isLoggedIn.value) {
            try {
                client.from("profiles").upsert(normalized.toCloudUpsertMap()) { select() }
                syncAnniversaryToCloud(normalized)
                _synced.value = true
            } catch (e: Exception) {
                cloudErrorLogger?.log("profile", "save_profile", e, "userId=${normalized.userId}")
            }
        }
    }

    override suspend fun updateNickname(nickname: String) {
        val current = _profile.value ?: loadLocalProfile()
        val updated = current.copy(nickname = nickname.trim(), updatedAt = Instant.now().toString())
        _profile.value = updated
        saveLocalProfile(updated)
        session.saveNickname(updated.nickname)
        if (session.isLoggedIn.value) {
            try { client.from("profiles").upsert(updated.toCloudUpsertMap()) { select() }; _synced.value = true } catch (e: Exception) {
                cloudErrorLogger?.log("profile", "update_nickname", e, "userId=${updated.userId}")
            }
        }
    }

    override suspend fun updateAvatar(avatarUrl: String) {
        val current = _profile.value ?: loadLocalProfile()
        val updated = current.copy(avatarUrl = avatarUrl.takeIfCloudAvatarUrl(), updatedAt = Instant.now().toString())
        _profile.value = updated
        saveLocalProfile(updated)
        session.saveAvatar(updated.avatarUrl ?: "")
        if (session.isLoggedIn.value) {
            try { client.from("profiles").upsert(updated.toCloudUpsertMap()) { select() }; _synced.value = true } catch (e: Exception) {
                cloudErrorLogger?.log("profile", "update_avatar", e, "userId=${updated.userId}")
            }
        }
    }

    override suspend fun addCandyCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        val current = _profile.value ?: loadLocalProfile()
        return persistCandyCoins(current, (current.candyCoins + amount).coerceAtMost(9999))
    }

    override suspend fun addPartnerCandyCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        val current = _profile.value ?: loadLocalProfile()
        val pairId = current.pairId
        if (!session.isLoggedIn.value || pairId.isBlank() || pairId == "00000000-0000-0000-0000-000000000000") {
            return false
        }
        return try {
            val partner = client.from("profiles").select {
                filter { eq("pair_id", pairId) }
            }.decodeList<Profile>().firstOrNull { it.userId != session.currentUserId } ?: return false
            val newBalance = (partner.candyCoins + amount).coerceAtMost(9999)
            client.from("profiles").update(
                mapOf("candy_coins" to newBalance, "updated_at" to Instant.now().toString())
            ) {
                filter { eq("user_id", partner.userId) }
            }
            candyCoinLedgerRepository.addRecord(
                CandyCoinRecord(
                    id = UUID.randomUUID().toString(),
                    type = "recharge",
                    amount = amount,
                    balanceAfter = newBalance,
                    actorRole = "caretaker",
                    targetRole = "eater",
                    note = "饲养员充值",
                    createdAt = Instant.now().toString()
                )
            )
            true
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "add_partner_candy", e, "pairId=$pairId amount=$amount")
            false
        }
    }

    override suspend fun spendCandyCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        val current = _profile.value ?: loadLocalProfile()
        if (current.candyCoins < amount) return false
        return persistCandyCoins(current, current.candyCoins - amount)
    }

    override suspend fun loadProfile() {
        val local = loadLocalProfile()
        if (_profile.value == null) _profile.value = local
        loadFromCloud()
    }

    override suspend fun saveSelectedRole(role: String?) {
        val normalizedRole = role?.takeIf { it == ROLE_CARETAKER || it == ROLE_EATER }.orEmpty()
        prefs.edit().putString("selected_role", normalizedRole).apply()
        val updatedProfile = (_profile.value ?: loadLocalProfile()).copy(
            selectedRole = normalizedRole,
            updatedAt = Instant.now().toString()
        )
        _profile.value = updatedProfile
        saveLocalProfile(updatedProfile)
        if (!session.isLoggedIn.value) return
        try {
            client.from("profiles").update(
                mapOf("selected_role" to normalizedRole, "updated_at" to updatedProfile.updatedAt)
            ) {
                filter { eq("user_id", session.currentUserId) }
            }
            _synced.value = true
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "save_selected_role", e, "role=$normalizedRole")
        }
    }

    override suspend fun generatePairCode(inviterRole: String): String {
        val normalizedRole = inviterRole.takeIf { it == ROLE_CARETAKER || it == ROLE_EATER } ?: ROLE_CARETAKER
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        val current = _profile.value ?: loadLocalProfile()
        saveProfile(current.copy(pairId = code, pairedAt = current.pairedAt.ifBlank { Instant.now().toString() }))
        session.setPairId(code)
        prefs.edit().putString(KEY_PENDING_PAIR_CODE, code).apply()
        saveSelectedRole(normalizedRole)
        return code
    }

    override suspend fun previewPairInvite(code: String): PairInvitePreview? {
        if (code.length != 6 || !session.isLoggedIn.value) return null
        val normalizedCode = code.uppercase()
        return try {
            val rows = client.from("profiles").select {
                filter { eq("pair_id", normalizedCode) }
            }.decodeList<PairInviteProfileRow>()
            val inviter = rows.firstOrNull { it.userId != session.currentUserId } ?: rows.firstOrNull()
            val inviterRole = inviter?.selectedRole?.takeIf { it == ROLE_CARETAKER || it == ROLE_EATER } ?: return null
            PairInvitePreview(
                code = normalizedCode,
                inviterName = inviter.nickname.ifBlank { "对方" },
                inviterRole = inviterRole
            )
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "preview_pair_invite", e, "code=$normalizedCode")
            null
        }
    }

    override suspend fun joinPair(code: String): Boolean {
        if (code.length != 6) return false
        val current = _profile.value ?: loadLocalProfile()
        val normalizedCode = code.uppercase()
        saveProfile(current.copy(pairId = normalizedCode, pairedAt = current.pairedAt.ifBlank { Instant.now().toString() }))
        session.setPairId(normalizedCode)
        prefs.edit().remove(KEY_PENDING_PAIR_CODE).apply()
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
        var partnerCandyCoins: Int? = null
        var hasPartner = false
        if (session.isLoggedIn.value) {
            try {
                val partnerProfiles = client.from("profiles").select {
                    filter { eq("pair_id", pairId) }
                }.decodeList<Profile>()
                val partner = partnerProfiles.firstOrNull { it.userId != session.currentUserId }
                hasPartner = partner != null
                partnerName = partner?.nickname ?: ""
                partnerUpdatedAt = partner?.updatedAt ?: ""
                partnerCandyCoins = partner?.candyCoins
            } catch (e: Exception) {
                cloudErrorLogger?.log("profile", "get_pair_info", e, "pairId=$pairId")
            }
        }
        if (!hasPartner) {
            return PairInfo(
                isPaired = false,
                isOnline = session.isLoggedIn.value,
                pairCode = pairId
            )
        }
        return PairInfo(
            partnerName = partnerName.ifBlank { "已配对" },
            isPaired = true,
            isOnline = partnerUpdatedAt.isRecentlySeen(),
            pairCode = pairId,
            partnerCandyCoins = partnerCandyCoins
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
                    mapOf(
                        "updated_at" to updated.updatedAt,
                        "session_updated_at" to updated.updatedAt
                    )
                ) {
                    filter { eq("user_id", session.currentUserId) }
                }
                _synced.value = true
            } catch (e: Exception) {
                cloudErrorLogger?.log("profile", "touch_presence", e)
            }
        }
    }

    override suspend fun unpair() {
        val current = _profile.value ?: return
        saveProfile(current.copy(pairId = DEFAULT_PAIR_ID, pairedAt = ""))
        session.setPairId(DEFAULT_PAIR_ID)
        prefs.edit().remove(KEY_PENDING_PAIR_CODE).apply()
        saveSelectedRole(null)
        if (session.isLoggedIn.value) {
            try {
                client.from("profiles").update(
                    mapOf("pair_id" to DEFAULT_PAIR_ID)
                ) {
                    filter { eq("user_id", session.currentUserId) }
                }
            } catch (e: Exception) {
                cloudErrorLogger?.log("profile", "unpair", e)
            }
        }
    }

    override fun isSynced(): Flow<Boolean> = _synced.asStateFlow()

    fun canStartDeviceSession(profile: Profile?): Boolean {
        val cloudSessionId = profile?.sessionId.orEmpty()
        if (cloudSessionId.isBlank()) return true
        if (cloudSessionId == session.currentSessionId || cloudSessionId == session.currentStableDeviceSessionId) return true
        return isDeviceSessionExpired(profile?.sessionUpdatedAt.orEmpty())
    }

    suspend fun claimCurrentDeviceSession(profile: Profile? = null): Profile? {
        if (!session.isLoggedIn.value || session.currentUserId.isBlank() || session.currentSessionId.isBlank()) {
            return null
        }
        val now = Instant.now().toString()
        return try {
            session.migrateToStableDeviceSession()
            client.from("profiles").update(
                mapOf(
                    "session_id" to session.currentSessionId,
                    "session_updated_at" to now,
                    "updated_at" to now
                )
            ) {
                filter { eq("user_id", session.currentUserId) }
            }
            val updated = (profile ?: _profile.value ?: loadLocalProfile()).copy(
                sessionId = session.currentSessionId,
                sessionUpdatedAt = now,
                updatedAt = now
            )
            _profile.value = updated
            saveLocalProfile(updated)
            updated
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "claim_device_session", e)
            null
        }
    }

    suspend fun releaseCurrentDeviceSession() {
        if (!session.isLoggedIn.value || session.currentUserId.isBlank()) return
        try {
            client.from("profiles").update(
                mapOf(
                    "session_id" to "",
                    "session_updated_at" to "",
                    "updated_at" to Instant.now().toString()
                )
            ) {
                filter { eq("user_id", session.currentUserId) }
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "release_device_session", e)
        }
    }

    suspend fun checkSessionValid(): Boolean {
        if (!session.isLoggedIn.value) return true
        try {
            val profiles = client.from("profiles").select {
                filter { eq("user_id", session.currentUserId) }
            }.decodeList<Profile>()
            val profile = profiles.firstOrNull() ?: return true
            val cloudSessionId = profile.sessionId
            if (cloudSessionId.isBlank()) return true
            if (isDeviceSessionExpired(profile.sessionUpdatedAt)) {
                releaseCurrentDeviceSession()
                return false
            }
            return cloudSessionId == session.currentSessionId || cloudSessionId == session.currentStableDeviceSessionId
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "check_session_valid", e)
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
                val cloud = profiles.first()
                val local = loadLocalProfile()
                val anniversary = loadAnniversaryFromCloud(cloud.pairId)
                val merged = mergeCloudProfileWithLocalFallback(cloud, local)
                    .copy(pairedAt = anniversary.ifBlank { local.pairedAt })
                if (anniversary.isBlank() && merged.pairedAt.isNotBlank()) {
                    syncAnniversaryToCloud(merged)
                }
                if (merged != cloud) {
                    client.from("profiles").update(
                        mapOf(
                            "nickname" to merged.nickname,
                    "avatar_url" to merged.avatarUrl?.takeIfCloudAvatarUrl(),
                            "updated_at" to Instant.now().toString()
                        )
                    ) {
                        filter { eq("user_id", session.currentUserId) }
                    }
                }
                _profile.value = merged
                saveLocalProfile(merged)
                session.saveNickname(merged.nickname)
                session.saveAvatar(merged.avatarUrl ?: "")
                _synced.value = true
            } else {
                val local = normalizeProfile(loadLocalProfile())
                if (local.nickname.isNotBlank() || !local.avatarUrl.isNullOrBlank()) {
                    client.from("profiles").upsert(local.toCloudUpsertMap()) { select() }
                    _profile.value = local
                    _synced.value = true
                }
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "load_profile", e)
        }
    }

    // ── 本地 SharedPreferences 持久化 ──

    private fun saveLocalProfile(profile: Profile) {
        prefs.edit()
            .putString("nickname", profile.nickname)
            .putString("avatar_url", profile.avatarUrl?.takeIfCloudAvatarUrl() ?: "")
            .putString("user_id", profile.userId.ifBlank { session.currentUserId })
            .putString("pair_id", profile.pairId.ifBlank { session.currentPairId })
            .putString("paired_at", profile.pairedAt)
            .putString("created_at", profile.createdAt)
            .putString("updated_at", profile.updatedAt)
            .putString("session_id", profile.sessionId)
            .putString("session_updated_at", profile.sessionUpdatedAt)
            .putString("selected_role", profile.selectedRole)
            .putInt("candy_coins", profile.candyCoins)
            .apply()
    }

    private fun loadLocalProfile(): Profile {
        val savedUserId = prefs.getString("user_id", "") ?: ""
        if (session.currentUserId.isNotBlank() && savedUserId.isNotBlank() && savedUserId != session.currentUserId) {
            return Profile(userId = session.currentUserId, pairId = session.currentPairId)
        }
        return Profile(
            userId = savedUserId,
            pairId = prefs.getString("pair_id", "") ?: "",
            nickname = prefs.getString("nickname", "") ?: "",
            avatarUrl = prefs.getString("avatar_url", "")?.ifBlank { null }?.takeIfCloudAvatarUrl(),
            tastePrefs = DietaryPreference(),
            createdAt = prefs.getString("created_at", "") ?: "",
            updatedAt = prefs.getString("updated_at", "") ?: "",
            sessionId = prefs.getString("session_id", "") ?: "",
            sessionUpdatedAt = prefs.getString("session_updated_at", "") ?: "",
            selectedRole = prefs.getString("selected_role", "") ?: "",
            candyCoins = prefs.getInt("candy_coins", 66),
            pairedAt = prefs.getString("paired_at", "") ?: ""
        )
    }

    private fun normalizeProfile(profile: Profile): Profile {
        return profile.copy(
            userId = profile.userId.ifBlank { session.currentUserId },
            pairId = profile.pairId.ifBlank {
                session.currentPairId.ifBlank { DEFAULT_PAIR_ID }
            },
            createdAt = profile.createdAt.ifBlank { Instant.now().toString() },
            sessionId = profile.sessionId.ifBlank { _profile.value?.sessionId.orEmpty() },
            sessionUpdatedAt = profile.sessionUpdatedAt.ifBlank { _profile.value?.sessionUpdatedAt.orEmpty() }
        )
    }

    private fun mergeCloudProfileWithLocalFallback(cloud: Profile, local: Profile): Profile {
        val sameUser = local.userId.isBlank() || local.userId == cloud.userId
        if (!sameUser) return cloud
        return cloud.copy(
            nickname = cloud.nickname.ifBlank { local.nickname },
            avatarUrl = cloud.avatarUrl?.takeIfCloudAvatarUrl() ?: local.avatarUrl?.takeIfCloudAvatarUrl()
        )
    }

    private suspend fun loadAnniversaryFromCloud(pairId: String): String {
        val normalizedPairId = pairId.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID } ?: return ""
        return try {
            client.from("anniversaries").select {
                filter { eq("pair_id", normalizedPairId) }
            }.decodeList<RemoteAnniversary>().firstOrNull()?.pairedAt.orEmpty()
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "load_anniversary", e, "pairId=$normalizedPairId")
            ""
        }
    }

    private suspend fun syncAnniversaryToCloud(profile: Profile) {
        val pairId = profile.pairId.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID } ?: return
        if (profile.pairedAt.isBlank()) return
        client.from("anniversaries").upsert(
            RemoteAnniversary(
                pairId = pairId,
                pairedAt = profile.pairedAt,
                updatedAt = Instant.now().toString()
            )
        ) { select() }
    }

    private fun Profile.toCloudUpsertMap(): Map<String, Any?> {
        return mapOf(
            "user_id" to userId.ifBlank { session.currentUserId },
            "pair_id" to pairId.ifBlank { session.currentPairId.ifBlank { DEFAULT_PAIR_ID } },
            "nickname" to nickname,
            "avatar_url" to avatarUrl?.takeIfCloudAvatarUrl(),
            "candy_coins" to candyCoins,
            "session_id" to sessionId,
            "session_updated_at" to sessionUpdatedAt,
            "selected_role" to selectedRole,
            "updated_at" to updatedAt.ifBlank { Instant.now().toString() }
        )
    }

    private fun isDeviceSessionExpired(sessionUpdatedAt: String): Boolean {
        val seenAt = runCatching { Instant.parse(sessionUpdatedAt) }.getOrNull() ?: return false
        return Duration.between(seenAt, Instant.now()) > DEVICE_SESSION_TIMEOUT
    }

    private suspend fun persistCandyCoins(current: Profile, newBalance: Int): Boolean {
        val updated = normalizeProfile(current).copy(candyCoins = newBalance, updatedAt = Instant.now().toString())
        if (session.isLoggedIn.value) {
            try {
                client.from("profiles").update(
                    mapOf("candy_coins" to updated.candyCoins, "updated_at" to updated.updatedAt)
                ) {
                    filter { eq("user_id", updated.userId) }
                }
            } catch (e: Exception) {
                cloudErrorLogger?.log("profile", "persist_candy_coins", e, "userId=${updated.userId} balance=$newBalance")
                return false
            }
        }
        _profile.value = updated
        saveLocalProfile(updated)
        candyCoinLedgerRepository.addRecord(
            CandyCoinRecord(
                id = UUID.randomUUID().toString(),
                type = if (newBalance >= current.candyCoins) "refund" else "spend",
                amount = newBalance - current.candyCoins,
                balanceAfter = newBalance,
                actorRole = "system",
                targetRole = "eater",
                note = if (newBalance >= current.candyCoins) "取消订单返还" else "点菜消耗",
                createdAt = Instant.now().toString()
            )
        )
        return true
    }

    private fun String.isRecentlySeen(): Boolean {
        val seenAt = runCatching { Instant.parse(this) }.getOrNull() ?: return false
        return Duration.between(seenAt, Instant.now()).abs() <= Duration.ofMinutes(5)
    }

    private fun String.takeIfCloudAvatarUrl(): String? {
        return trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }
}

@kotlinx.serialization.Serializable
private data class PairInviteProfileRow(
    @kotlinx.serialization.SerialName("user_id") val userId: String = "",
    val nickname: String = "",
    @kotlinx.serialization.SerialName("selected_role") val selectedRole: String = ""
)

@kotlinx.serialization.Serializable
private data class RemoteAnniversary(
    @kotlinx.serialization.SerialName("pair_id") val pairId: String,
    @kotlinx.serialization.SerialName("paired_at") val pairedAt: String = "",
    @kotlinx.serialization.SerialName("updated_at") val updatedAt: String = ""
)
