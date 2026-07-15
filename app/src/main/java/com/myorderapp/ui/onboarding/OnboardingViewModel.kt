package com.myorderapp.ui.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.core.worker.CloudImageUploadWorker
import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.data.remote.supabase.SupabaseStorageUploader
import com.myorderapp.data.repository.SupabaseProfileRepository
import com.myorderapp.data.sync.CloudSyncCoordinator
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.model.ROLE_CARETAKER
import com.myorderapp.domain.repository.ProfileRepository
import com.myorderapp.ui.auth.AccountIdentifier
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 1,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val pairCode: String = "",
    val joinPairCode: String = "",
    val pairSkipped: Boolean = false,
    val accountCreated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationComplete: Boolean = false,
    val requiresLoginAfterRegistration: Boolean = false
)

class OnboardingViewModel(
    private val session: SessionManager,
    private val profileRepo: SupabaseProfileRepository,
    private val profileRepository: ProfileRepository,
    private val storageUploader: SupabaseStorageUploader,
    private val cloudSyncCoordinator: CloudSyncCoordinator,
    private val cloudErrorLogger: CloudErrorLogger
) : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // ── Step 1: 账号 ──
    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }
    fun onPasswordChanged(pw: String) {
        _uiState.value = _uiState.value.copy(password = pw, errorMessage = null)
    }
    fun onConfirmPasswordChanged(pw: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = pw, errorMessage = null)
    }

    fun goToStep2() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入邮箱和密码")
            return
        }
        if (state.password.length < 6) {
            _uiState.value = state.copy(errorMessage = "密码至少6位")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(errorMessage = "两次密码输入不一致")
            return
        }
        if (state.accountCreated) {
            _uiState.value = state.copy(step = 2, errorMessage = null)
            return
        }
        createAccountBeforeProfile(state)
    }

    fun goBackToStep1() {
        _uiState.value = _uiState.value.copy(step = 1, errorMessage = null)
    }

    // ── Step 2: 个人资料 ──
    fun onNicknameChanged(nickname: String) {
        if (nickname.length <= 12) _uiState.value = _uiState.value.copy(nickname = nickname)
    }
    fun onAvatarUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(avatarUrl = url)
    }

    fun completeRegistration(context: Context? = null, avatarUri: Uri? = null) {
        if (_uiState.value.nickname.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入昵称")
            return
        }
        saveProfileAndFinishRegistration(context, avatarUri)
    }

    fun goToStep3() = completeRegistration()

    // ── Step 3: 配对 ──
    fun onJoinPairCodeChanged(code: String) {
        if (code.length <= 6) _uiState.value = _uiState.value.copy(joinPairCode = code.uppercase())
    }

    fun generatePairCode() {
        viewModelScope.launch {
            val code = profileRepository.generatePairCode(ROLE_CARETAKER)
            _uiState.value = _uiState.value.copy(pairCode = code)
        }
    }

    fun joinPair() {
        val code = _uiState.value.joinPairCode
        if (code.length != 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "配对码为6位")
            return
        }
        viewModelScope.launch {
            val success = profileRepository.joinPair(code)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = null,
                    registrationComplete = true
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "配对失败，请检查配对码")
            }
        }
    }

    fun skipPairing() {
        _uiState.value = _uiState.value.copy(pairSkipped = true, registrationComplete = true)
    }

    // ── 注册账号：在进入资料页前先创建账号，避免用户填完资料后才发现账号已存在 ──
    private fun createAccountBeforeProfile(state: OnboardingUiState) {
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            try {
                val authEmail = AccountIdentifier.normalizeForAuth(state.email)
                val signUpResult = client.auth.signUpWith(Email) {
                    email = authEmail
                    password = state.password
                }

                val user = client.auth.currentUserOrNull()
                val token = client.auth.currentAccessTokenOrNull()
                val userId = user?.id ?: signUpResult?.id.orEmpty()
                if (token != null && userId.isNotBlank()) {
                    session.setSession(token, userId, "")
                    session.saveEmail(state.email.trim())
                    _uiState.value = _uiState.value.copy(
                        step = 2,
                        accountCreated = true,
                        isLoading = false,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "注册未完成：Supabase 仍要求邮箱验证，请先关闭 Confirm email 后重试"
                    )
                }
            } catch (e: Exception) {
                cloudErrorLogger.log("onboarding", "create_account", e)
                val msg = e.message ?: ""
                val errorMsg = when {
                    isAccountAlreadyExistsMessage(msg) ->
                        "账号已存在，请直接登录"
                    else -> "请求失败，请检查网络或稍后重试"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    private fun saveProfileAndFinishRegistration(context: Context?, avatarUri: Uri?) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val userId = client.auth.currentUserOrNull()?.id.orEmpty()
            if (userId.isBlank()) {
                val appContext = context?.applicationContext
                val localAvatarFile = if (appContext != null && avatarUri != null) {
                    runCatching { copyAvatarToPrivateStorage(appContext, avatarUri) }.getOrNull()
                } else null
                session.savePendingRegistration(
                    email = state.email,
                    nickname = state.nickname,
                    avatarPath = localAvatarFile?.absolutePath.orEmpty()
                )
                _uiState.value = state.copy(
                    isLoading = false,
                    errorMessage = null,
                    registrationComplete = true,
                    requiresLoginAfterRegistration = true
                )
                return@launch
            }

            val appContext = context?.applicationContext
            val localAvatarFile = if (appContext != null && avatarUri != null) {
                runCatching { copyAvatarToPrivateStorage(appContext, avatarUri) }
                    .onFailure { cloudErrorLogger.log("onboarding", "copy_avatar", it, "userId=$userId") }
                    .getOrNull()
            } else {
                null
            }
            val cloudAvatarUrl = if (appContext != null && avatarUri != null) {
                storageUploader.compressAndUploadAvatar(appContext, avatarUri)
                    .publicUrl
                    ?.takeIf { it.isCloudAvatarUrl() }
                    .orEmpty()
            } else {
                state.avatarUrl.takeIf { it.isCloudAvatarUrl() }.orEmpty()
            }
            profileRepository.saveProfile(
                Profile(
                    userId = userId,
                    pairId = DEFAULT_PAIR_ID,
                    nickname = state.nickname.trim(),
                    avatarUrl = cloudAvatarUrl.ifBlank { null },
                    createdAt = Instant.now().toString()
                )
            )
            if (cloudAvatarUrl.isBlank() && localAvatarFile != null && appContext != null && session.isLoggedIn.value) {
                CloudImageUploadWorker.enqueue(
                    appContext,
                    CloudImageUploadWorker.TARGET_AVATAR,
                    userId,
                    Uri.fromFile(localAvatarFile),
                    session.currentUserId,
                    session.currentSessionId
                )
            }
            cloudSyncCoordinator.syncInBackground()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = null,
                registrationComplete = true
            )
        }
    }

    private fun isAccountAlreadyExistsMessage(message: String): Boolean {
        return listOf(
            "already registered",
            "already exists",
            "already been registered",
            "user already registered",
            "user_already_exists",
            "email_exists",
            "email already",
            "duplicate key"
        ).any { message.contains(it, ignoreCase = true) }
    }

    private fun copyAvatarToPrivateStorage(context: Context, uri: Uri): File {
        val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
        val destination = File(avatarsDir, "avatar_${UUID.randomUUID()}.jpg")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("Unable to read selected avatar")
        inputStream.use { input ->
            FileOutputStream(destination).use(input::copyTo)
        }
        return destination
    }

    private fun String.isCloudAvatarUrl(): Boolean = startsWith("http://") || startsWith("https://")

    private companion object {
        const val DEFAULT_PAIR_ID = "00000000-0000-0000-0000-000000000000"
    }
}
