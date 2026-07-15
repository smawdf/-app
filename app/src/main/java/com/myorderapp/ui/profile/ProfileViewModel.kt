package com.myorderapp.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.core.worker.CloudImageUploadWorker
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseStorageUploader
import com.myorderapp.data.sync.CloudSyncCoordinator
import com.myorderapp.data.sync.CloudSyncState
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.PairInvitePreview
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.model.ROLE_CARETAKER
import com.myorderapp.domain.model.ROLE_EATER
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ProfileUiState(
    val profile: Profile? = null,
    val pairInfo: PairInfo = PairInfo(),
    val isLoggedIn: Boolean = false,
    val isSynced: Boolean = false,
    val cloudSyncState: CloudSyncState = CloudSyncState(),
    val walletBalance: Int = 66,
    val pairCode: String = "",
    val joinPairCode: String = "",
    val invitePreview: PairInvitePreview? = null,
    val isLoading: Boolean = true,
    val saveMessage: String? = null
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager,
    private val storageUploader: SupabaseStorageUploader,
    private val cloudSyncCoordinator: CloudSyncCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // 登录状态直接来自 SessionManager
        val loggedIn = sessionManager.isLoggedIn.value
        // 本地预填充 — 避免加载闪烁
        val localNick = sessionManager.getSavedNickname()
        val localAvatar = sessionManager.getSavedAvatar()
        _uiState.value = _uiState.value.copy(
            isLoggedIn = loggedIn,
            profile = if (localNick.isNotBlank() || localAvatar.isNotBlank())
                com.myorderapp.domain.model.Profile(nickname = localNick, avatarUrl = localAvatar.ifBlank { null })
            else null
        )
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn2 ->
                _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn2)
            }
        }
        viewModelScope.launch {
            profileRepository.loadProfile()
            refreshPairInfo()
            profileRepository.getProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    profile = profile, isLoading = false
                )
            }
        }
        viewModelScope.launch {
            profileRepository.observeCandyWalletBalance().collect { balance ->
                _uiState.value = _uiState.value.copy(walletBalance = balance)
            }
        }
        viewModelScope.launch {
            runCatching { profileRepository.refreshCandyWalletBalance() }
        }
        viewModelScope.launch {
            profileRepository.isSynced().collect { synced ->
                _uiState.value = _uiState.value.copy(isSynced = synced)
            }
        }
        viewModelScope.launch {
            cloudSyncCoordinator.state.collect { syncState ->
                _uiState.value = _uiState.value.copy(cloudSyncState = syncState)
            }
        }
    }

    fun retryCloudSync() {
        viewModelScope.launch { cloudSyncCoordinator.syncAll() }
    }

    fun refreshPairState() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
        viewModelScope.launch {
            profileRepository.loadProfile()
            refreshPairInfo()
            runCatching { profileRepository.refreshCandyWalletBalance() }
        }
    }

    fun saveSelectedRole(role: String?) {
        if (role != ROLE_CARETAKER && role != ROLE_EATER) return
        viewModelScope.launch {
            profileRepository.saveSelectedRole(role)
        }
    }

    fun generatePairCode(inviterRole: String?) {
        if (inviterRole != ROLE_CARETAKER && inviterRole != ROLE_EATER) {
            _uiState.value = _uiState.value.copy(saveMessage = "请先在首页选择饲养员或吃货")
            return
        }
        viewModelScope.launch {
            profileRepository.loadProfile()
            val currentInfo = profileRepository.getPairInfo()
            if (currentInfo.isPaired) {
                _uiState.value = _uiState.value.copy(
                    pairInfo = currentInfo,
                    pairCode = "",
                    invitePreview = null,
                    saveMessage = "你们已经绑定，无需再次生成邀请码"
                )
                return@launch
            }
            profileRepository.saveSelectedRole(inviterRole)
            val code = profileRepository.generatePairCode(inviterRole)
            val info = profileRepository.getPairInfo()
            _uiState.value = _uiState.value.copy(
                pairInfo = info,
                pairCode = code,
                joinPairCode = "",
                invitePreview = null,
                saveMessage = when {
                    info.isPaired -> "你们已经绑定，无需再次生成邀请码"
                    code.isNotBlank() -> "邀请码已生成，可以发给对方"
                    else -> "邀请码生成失败，请检查网络后重试"
                }
            )
        }
    }

    fun onJoinPairCodeChanged(code: String) {
        if (code.length <= 6) {
            _uiState.value = _uiState.value.copy(
                joinPairCode = code.uppercase(),
                invitePreview = null,
                saveMessage = null
            )
        }
    }

    fun previewPairInvite(code: String) {
        if (code.length != 6) {
            _uiState.value = _uiState.value.copy(saveMessage = "配对码应为6位", invitePreview = null)
            return
        }
        viewModelScope.launch {
            val preview = profileRepository.previewPairInvite(code)
            _uiState.value = if (preview != null) {
                _uiState.value.copy(invitePreview = preview, saveMessage = null)
            } else {
                _uiState.value.copy(
                    invitePreview = null,
                    saveMessage = "未找到有效邀请，请确认对方已选择身份并生成邀请码"
                )
            }
        }
    }

    fun joinPair(code: String, inviteeRole: String? = null, onSuccess: (String) -> Unit = {}) {
        if (code.length != 6) {
            _uiState.value = _uiState.value.copy(saveMessage = "配对码应为6位")
            return
        }
        val roleToSave = inviteeRole ?: _uiState.value.invitePreview?.inviteeRole
        if (roleToSave != ROLE_CARETAKER && roleToSave != ROLE_EATER) {
            _uiState.value = _uiState.value.copy(saveMessage = "请先确认邀请信息")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saveMessage = null)
            val success = profileRepository.joinPair(code)
            if (success) {
                profileRepository.saveSelectedRole(roleToSave)
                onSuccess(roleToSave)
                val info = profileRepository.getPairInfo()
                runCatching { profileRepository.refreshCandyWalletBalance() }
                _uiState.value = _uiState.value.copy(
                    pairInfo = info,
                    joinPairCode = "",
                    pairCode = "",
                    invitePreview = null,
                    saveMessage = "配对成功！"
                )
            } else {
                val info = profileRepository.getPairInfo()
                if (info.isPaired) runCatching { profileRepository.refreshCandyWalletBalance() }
                _uiState.value = if (info.isPaired) {
                    onSuccess(roleToSave)
                    _uiState.value.copy(
                        pairInfo = info,
                        joinPairCode = "",
                        pairCode = "",
                        invitePreview = null,
                        saveMessage = "绑定成功"
                    )
                } else {
                    _uiState.value.copy(
                        pairInfo = info,
                        invitePreview = null,
                        saveMessage = "绑定失败，邀请码可能已失效，请重新生成后再试"
                    )
                }
            }
        }
    }

    fun unpair() {
        viewModelScope.launch {
            val success = profileRepository.unpair()
            if (!success) {
                _uiState.value = _uiState.value.copy(saveMessage = "解除绑定失败，请检查网络后重试")
                return@launch
            }
            cloudSyncCoordinator.syncAll()
            val info = profileRepository.getPairInfo()
            runCatching { profileRepository.refreshCandyWalletBalance() }
            _uiState.value = _uiState.value.copy(
                pairInfo = info,
                pairCode = "",
                invitePreview = null,
                saveMessage = "已解除配对"
            )
        }
    }

    fun addCandyCoins(amount: Int) {
        viewModelScope.launch {
            val success = profileRepository.addPartnerCandyCoins(amount)
            val info = profileRepository.getPairInfo()
            _uiState.value = _uiState.value.copy(
                pairInfo = info,
                saveMessage = if (success) {
                    "已给吃货增加 $amount 枚糖糖币"
                } else {
                    "加糖失败，请确认已登录、已绑定，并已执行糖糖币数据库脚本"
                }
            )
        }
    }

    fun updateNickname(nickname: String) {
        val trimmed = nickname.trim()
        if (!validateNickname(trimmed)) return
        val current = _uiState.value.profile
        _uiState.value = _uiState.value.copy(
            profile = (current ?: Profile()).copy(nickname = trimmed),
            saveMessage = "昵称已更新"
        )
        viewModelScope.launch {
            profileRepository.updateNickname(trimmed)
            saveToCloud()
        }
    }

    fun saveAvatarUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val avatarsDir = File(context.filesDir, "avatars")
                avatarsDir.mkdirs()
                val fileName = "avatar_${UUID.randomUUID()}.jpg"
                val destFile = File(avatarsDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val localPath = destFile.absolutePath
                val cloudAvatarUrl = uploadAvatar(context, uri)
                val avatarUrl = cloudAvatarUrl ?: localPath
                cloudAvatarUrl?.let { profileRepository.updateAvatar(it) }
                if (cloudAvatarUrl == null && sessionManager.isLoggedIn.value) {
                    CloudImageUploadWorker.enqueue(
                        context,
                        CloudImageUploadWorker.TARGET_AVATAR,
                        sessionManager.currentUserId,
                        Uri.fromFile(destFile),
                        sessionManager.currentUserId,
                        sessionManager.currentSessionId
                    )
                }
                val current = _uiState.value.profile
                _uiState.value = _uiState.value.copy(
                    profile = (current ?: Profile()).copy(avatarUrl = avatarUrl),
                    saveMessage = if (avatarUrl == localPath) "头像已保存到本机，云端上传失败" else "头像已同步"
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(saveMessage = "头像保存失败")
            }
        }
    }

    fun saveProfileEdits(context: Context, nickname: String, avatarUri: Uri?) {
        val trimmed = nickname.trim()
        if (!validateNickname(trimmed)) return
        viewModelScope.launch {
            try {
                var avatarPath = _uiState.value.profile?.avatarUrl
                if (avatarUri != null) {
                    val localAvatarPath = copyAvatarToPrivateStorage(context, avatarUri)
                    val cloudAvatarUrl = uploadAvatar(context, avatarUri)
                    avatarPath = cloudAvatarUrl ?: localAvatarPath
                    cloudAvatarUrl?.let { profileRepository.updateAvatar(it) }
                    if (cloudAvatarUrl == null && sessionManager.isLoggedIn.value) {
                        CloudImageUploadWorker.enqueue(
                            context,
                            CloudImageUploadWorker.TARGET_AVATAR,
                            sessionManager.currentUserId,
                            Uri.fromFile(File(localAvatarPath)),
                            sessionManager.currentUserId,
                            sessionManager.currentSessionId
                        )
                    }
                }
                profileRepository.updateNickname(trimmed)
                val current = _uiState.value.profile ?: Profile()
                _uiState.value = _uiState.value.copy(
                    profile = current.copy(nickname = trimmed, avatarUrl = avatarPath),
                    saveMessage = if (avatarUri != null && avatarPath?.startsWith("http") == false) {
                        "资料已保存，头像云端上传失败"
                    } else {
                        "资料已同步"
                    }
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(saveMessage = "资料保存失败，请重新选择头像后再试")
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
    }

    private suspend fun refreshPairInfo() {
        runCatching { profileRepository.getPairInfo() }
            .onSuccess { info ->
                _uiState.value = _uiState.value.copy(
                    pairInfo = info,
                    pairCode = if (info.isPaired) "" else _uiState.value.pairCode,
                    invitePreview = if (info.isPaired) null else _uiState.value.invitePreview
                )
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(
                    saveMessage = "绑定资料同步失败，请稍后重试"
                )
            }
    }

    private fun validateNickname(nickname: String): Boolean {
        return when {
            nickname.isBlank() -> {
                _uiState.value = _uiState.value.copy(saveMessage = "请输入昵称")
                false
            }
            nickname.length > 12 -> {
                _uiState.value = _uiState.value.copy(saveMessage = "昵称最多 12 个字")
                false
            }
            else -> true
        }
    }

    private fun copyAvatarToPrivateStorage(context: Context, uri: Uri): String {
        val avatarsDir = File(context.filesDir, "avatars")
        avatarsDir.mkdirs()
        val fileName = "avatar_${UUID.randomUUID()}.jpg"
        val destFile = File(avatarsDir, fileName)
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("无法读取头像")
        inputStream.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile.absolutePath
    }

    private suspend fun uploadAvatar(context: Context, uri: Uri): String? {
        if (!sessionManager.isLoggedIn.value) return null
        return storageUploader.compressAndUploadAvatar(context, uri).publicUrl?.takeIf { it.isCloudAvatarUrl() }
    }

    private fun String.isCloudAvatarUrl(): Boolean = startsWith("http://") || startsWith("https://")

    private suspend fun saveToCloud() {
        if (_uiState.value.isSynced) {
            _uiState.value = _uiState.value.copy(saveMessage = "已同步到云端")
        } else {
            _uiState.value = _uiState.value.copy(saveMessage = "已保存（登录后可同步到云端）")
        }
    }
}
