package com.myorderapp.ui.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.data.remote.supabase.SupabaseStorageUploader
import com.myorderapp.data.repository.HybridDishRepository
import com.myorderapp.data.repository.SupabaseProfileRepository
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.model.ROLE_CARETAKER
import com.myorderapp.domain.repository.ProfileRepository
import com.myorderapp.ui.auth.AccountIdentifier
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import java.time.Instant
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
    val registrationComplete: Boolean = false
)

class OnboardingViewModel(
    private val session: SessionManager,
    private val dishRepo: HybridDishRepository,
    private val profileRepo: SupabaseProfileRepository,
    private val profileRepository: ProfileRepository,
    private val storageUploader: SupabaseStorageUploader,
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
                    profileRepo.claimCurrentDeviceSession()
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
                        errorMessage = "账号已创建。如果你的邮箱需要验证，请先打开确认邮件；确认后再返回登录。"
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
                _uiState.value = state.copy(
                    isLoading = false,
                    errorMessage = "登录状态已失效，请返回上一步重新注册"
                )
                return@launch
            }

            val cloudAvatarUrl = if (context != null && avatarUri != null) {
                storageUploader.compressAndUploadAvatar(context, avatarUri).publicUrl?.takeIf { it.isCloudAvatarUrl() }.orEmpty()
            } else {
                state.avatarUrl.takeIf { it.isCloudAvatarUrl() }.orEmpty()
            }
            createProfileWithDetails(userId, cloudAvatarUrl)
            session.saveNickname(state.nickname)
            session.saveAvatar(cloudAvatarUrl)
            dishRepo.syncFromCloud()
            profileRepo.loadFromCloud()
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

    private suspend fun createProfileWithDetails(userId: String, avatarUrl: String) {
        val state = _uiState.value
        try {
            client.from("profiles").upsert(
                mapOf(
                    "user_id" to userId,
                    "pair_id" to "00000000-0000-0000-0000-000000000000",
                    "nickname" to state.nickname,
                    "avatar_url" to avatarUrl.ifBlank { null },
                    "selected_role" to "",
                    "updated_at" to Instant.now().toString()
                )
            ) { select() }
        } catch (e: Exception) {
            cloudErrorLogger.log("onboarding", "create_profile", e, "userId=$userId")
        }
    }

    private fun String.isCloudAvatarUrl(): Boolean = startsWith("http://") || startsWith("https://")
}
