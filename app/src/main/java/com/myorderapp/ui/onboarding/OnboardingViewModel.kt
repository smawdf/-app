package com.myorderapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.ApiConfig
import com.myorderapp.data.remote.supabase.AuthBody
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseAuthApi
import com.myorderapp.data.remote.supabase.SupabaseApi
import com.myorderapp.data.repository.HybridDishRepository
import com.myorderapp.data.repository.SupabaseMealRepository
import com.myorderapp.data.repository.SupabaseProfileRepository
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 1,                     // 1=账号, 2=个人资料, 3=配对
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val pairCode: String = "",             // 生成的配对码
    val joinPairCode: String = "",         // 输入的对方配对码
    val pairSkipped: Boolean = false,      // 跳过配对
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationComplete: Boolean = false
)

class OnboardingViewModel(
    private val authApi: SupabaseAuthApi,
    private val supabaseApi: SupabaseApi,
    private val session: SessionManager,
    private val dishRepo: HybridDishRepository,
    private val profileRepo: SupabaseProfileRepository,
    private val mealRepo: SupabaseMealRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

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
        _uiState.value = state.copy(step = 2, errorMessage = null)
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

    fun goToStep3() {
        if (_uiState.value.nickname.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入昵称")
            return
        }
        // 先注册账号，再进入配对步骤
        registerAccount()
    }

    // ── Step 3: 配对 ──
    fun onJoinPairCodeChanged(code: String) {
        if (code.length <= 6) _uiState.value = _uiState.value.copy(joinPairCode = code.uppercase())
    }

    fun generatePairCode() {
        viewModelScope.launch {
            val code = profileRepository.generatePairCode()
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

    // ── 注册账号 ──
    private fun registerAccount() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            try {
                val body = AuthBody(state.email, state.password)
                val response = authApi.signUp(body, ApiConfig.SUPABASE_ANON_KEY)

                val token = response.accessToken
                val userId = response.user?.id ?: ""
                if (token.isNotBlank() && userId.isNotBlank()) {
                    createProfileWithDetails(userId, token)
                    session.setSession(token, userId, "")
                    session.saveEmail(state.email)
                    writeSessionId(userId, token)
                    dishRepo.syncFromCloud()
                    profileRepo.loadFromCloud()
                    mealRepo.syncFromCloud()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        step = 3,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "注册失败：请稍后重试"
                    )
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val errorMsg = when {
                    msg.contains("already registered", ignoreCase = true) ||
                    msg.contains("already exists", ignoreCase = true) ->
                        "该邮箱已注册，请直接登录"
                    else -> msg.ifBlank { "请求失败，请稍后重试" }.take(100)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    private suspend fun writeSessionId(userId: String, token: String) {
        try {
            supabaseApi.updateProfile(
                userId = userId,
                fields = mapOf("session_id" to session.currentSessionId),
                token = "Bearer $token"
            )
        } catch (_: Exception) { }
    }

    private suspend fun createProfileWithDetails(userId: String, token: String) {
        val state = _uiState.value
        try {
            val profile = Profile(
                userId = userId,
                pairId = "00000000-0000-0000-0000-000000000000",
                nickname = state.nickname,
                avatarUrl = state.avatarUrl
            )
            supabaseApi.createProfile(profile, "Bearer $token")
        } catch (_: Exception) {
            // Fallback: try update if already exists
            try {
                supabaseApi.updateProfile(
                    userId = userId,
                    fields = mapOf(
                        "nickname" to state.nickname,
                        "avatar_url" to state.avatarUrl
                    ),
                    token = "Bearer $token"
                )
            } catch (_: Exception) { }
        }
    }
}
