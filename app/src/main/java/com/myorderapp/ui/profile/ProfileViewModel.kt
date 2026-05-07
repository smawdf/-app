package com.myorderapp.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isSynced: Boolean = false,
    val pairCode: String = "",
    val joinPairCode: String = "",
    val isLoading: Boolean = true,
    val customTags: List<String> = emptyList(),
    val newTag: String = "",
    val saveMessage: String? = null
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                val tags = profile?.tastePrefs?.custom ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    profile = profile, customTags = tags, isLoading = false
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
            _uiState.value = _uiState.value.copy(pairCode = code, joinPairCode = "")
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
                    saveMessage = "配对成功！💕"
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

    fun onNewTagChanged(tag: String) {
        _uiState.value = _uiState.value.copy(newTag = tag)
    }

    fun addTag() {
        val tag = _uiState.value.newTag.trim()
        if (tag.isBlank() || tag.length > 6) return
        val tags = _uiState.value.customTags.toMutableList()
        if (tags.contains(tag)) {
            _uiState.value = _uiState.value.copy(newTag = "", saveMessage = "标签已存在")
            return
        }
        tags.add(tag)
        _uiState.value = _uiState.value.copy(customTags = tags, newTag = "", saveMessage = null)
        saveTagsToCloud(tags)
    }

    fun removeTag(tag: String) {
        val tags = _uiState.value.customTags.toMutableList()
        tags.remove(tag)
        _uiState.value = _uiState.value.copy(customTags = tags)
        saveTagsToCloud(tags)
    }

    private fun saveTagsToCloud(tags: List<String>) {
        val profile = _uiState.value.profile ?: return
        val newTastePrefs = profile.tastePrefs.copy(custom = tags)
        viewModelScope.launch {
            profileRepository.saveProfile(profile.copy(tastePrefs = newTastePrefs))
            saveToCloud()
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            profileRepository.updateNickname(nickname)
            saveToCloud()
        }
    }

    fun updateAvatar(avatarUrl: String) {
        if (avatarUrl.isBlank()) return
        viewModelScope.launch {
            profileRepository.updateAvatar(avatarUrl)
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
                _uiState.value = _uiState.value.copy(saveMessage = "头像已保存 ✓")
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(saveMessage = "头像保存失败")
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
    }

    private suspend fun saveToCloud() {
        _uiState.value = _uiState.value.copy(saveMessage = "已保存 ✓")
    }
}
