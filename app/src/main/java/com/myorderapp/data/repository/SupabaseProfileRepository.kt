package com.myorderapp.data.repository

import android.content.Context
import android.net.Uri
import com.myorderapp.core.worker.CloudImageUploadWorker
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
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseProfileRepository(
    private val session: SessionManager,
    private val context: Context,
    private val candyCoinLedgerRepository: CandyCoinLedgerRepository,
    private val cloudErrorLogger: CloudErrorLogger? = null
) : ProfileRepository {

    private companion object {
        const val KEY_PENDING_PAIR_CODE = "pending_pair_code"
        const val DEFAULT_PAIR_ID = "00000000-0000-0000-0000-000000000000"
    }

    private val client = SupabaseClientProvider.client
    private val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow<Profile?>(loadLocalProfile())
    private val _synced = MutableStateFlow(false)
    private val _candyWalletBalance = MutableStateFlow(_profile.value?.candyCoins ?: 66)

    override fun getProfile(): Flow<Profile?> = _profile.asStateFlow()

    override fun observeCandyWalletBalance(): Flow<Int> = _candyWalletBalance.asStateFlow()

    override suspend fun refreshCandyWalletBalance(): Int {
        val current = _profile.value ?: loadLocalProfile()
        val hasFreshSession = !session.isLoggedIn.value || SupabaseClientProvider.ensureFreshAuthSession()
        val balance = when {
            !session.isLoggedIn.value || !hasFreshSession -> current.candyCoins
            current.selectedRole == ROLE_CARETAKER ->
                getPairInfo().partnerCandyCoins ?: _candyWalletBalance.value
            else -> try {
                client.from("profiles").select {
                    filter { eq("user_id", session.currentUserId) }
                }.decodeList<ProfileCandyBalanceRow>()
                    .firstOrNull()
                    ?.candyCoins
                    ?: current.candyCoins
            } catch (error: Exception) {
                cloudErrorLogger?.log("profile", "refresh_candy_wallet", error, "userId=${session.currentUserId}")
                current.candyCoins
            }
        }
        if (current.selectedRole != ROLE_CARETAKER && balance != current.candyCoins) {
            cacheCandyBalanceLocally(current, balance)
        }
        _candyWalletBalance.value = balance
        return balance
    }

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
                persistEditableProfile(normalized)
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
            try { persistEditableProfile(updated); _synced.value = true } catch (e: Exception) {
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
            try { persistEditableProfile(updated); _synced.value = true } catch (e: Exception) {
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
        // The session is updated immediately after a successful pair join; the
        // profile flow can still be one emission behind when this screen opens.
        val pairId = session.currentPairId
            .takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }
            ?: current.pairId
        if (!session.isLoggedIn.value || pairId.isBlank() || pairId == DEFAULT_PAIR_ID) {
            return false
        }
        if (session.currentPairId != pairId) session.setPairId(pairId)
        return try {
            val recordId = UUID.randomUUID().toString()
            val newBalance = try {
                client.postgrest.rpc(
                    function = "add_partner_candy_coins_with_record",
                    parameters = PartnerRechargeParams(
                        amount = amount,
                        recordId = recordId,
                        recordNote = "partner recharge"
                    )
                ).decodeAs<Int>()
            } catch (error: Exception) {
                if (!error.isMissingPartnerRechargeRpc()) throw error
                // Older deployments may have the original RPC but not the atomic
                // variant yet. Keep the server-side update and sync the ledger below.
                cloudErrorLogger?.log(
                    "profile",
                    "add_partner_candy_fallback",
                    error,
                    "pairId=$pairId amount=$amount"
                )
                client.postgrest.rpc(
                    function = "add_partner_candy_coins",
                    parameters = mapOf("amount" to amount)
                ).decodeAs<Int>()
            }
            runCatching {
                candyCoinLedgerRepository.addRecord(
                    CandyCoinRecord(
                        id = recordId,
                        type = "recharge",
                        amount = amount,
                        balanceAfter = newBalance,
                        actorRole = "caretaker",
                        targetRole = "eater",
                        note = "饲养员充值",
                        createdAt = Instant.now().toString()
                    )
                )
            }.onFailure { ledgerError ->
                // The RPC already committed the balance; a later cloud sync can
                // restore the ledger if local Room is temporarily unavailable.
                cloudErrorLogger?.log(
                    "candy_coin",
                    "save_recharge_record",
                    ledgerError,
                    "pairId=$pairId recordId=$recordId"
                )
            }
            _candyWalletBalance.value = newBalance
            true
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "add_partner_candy", e, "pairId=$pairId amount=$amount")
            false
        }
    }

    private fun Throwable.isMissingPartnerRechargeRpc(): Boolean {
        val text = sequenceOf(message, localizedMessage, cause?.message)
            .filterNotNull()
            .joinToString(" ")
        return text.contains("PGRST202", ignoreCase = true) ||
            (text.contains("add_partner_candy_coins_with_record", ignoreCase = true) &&
                (text.contains("schema cache", ignoreCase = true) ||
                    text.contains("could not find", ignoreCase = true) ||
                    text.contains("function not found", ignoreCase = true)))
    }

    override suspend fun spendCandyCoins(amount: Int, transactionId: String): Boolean {
        if (amount <= 0) return true
        val current = _profile.value ?: loadLocalProfile()
        if (session.isLoggedIn.value && current.userId.isNotBlank()) {
            try {
                val remoteBalance = client.postgrest.rpc(
                    function = "spend_eater_candy_coins",
                    parameters = CandyCoinTransactionParams(amount, transactionId)
                ).decodeAs<Int>()
                updateCandyWalletAfterCloudChange(current, remoteBalance)
                return true
            } catch (error: Exception) {
                cloudErrorLogger?.log("profile", "spend_candy_coins_rpc", error, "userId=${current.userId} amount=$amount")
                if (error.isInsufficientCandyCoins()) return false
                throw error
            }
        }
        if (current.selectedRole == ROLE_CARETAKER || current.candyCoins < amount) return false
        return persistCandyCoins(current, current.candyCoins - amount).also { success ->
            if (success) _candyWalletBalance.value = current.candyCoins - amount
        }
    }

    override suspend fun refundCandyCoins(amount: Int, transactionId: String): Boolean {
        if (amount <= 0) return true
        val current = _profile.value ?: loadLocalProfile()
        if (session.isLoggedIn.value && current.userId.isNotBlank()) {
            return try {
                val remoteBalance = client.postgrest.rpc(
                    function = "refund_eater_candy_coins",
                    parameters = CandyCoinTransactionParams(amount, transactionId)
                ).decodeAs<Int>()
                updateCandyWalletAfterCloudChange(current, remoteBalance)
                true
            } catch (error: Exception) {
                cloudErrorLogger?.log("profile", "refund_candy_coins_rpc", error, "userId=${current.userId} amount=$amount")
                false
            }
        }
        if (current.selectedRole == ROLE_CARETAKER) return false
        val newBalance = (current.candyCoins + amount).coerceAtMost(9999)
        return persistCandyCoins(current, newBalance).also { success ->
            if (success) _candyWalletBalance.value = newBalance
        }
    }

    private suspend fun updateCandyWalletAfterCloudChange(current: Profile, newBalance: Int) {
        _candyWalletBalance.value = newBalance
        if (current.selectedRole == ROLE_EATER) {
            cacheCandyBalanceLocally(current, newBalance)
        }
        runCatching { candyCoinLedgerRepository.loadFromCloud() }
            .onFailure { error ->
                cloudErrorLogger?.log("candy_coin", "refresh_records_after_balance_change", error)
            }
    }

    private fun Throwable.isInsufficientCandyCoins(): Boolean {
        return sequenceOf(message, localizedMessage, cause?.message)
            .filterNotNull()
            .any { it.contains("insufficient candy coins", ignoreCase = true) }
    }

    private fun Throwable.isMissingCandyCoinRpc(): Boolean {
        val text = sequenceOf(message, localizedMessage, cause?.message)
            .filterNotNull()
            .joinToString(" ")
        return text.contains("PGRST202", ignoreCase = true) ||
            text.contains("spend_candy_coins", ignoreCase = true) &&
            (text.contains("schema cache", ignoreCase = true) || text.contains("could not find", ignoreCase = true))
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
        return try {
            val result = client.postgrest.rpc(
                function = "create_pair_invite",
                parameters = mapOf("inviter_role" to normalizedRole)
            ).decodeSingle<PairInviteCreationResult>()
            val current = _profile.value ?: loadLocalProfile()
            val updated = normalizeProfile(current).copy(
                pairId = DEFAULT_PAIR_ID,
                selectedRole = result.selectedRole,
                updatedAt = Instant.now().toString()
            )
            _profile.value = updated
            saveLocalProfile(updated)
            session.setPairId(DEFAULT_PAIR_ID)
            prefs.edit().putString(KEY_PENDING_PAIR_CODE, result.pairCode).apply()
            result.pairCode
        } catch (error: Exception) {
            cloudErrorLogger?.log("profile", "create_pair_invite", error, "role=$normalizedRole")
            if (error.message.orEmpty().contains("already paired", ignoreCase = true)) {
                loadFromCloud()
                ""
            } else {
                recoverCreatedPairInvite(normalizedRole)
            }
        }
    }

    private suspend fun recoverCreatedPairInvite(expectedRole: String): String {
        return try {
            val invite = client.from("pair_invites").select {
                filter { eq("inviter_id", session.currentUserId) }
            }.decodeList<RemotePairInvite>()
                .filter { invite ->
                    invite.inviterRole == expectedRole &&
                        invite.usedAt == null &&
                        runCatching { Instant.parse(invite.expiresAt).isAfter(Instant.now()) }.getOrDefault(false)
                }
                .maxByOrNull { it.createdAt }
                ?: return ""
            val current = _profile.value ?: loadLocalProfile()
            val updated = normalizeProfile(current).copy(
                pairId = DEFAULT_PAIR_ID,
                selectedRole = expectedRole,
                updatedAt = Instant.now().toString()
            )
            _profile.value = updated
            saveLocalProfile(updated)
            session.setPairId(DEFAULT_PAIR_ID)
            prefs.edit().putString(KEY_PENDING_PAIR_CODE, invite.code).apply()
            invite.code
        } catch (recoveryError: Exception) {
            cloudErrorLogger?.log("profile", "recover_create_pair_invite", recoveryError, "role=$expectedRole")
            ""
        }
    }

    override suspend fun previewPairInvite(code: String): PairInvitePreview? {
        if (code.length != 6 || !session.isLoggedIn.value) return null
        val normalizedCode = code.uppercase()
        return try {
            val inviter = client.postgrest.rpc(
                function = "preview_pair_invite",
                parameters = mapOf("invite_code" to normalizedCode)
            ).decodeSingle<PairInvitePreviewResult>()
            val inviterRole = inviter.inviterRole.takeIf { it == ROLE_CARETAKER || it == ROLE_EATER } ?: return null
            PairInvitePreview(
                code = normalizedCode,
                inviterName = inviter.inviterName.ifBlank { "对方" },
                inviterRole = inviterRole
            )
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "preview_pair_invite", e, "code=$normalizedCode")
            null
        }
    }

    override suspend fun joinPair(code: String): Boolean {
        if (code.length != 6) return false
        val normalizedCode = code.uppercase()
        return try {
            val result = client.postgrest.rpc(
                function = "join_pair_invite",
                parameters = mapOf("invite_code" to normalizedCode)
            ).decodeSingle<PairJoinResult>()
            val current = _profile.value ?: loadLocalProfile()
            val updated = normalizeProfile(current).copy(
                pairId = result.pairCode,
                selectedRole = result.selectedRole,
                pairedAt = current.pairedAt.ifBlank { Instant.now().toString() },
                updatedAt = Instant.now().toString()
            )
            _profile.value = updated
            saveLocalProfile(updated)
            session.setPairId(result.pairCode)
            prefs.edit().remove(KEY_PENDING_PAIR_CODE).apply()
            true
        } catch (error: Exception) {
            cloudErrorLogger?.log("profile", "join_pair_invite", error, "code=$normalizedCode")
            recoverJoinedPair(normalizedCode)
        }
    }

    private suspend fun recoverJoinedPair(expectedPairCode: String): Boolean {
        return try {
            val snapshot = client.postgrest.rpc("current_pair_snapshot")
                .decodeSingle<PairSnapshot>()
            if (!snapshot.isPaired || !snapshot.pairCode.equals(expectedPairCode, ignoreCase = true)) {
                false
            } else {
                persistPairState(snapshot.toPairInfo())
                prefs.edit().remove(KEY_PENDING_PAIR_CODE).apply()
                true
            }
        } catch (recoveryError: Exception) {
            cloudErrorLogger?.log(
                "pair",
                "recover_join_pair",
                recoveryError,
                "code=$expectedPairCode"
            )
            false
        }
    }

    override suspend fun getPairInfo(): PairInfo {
        if (!session.isLoggedIn.value) return PairInfo()
        return try {
            client.postgrest.rpc("current_pair_snapshot")
                .decodeSingle<PairSnapshot>()
                .toPairInfo()
                .also(::persistPairState)
        } catch (rpcError: Exception) {
            cloudErrorLogger?.log("pair", "current_pair_snapshot", rpcError)
            try {
                loadPairInfoFallback().also(::persistPairState)
            } catch (fallbackError: Exception) {
                cloudErrorLogger?.log("pair", "pair_snapshot_fallback", fallbackError)
                localPairInfo()
            }
        }
    }

    private fun PairSnapshot.toPairInfo(): PairInfo {
        val normalizedPairId = pairCode.ifBlank { DEFAULT_PAIR_ID }
        return PairInfo(
            partnerName = partnerName.ifBlank { if (isPaired) "已配对" else "" },
            partnerAvatarUrl = partnerAvatarUrl.takeIfCloudAvatarUrl(),
            isPaired = isPaired,
            isOnline = partnerUpdatedAt.isRecentlySeen(),
            pairCode = normalizedPairId.takeIf { it != DEFAULT_PAIR_ID }.orEmpty(),
            partnerCandyCoins = partnerCandyCoins,
            noticeId = noticeId,
            noticeMessage = noticeMessage
        )
    }

    private suspend fun loadPairInfoFallback(): PairInfo {
        val ownProfile = client.from("profiles").select {
            filter { eq("user_id", session.currentUserId) }
        }.decodeList<Profile>().firstOrNull() ?: return PairInfo()
        val normalizedPairId = ownProfile.pairId.ifBlank { DEFAULT_PAIR_ID }
        val partner = if (normalizedPairId == DEFAULT_PAIR_ID) null else {
            client.from("profiles").select {
                filter { eq("pair_id", normalizedPairId) }
            }.decodeList<Profile>().firstOrNull { it.userId != session.currentUserId }
        }
        val notice = client.from("pair_events").select {
            filter { eq("recipient_id", session.currentUserId) }
        }.decodeList<RemotePairEvent>()
            .filter { it.readAt.isNullOrBlank() }
            .maxByOrNull { it.createdAt }
        return PairInfo(
            partnerName = partner?.nickname.orEmpty().ifBlank { if (partner != null) "已配对" else "" },
            partnerAvatarUrl = partner?.avatarUrl?.takeIfCloudAvatarUrl(),
            isPaired = partner != null,
            isOnline = partner?.updatedAt.orEmpty().isRecentlySeen(),
            pairCode = normalizedPairId.takeIf { it != DEFAULT_PAIR_ID }.orEmpty(),
            partnerCandyCoins = partner?.candyCoins,
            noticeId = notice?.id.orEmpty(),
            noticeMessage = notice?.message.orEmpty()
        )
    }

    private fun localPairInfo(): PairInfo {
        val localPairId = (_profile.value ?: loadLocalProfile()).pairId
            .ifBlank { session.currentPairId }
            .takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }
            .orEmpty()
        return PairInfo(
            partnerName = if (localPairId.isNotBlank()) "伴侣资料同步中" else "",
            isPaired = localPairId.isNotBlank(),
            pairCode = localPairId
        )
    }

    private fun persistPairState(info: PairInfo) {
        val normalizedPairId = info.pairCode.ifBlank { DEFAULT_PAIR_ID }
        session.setPairId(normalizedPairId)
        val current = _profile.value ?: loadLocalProfile()
        if (current.pairId != normalizedPairId) {
            val updated = current.copy(pairId = normalizedPairId)
            _profile.value = updated
            saveLocalProfile(updated)
        }
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

    override suspend fun unpair(): Boolean {
        val current = _profile.value ?: return false
        if (session.isLoggedIn.value) {
            try {
                client.postgrest.rpc(function = "unpair_current_pair")
            } catch (e: Exception) {
                cloudErrorLogger?.log("profile", "unpair_rpc", e)
                return false
            }
        }
        val updated = current.copy(pairId = DEFAULT_PAIR_ID, selectedRole = "")
        _profile.value = updated
        saveLocalProfile(updated)
        session.setPairId(DEFAULT_PAIR_ID)
        prefs.edit().remove(KEY_PENDING_PAIR_CODE).apply()
        prefs.edit().putString("selected_role", "").apply()
        return true
    }

    override suspend fun acknowledgePairEvent(eventId: String) {
        if (eventId.isBlank() || !session.isLoggedIn.value) return
        try {
            client.from("pair_events").update(mapOf("read_at" to Instant.now().toString())) {
                filter { eq("id", eventId); eq("recipient_id", session.currentUserId) }
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("pair", "ack_pair_event", e, "eventId=$eventId")
        }
    }

    override fun isSynced(): Flow<Boolean> = _synced.asStateFlow()

    suspend fun applyPendingRegistration(email: String, profile: Profile): Profile {
        val pending = session.getPendingRegistration(email) ?: return profile
        val updated = if (pending.nickname.isNotBlank()) profile.copy(nickname = pending.nickname) else profile
        saveProfile(updated)
        val avatarFile = pending.avatarPath.takeIf { it.isNotBlank() }?.let(::File)
        if (avatarFile?.isFile == true) {
            CloudImageUploadWorker.enqueue(
                context.applicationContext,
                CloudImageUploadWorker.TARGET_AVATAR,
                profile.userId,
                Uri.fromFile(avatarFile),
                session.currentUserId,
                session.currentSessionId
            )
        }
        session.clearPendingRegistration()
        return updated
    }

    suspend fun loadFromCloud(): Boolean {
        if (!session.isLoggedIn.value) return true
        if (!SupabaseClientProvider.ensureFreshAuthSession()) {
            cloudErrorLogger?.log(
                "profile",
                "refresh_auth_session",
                IllegalStateException("Supabase auth session could not be refreshed")
            )
            return false
        }
        return try {
            val profiles = client.from("profiles").select {
                filter { eq("user_id", session.currentUserId) }
            }.decodeList<Profile>()
            if (profiles.isNotEmpty()) {
                val cloud = profiles.first()
                val local = loadLocalProfile()
                val anniversary = loadAnniversaryFromCloud(anniversaryScope(cloud))
                val merged = mergeCloudProfileWithLocalFallback(cloud, local)
                    .copy(pairedAt = anniversary.ifBlank { local.pairedAt })
                if (anniversary.isBlank() && merged.pairedAt.isNotBlank()) {
                    runCatching { syncAnniversaryToCloud(merged) }
                        .onFailure { cloudErrorLogger?.log("anniversary", "sync_anniversary", it) }
                }
                val profileFieldsChanged = merged.nickname != cloud.nickname ||
                    merged.avatarUrl?.takeIfCloudAvatarUrl() != cloud.avatarUrl?.takeIfCloudAvatarUrl()
                if (profileFieldsChanged) {
                    client.from("profiles").update(
                        ProfileEditableUpdate(
                            nickname = merged.nickname,
                            avatarUrl = merged.avatarUrl?.takeIfCloudAvatarUrl(),
                            updatedAt = Instant.now().toString()
                        )
                    ) {
                        filter { eq("user_id", session.currentUserId) }
                    }
                }
                _profile.value = merged
                saveLocalProfile(merged)
                session.setPairId(merged.pairId.ifBlank { DEFAULT_PAIR_ID })
                session.saveNickname(merged.nickname)
                session.saveAvatar(merged.avatarUrl ?: "")
                _synced.value = true
            } else {
                val local = normalizeProfile(loadLocalProfile())
                if (local.nickname.isNotBlank() || !local.avatarUrl.isNullOrBlank()) {
                    client.from("profiles").upsert(local.toCloudUpsertPayload(), onConflict = "user_id") { select() }
                    _profile.value = local
                    _synced.value = true
                }
            }
            true
        } catch (e: Exception) {
            cloudErrorLogger?.log("profile", "load_profile", e)
            false
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
        val localNickname = local.nickname.trim()
        val cloudNickname = cloud.nickname.trim()
        val emailNickname = session.getSavedEmail()
            .substringBefore('@')
            .trim()
        val cloudNicknameIsPlaceholder = cloudNickname.isBlank() ||
            (emailNickname.isNotBlank() && cloudNickname == emailNickname)
        return cloud.copy(
            nickname = if (cloudNicknameIsPlaceholder && localNickname.isNotBlank()) {
                localNickname
            } else {
                cloudNickname
            },
            avatarUrl = cloud.avatarUrl?.takeIfCloudAvatarUrl() ?: local.avatarUrl?.takeIfCloudAvatarUrl()
        )
    }

    private suspend fun loadAnniversaryFromCloud(pairId: String): String {
        val normalizedPairId = pairId.takeIf { it.isNotBlank() } ?: return ""
        return try {
            client.from("anniversaries").select {
                filter { eq("pair_id", normalizedPairId) }
            }.decodeList<RemoteAnniversary>().firstOrNull()?.pairedAt.orEmpty()
        } catch (e: Exception) {
            cloudErrorLogger?.log("anniversary", "load_anniversary", e, "pairId=$normalizedPairId")
            ""
        }
    }

    private fun anniversaryScope(profile: Profile): String {
        return profile.pairId.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }
            ?: profile.userId.ifBlank { session.currentUserId }
                .takeIf { it.isNotBlank() }
                ?.let { "user:$it" }
            .orEmpty()
    }

    private suspend fun syncAnniversaryToCloud(profile: Profile) {
        val pairId = anniversaryScope(profile).takeIf { it.isNotBlank() } ?: return
        if (profile.pairedAt.isBlank()) return
        client.from("anniversaries").upsert(
            RemoteAnniversary(
                pairId = pairId,
                pairedAt = profile.pairedAt,
                updatedAt = Instant.now().toString()
            )
        ) { select() }
    }

    private suspend fun persistEditableProfile(profile: Profile) {
        val normalized = normalizeProfile(profile)
        val userId = normalized.userId.ifBlank { session.currentUserId }
        val profileExists = client.from("profiles").select {
            filter { eq("user_id", userId) }
        }.decodeList<ProfileIdRow>().isNotEmpty()
        if (profileExists) {
            client.from("profiles").update(
                ProfileEditableUpdate(
                    nickname = normalized.nickname,
                    avatarUrl = normalized.avatarUrl?.takeIfCloudAvatarUrl(),
                    updatedAt = normalized.updatedAt.ifBlank { Instant.now().toString() }
                )
            ) {
                filter { eq("user_id", userId) }
            }
        } else {
            client.from("profiles").upsert(normalized.toCloudUpsertPayload(), onConflict = "user_id") { select() }
        }
    }

    private fun Profile.toCloudUpsertPayload() = ProfileCloudUpsert(
        userId = userId.ifBlank { session.currentUserId },
        pairId = pairId.ifBlank { session.currentPairId.ifBlank { DEFAULT_PAIR_ID } },
        nickname = nickname,
        avatarUrl = avatarUrl?.takeIfCloudAvatarUrl(),
        candyCoins = candyCoins,
        sessionId = sessionId,
        sessionUpdatedAt = sessionUpdatedAt,
        selectedRole = selectedRole,
        updatedAt = updatedAt.ifBlank { Instant.now().toString() }
    )

    private suspend fun persistCandyCoins(current: Profile, newBalance: Int): Boolean {
        val updated = normalizeProfile(current).copy(candyCoins = newBalance, updatedAt = Instant.now().toString())
        if (session.isLoggedIn.value) {
            try {
                client.from("profiles").update(
                    ProfileCandyBalanceUpdate(
                        candyCoins = updated.candyCoins,
                        updatedAt = updated.updatedAt
                    )
                ) {
                    filter { eq("user_id", updated.userId) }
                }
            } catch (e: Exception) {
                cloudErrorLogger?.log("profile", "persist_candy_coins", e, "userId=${updated.userId} balance=$newBalance")
                return false
            }
        }
        return persistCandyCoinsLocally(current, updated.candyCoins)
    }

    private fun cacheCandyBalanceLocally(current: Profile, newBalance: Int) {
        val updated = normalizeProfile(current).copy(candyCoins = newBalance, updatedAt = Instant.now().toString())
        _profile.value = updated
        saveLocalProfile(updated)
    }

    private suspend fun persistCandyCoinsLocally(current: Profile, newBalance: Int): Boolean {
        cacheCandyBalanceLocally(current, newBalance)
        candyCoinLedgerRepository.addRecord(
            CandyCoinRecord(
                id = UUID.randomUUID().toString(),
                type = if (newBalance >= current.candyCoins) "refund" else "spend",
                amount = newBalance - current.candyCoins,
                balanceAfter = newBalance,
                actorRole = "system",
                targetRole = "eater",
                note = if (newBalance >= current.candyCoins) "取消订单返还" else "点菜消费",
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
private data class ProfileCloudUpsert(
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    @kotlinx.serialization.SerialName("pair_id") val pairId: String,
    val nickname: String,
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String? = null,
    @kotlinx.serialization.SerialName("candy_coins") val candyCoins: Int,
    @kotlinx.serialization.SerialName("session_id") val sessionId: String,
    @kotlinx.serialization.SerialName("session_updated_at") val sessionUpdatedAt: String,
    @kotlinx.serialization.SerialName("selected_role") val selectedRole: String,
    @kotlinx.serialization.SerialName("updated_at") val updatedAt: String
)

@kotlinx.serialization.Serializable
private data class ProfileIdRow(
    @kotlinx.serialization.SerialName("user_id") val userId: String
)

@kotlinx.serialization.Serializable
private data class ProfileEditableUpdate(
    val nickname: String,
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String? = null,
    @kotlinx.serialization.SerialName("updated_at") val updatedAt: String
)

@kotlinx.serialization.Serializable
private data class ProfileCandyBalanceUpdate(
    @kotlinx.serialization.SerialName("candy_coins") val candyCoins: Int,
    @kotlinx.serialization.SerialName("updated_at") val updatedAt: String
)

@kotlinx.serialization.Serializable
private data class ProfileCandyBalanceRow(
    @kotlinx.serialization.SerialName("candy_coins") val candyCoins: Int = 66
)

@kotlinx.serialization.Serializable
private data class PartnerRechargeParams(
    val amount: Int,
    @kotlinx.serialization.SerialName("record_id") val recordId: String,
    @kotlinx.serialization.SerialName("record_note") val recordNote: String
)

@kotlinx.serialization.Serializable
private data class CandyCoinTransactionParams(
    val amount: Int,
    @kotlinx.serialization.SerialName("record_id") val recordId: String
)

@kotlinx.serialization.Serializable
private data class PairInviteCreationResult(
    @kotlinx.serialization.SerialName("pair_code") val pairCode: String,
    @kotlinx.serialization.SerialName("selected_role") val selectedRole: String
)

@kotlinx.serialization.Serializable
private data class RemotePairInvite(
    val code: String,
    @kotlinx.serialization.SerialName("inviter_role") val inviterRole: String,
    @kotlinx.serialization.SerialName("created_at") val createdAt: String,
    @kotlinx.serialization.SerialName("expires_at") val expiresAt: String,
    @kotlinx.serialization.SerialName("used_at") val usedAt: String? = null
)

@kotlinx.serialization.Serializable
private data class PairInvitePreviewResult(
    @kotlinx.serialization.SerialName("pair_code") val pairCode: String,
    @kotlinx.serialization.SerialName("inviter_name") val inviterName: String,
    @kotlinx.serialization.SerialName("inviter_role") val inviterRole: String
)

@kotlinx.serialization.Serializable
private data class PairJoinResult(
    @kotlinx.serialization.SerialName("pair_code") val pairCode: String,
    @kotlinx.serialization.SerialName("selected_role") val selectedRole: String
)

@kotlinx.serialization.Serializable
private data class PairSnapshot(
    @kotlinx.serialization.SerialName("pair_code") val pairCode: String = "",
    @kotlinx.serialization.SerialName("is_paired") val isPaired: Boolean = false,
    @kotlinx.serialization.SerialName("partner_name") val partnerName: String = "",
    @kotlinx.serialization.SerialName("partner_avatar_url") val partnerAvatarUrl: String = "",
    @kotlinx.serialization.SerialName("partner_updated_at") val partnerUpdatedAt: String = "",
    @kotlinx.serialization.SerialName("partner_candy_coins") val partnerCandyCoins: Int? = null,
    @kotlinx.serialization.SerialName("notice_id") val noticeId: String = "",
    @kotlinx.serialization.SerialName("notice_message") val noticeMessage: String = ""
)

@kotlinx.serialization.Serializable
private data class RemotePairEvent(
    val id: String = "",
    val message: String = "",
    @kotlinx.serialization.SerialName("created_at") val createdAt: String = "",
    @kotlinx.serialization.SerialName("read_at") val readAt: String? = null
)

@kotlinx.serialization.Serializable
private data class RemoteAnniversary(
    @kotlinx.serialization.SerialName("pair_id") val pairId: String,
    @kotlinx.serialization.SerialName("paired_at") val pairedAt: String = "",
    @kotlinx.serialization.SerialName("updated_at") val updatedAt: String = ""
)
