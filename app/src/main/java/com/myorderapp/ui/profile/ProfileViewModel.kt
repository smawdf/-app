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
    val preferences: List<TastePreference> = emptyList(),
    val saveMessage: String? = null
)

data class TastePreference(val emoji: String, val label: String, val value: Boolean)

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                val prefs = profile?.let { p ->
                    listOf(
                        TastePreference("🌶", "重辣", p.tastePrefs.spicy),
                        TastePreference("🍬", "甜口", p.tastePrefs.sweet),
                        TastePreference("🧂", "重口", p.tastePrefs.salty || p.tastePrefs.heavy),
                        TastePreference("🥬", "清淡", p.tastePrefs.light),
                        TastePreference("🍋", "酸口", p.tastePrefs.sour)
                    )
                } ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    profile = profile, preferences = prefs, isLoading = false
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

    fun togglePreference(index: Int) {
        val profile = _uiState.value.profile ?: return
        val prefs = _uiState.value.preferences.toMutableList()
        val updated = prefs[index].copy(value = !prefs[index].value)
        prefs[index] = updated
        val tastePrefs = profile.tastePrefs.copy(
            spicy = prefs[0].value, sweet = prefs[1].value,
            salty = prefs[2].value, light = prefs[3].value, sour = prefs[4].value
        )
        viewModelScope.launch {
            profileRepository.saveProfile(profile.copy(tastePrefs = tastePrefs))
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
