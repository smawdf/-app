package com.myorderapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.data.repository.HybridDishRepository
import com.myorderapp.data.repository.SupabaseProfileRepository
import com.myorderapp.domain.model.Profile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

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
    private val session: SessionManager,
    private val dishRepo: HybridDishRepository,
    private val profileRepo: SupabaseProfileRepository
) : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val savedEmail = session.getSavedEmail()
        _uiState.value = _uiState.value.copy(email = savedEmail)
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
                if (state.mode == "register") {
                    client.auth.signUpWith(Email) {
                        email = state.email
                        password = state.password
                    }
                } else {
                    client.auth.signInWith(Email) {
                        email = state.email
                        password = state.password
                    }
                }

                val user = client.auth.currentUserOrNull()
                val token = client.auth.currentAccessTokenOrNull()
                val userId = user?.id ?: ""
                if (token != null && userId.isNotBlank()) {
                    val profile = loadOrCreateProfile(userId)
                    val pairId = profile?.pairId ?: ""
                    session.setSession(token, userId, pairId)
                    session.saveEmail(state.email)
                    if (profile != null) {
                        session.saveNickname(profile.nickname)
                        session.saveAvatar(profile.avatarUrl ?: "")
                    }
                    dishRepo.syncFromCloud()
                    profileRepo.loadFromCloud()
                    _uiState.value = _uiState.value.copy(isLoggedIn = true, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "登录失败：未获取到用户信息"
                    )
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val errorMsg = when {
                    msg.contains("Invalid login credentials", ignoreCase = true) ||
                    msg.contains("User not found", ignoreCase = true) ||
                    msg.contains("invalid_grant", ignoreCase = true) ->
                        "该邮箱尚未注册，请先创建账号"
                    msg.contains("Email not confirmed", ignoreCase = true) ||
                    msg.contains("email_not_confirmed", ignoreCase = true) ->
                        "邮箱未验证，请检查邮件确认链接"
                    msg.contains("Invalid password", ignoreCase = true) ||
                    msg.contains("wrong password", ignoreCase = true) ->
                        "密码错误，请重试"
                    msg.contains("already registered", ignoreCase = true) ||
                    msg.contains("already exists", ignoreCase = true) ->
                        "该邮箱已注册，请直接登录"
                    else -> "请求失败，请稍后重试"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    private suspend fun loadOrCreateProfile(userId: String): Profile? {
        return try {
            val profiles = client.from("profiles").select {
                filter { eq("user_id", userId) }
            }.decodeList<Profile>()
            profiles.firstOrNull() ?: run {
                val localNick = session.getSavedNickname()
                val localAvatar = session.getSavedAvatar()
                val profile = Profile(
                    userId = userId,
                    pairId = "00000000-0000-0000-0000-000000000000",
                    nickname = localNick,
                    avatarUrl = localAvatar.ifBlank { null }
                )
                client.from("profiles").insert(profile) { select() }
                profile
            }
        } catch (_: Exception) {
            Profile(
                userId = userId,
                nickname = session.getSavedNickname(),
                avatarUrl = session.getSavedAvatar().ifBlank { null }
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            try { client.auth.signOut() } catch (_: Exception) { }
            session.clear()
            _uiState.value = AuthUiState()
            onLoggedOut()
        }
    }
}
