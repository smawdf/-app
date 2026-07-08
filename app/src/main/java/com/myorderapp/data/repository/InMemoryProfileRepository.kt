package com.myorderapp.data.repository

import com.myorderapp.domain.model.DietaryPreference
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.PairInvitePreview
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.model.ROLE_CARETAKER
import com.myorderapp.domain.model.ROLE_EATER
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
    private var selectedRole: String = ""
    private var hasJoinedPair: Boolean = false

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

    override suspend fun addCandyCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        val current = _profile.value ?: Profile()
        _profile.value = current.copy(candyCoins = (current.candyCoins + amount).coerceAtMost(9999))
        return true
    }

    override suspend fun addPartnerCandyCoins(amount: Int): Boolean {
        return addCandyCoins(amount)
    }

    override suspend fun spendCandyCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        val current = _profile.value ?: Profile()
        if (current.candyCoins < amount) return false
        _profile.value = current.copy(candyCoins = current.candyCoins - amount)
        return true
    }

    override suspend fun loadProfile() {
        // In-memory already has a default profile.
    }

    override suspend fun saveSelectedRole(role: String?) {
        selectedRole = role?.takeIf { it == ROLE_CARETAKER || it == ROLE_EATER }.orEmpty()
    }

    override suspend fun generatePairCode(inviterRole: String): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        val current = _profile.value ?: Profile()
        _profile.value = current.copy(pairId = code, pairedAt = current.pairedAt.ifBlank { Instant.now().toString() })
        pendingPairCode = code
        hasJoinedPair = false
        saveSelectedRole(inviterRole)
        return code
    }

    override suspend fun previewPairInvite(code: String): PairInvitePreview? {
        if (code.length != 6) return null
        val inviterRole = selectedRole.takeIf { it == ROLE_CARETAKER || it == ROLE_EATER } ?: return null
        return PairInvitePreview(
            code = code.uppercase(),
            inviterName = _profile.value?.nickname?.ifBlank { "对方" } ?: "对方",
            inviterRole = inviterRole
        )
    }

    override suspend fun joinPair(code: String): Boolean {
        if (code.length != 6) return false
        val current = _profile.value ?: return false
        _profile.value = current.copy(pairId = code.uppercase(), pairedAt = current.pairedAt.ifBlank { Instant.now().toString() })
        pendingPairCode = ""
        hasJoinedPair = true
        return true
    }

    override suspend fun unpair() {
        val current = _profile.value ?: return
        _profile.value = current.copy(pairId = "", pairedAt = "")
        pendingPairCode = ""
        hasJoinedPair = false
    }

    override suspend fun getPairInfo(): PairInfo {
        val pairCode = _profile.value?.pairId ?: ""
        val isPaired = hasJoinedPair && pairCode.isNotBlank()
        return PairInfo(
            partnerName = if (isPaired) "已配对" else "",
            isPaired = isPaired,
            isOnline = isPaired,
            pairCode = pairCode
        )
    }

    override suspend fun touchPresence() {
        val current = _profile.value ?: return
        _profile.value = current.copy(updatedAt = Instant.now().toString())
    }

    override fun isSynced(): Flow<Boolean> = _synced
}
