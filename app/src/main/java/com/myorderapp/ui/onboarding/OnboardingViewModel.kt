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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
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
    private val mealRepo: SupabaseMealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) { _uiState.value = _uiState.value.copy(email = email, errorMessage = null) }
    fun onPasswordChanged(pw: String) { _uiState.value = _uiState.value.copy(password = pw, errorMessage = null) }
    fun onConfirmPasswordChanged(pw: String) { _uiState.value = _uiState.value.copy(confirmPassword = pw, errorMessage = null) }

    fun register() {
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

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val body = AuthBody(state.email, state.password)
                val response = authApi.signUp(body, ApiConfig.SUPABASE_ANON_KEY)

                val token = response.accessToken
                val userId = response.user?.id ?: ""
                if (token.isNotBlank() && userId.isNotBlank()) {
                    loadOrCreateProfile(userId, token)
                    session.setSession(token, userId, "")
                    dishRepo.syncFromCloud()
                    profileRepo.loadFromCloud()
                    mealRepo.syncFromCloud()
                    _uiState.value = _uiState.value.copy(isLoading = false, registrationComplete = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "注册失败：请稍后重试"
                    )
                }
            } catch (e: Exception) {
                val msg = e.message ?: "请求失败"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (msg.contains("already registered", ignoreCase = true))
                        "该邮箱已被注册，请直接登录" else msg.take(100)
                )
            }
        }
    }

    private suspend fun loadOrCreateProfile(userId: String, token: String) {
        try {
            val profiles = supabaseApi.getProfile(userId, "Bearer $token")
            if (profiles.isEmpty()) {
                createDefaultProfile(userId, token)
            }
        } catch (_: Exception) {
            createDefaultProfile(userId, token)
        }
    }

    private suspend fun createDefaultProfile(userId: String, token: String) {
        try {
            val profile = Profile(
                userId = userId,
                pairId = "00000000-0000-0000-0000-000000000000",
                nickname = ""
            )
            supabaseApi.createProfile(profile, "Bearer $token")
        } catch (_: Exception) { }
    }
}
