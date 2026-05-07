package com.myorderapp.ui.auth

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

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val mode: String = "login"
)

class AuthViewModel(
    private val authApi: SupabaseAuthApi,
    private val supabaseApi: SupabaseApi,
    private val session: SessionManager,
    private val dishRepo: HybridDishRepository,
    private val profileRepo: SupabaseProfileRepository,
    private val mealRepo: SupabaseMealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (session.isLoggedIn.value) {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
        }
    }

    fun onEmailChanged(email: String) { _uiState.value = _uiState.value.copy(email = email, errorMessage = null) }
    fun onPasswordChanged(pw: String) { _uiState.value = _uiState.value.copy(password = pw, errorMessage = null) }
    fun switchMode() {
        _uiState.value = _uiState.value.copy(
            mode = if (_uiState.value.mode == "login") "register" else "login",
            errorMessage = null
        )
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入邮箱和密码")
            return
        }
        if (state.mode == "register" && state.password.length < 6) {
            _uiState.value = state.copy(errorMessage = "密码至少6位")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val body = AuthBody(state.email, state.password)
                val response = if (state.mode == "register") {
                    authApi.signUp(body, ApiConfig.SUPABASE_ANON_KEY)
                } else {
                    authApi.signIn(body, ApiConfig.SUPABASE_ANON_KEY)
                }

                val token = response.accessToken
                val userId = response.user?.id ?: ""
                if (token.isNotBlank() && userId.isNotBlank()) {
                    // Load or create profile
                    val profile = loadOrCreateProfile(userId, token)
                    val pairId = profile?.pairId ?: ""
                    session.setSession(token, userId, pairId)
                    // Sync all cloud data
                    dishRepo.syncFromCloud()
                    profileRepo.loadFromCloud()
                    mealRepo.syncFromCloud()
                    _uiState.value = _uiState.value.copy(isLoggedIn = true, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "登录失败：未获取到用户信息"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message?.take(100) ?: "请求失败"
                )
            }
        }
    }

    private suspend fun loadOrCreateProfile(userId: String, token: String): Profile? {
        return try {
            val profiles = supabaseApi.getProfile(userId, "Bearer $token")
            profiles.firstOrNull() ?: createDefaultProfile(userId, token)
        } catch (_: Exception) {
            createDefaultProfile(userId, token)
        }
    }

    private suspend fun createDefaultProfile(userId: String, token: String): Profile? {
        return try {
            val profile = Profile(
                userId = userId,
                pairId = "00000000-0000-0000-0000-000000000000",
                nickname = ""
            )
            val created = supabaseApi.createProfile(profile, "Bearer $token")
            created.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { authApi.signOut(session.accessToken, ApiConfig.SUPABASE_ANON_KEY) } catch (_: Exception) { }
            session.clear()
            _uiState.value = AuthUiState()
        }
    }
}
