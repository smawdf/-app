package com.myorderapp.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.Profile
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
    val pairCode: String = "",
    val joinPairCode: String = "",
    val isLoading: Boolean = true,
    val saveMessage: String? = null
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
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
            profileRepository.getProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    profile = profile, isLoading = false
                )
            }
        }
        viewModelScope.launch {
            val info = profileRepository.getPairInfo()
            _uiState.value = _uiState.value.copy(pairInfo = info)
        }
        viewModelScope.launch {
            profileRepository.isSynced().collect { synced ->
                _uiState.value = _uiState.value.copy(isSynced = synced)
            }
        }
    }

    fun generatePairCode() {
        viewModelScope.launch {
            val code = profileRepository.generatePairCode()
            val info = profileRepository.getPairInfo()
            _uiState.value = _uiState.value.copy(
                pairInfo = info,
                pairCode = code,
                joinPairCode = "",
                saveMessage = "邀请码已生成，可以发给对方"
            )
        }
    }

    fun onJoinPairCodeChanged(code: String) {
        if (code.length <= 6) {
            _uiState.value = _uiState.value.copy(joinPairCode = code.uppercase())
        }
    }

    fun joinPair(code: String) {
        if (code.length != 6) {
            _uiState.value = _uiState.value.copy(saveMessage = "配对码应为6位")
            return
        }
        viewModelScope.launch {
            val success = profileRepository.joinPair(code)
            if (success) {
                val info = profileRepository.getPairInfo()
                _uiState.value = _uiState.value.copy(
                    pairInfo = info,
                    joinPairCode = "",
                    pairCode = "",
                    saveMessage = "配对成功！"
                )
            } else {
                _uiState.value = _uiState.value.copy(saveMessage = "配对失败，请重试")
            }
        }
    }

    fun unpair() {
        viewModelScope.launch {
            profileRepository.unpair()
            val info = profileRepository.getPairInfo()
            _uiState.value = _uiState.value.copy(
                pairInfo = info,
                pairCode = "",
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
                profileRepository.updateAvatar(localPath)
                val current = _uiState.value.profile
                _uiState.value = _uiState.value.copy(
                    profile = (current ?: Profile()).copy(avatarUrl = localPath),
                    saveMessage = "头像已保存"
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
                    avatarPath = copyAvatarToPrivateStorage(context, avatarUri)
                    profileRepository.updateAvatar(avatarPath)
                }
                profileRepository.updateNickname(trimmed)
                val current = _uiState.value.profile ?: Profile()
                _uiState.value = _uiState.value.copy(
                    profile = current.copy(nickname = trimmed, avatarUrl = avatarPath),
                    saveMessage = "资料已保存"
                )
                saveToCloud()
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(saveMessage = "资料保存失败，请重新选择头像后再试")
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
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

    private suspend fun saveToCloud() {
        if (_uiState.value.isSynced) {
            _uiState.value = _uiState.value.copy(saveMessage = "已同步到云端")
        } else {
            _uiState.value = _uiState.value.copy(saveMessage = "已保存（登录后可同步到云端）")
        }
    }
}
