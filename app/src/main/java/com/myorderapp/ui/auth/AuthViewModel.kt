package com.myorderapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.data.repository.HybridDishRepository
import com.myorderapp.data.repository.SupabaseProfileRepository
import com.myorderapp.domain.model.Profile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.parseSessionFromUrl
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val rememberCredentials: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isResetEmailSent: Boolean = false,
    val isPasswordResetComplete: Boolean = false,
    val canSwitchDeviceByEmail: Boolean = false,
    val isDeviceSwitchComplete: Boolean = false,
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
        val rememberCredentials = session.isRememberCredentialsEnabled()
        val savedPassword = session.getSavedPassword()
        _uiState.value = _uiState.value.copy(
            email = savedEmail,
            password = savedPassword,
            rememberCredentials = rememberCredentials
        )
        if (session.isLoggedIn.value) {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
        }
    }

    fun onEmailChanged(email: String) { _uiState.value = _uiState.value.copy(email = email, errorMessage = null) }
    fun onPasswordChanged(pw: String) { _uiState.value = _uiState.value.copy(password = pw, errorMessage = null) }
    fun onRememberCredentialsChanged(remember: Boolean) {
        val state = _uiState.value
        _uiState.value = state.copy(rememberCredentials = remember)
        if (!remember) {
            session.saveRememberedCredentials(state.email, "", false)
        }
    }

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
                val authEmail = AccountIdentifier.normalizeForAuth(state.email)
                if (state.mode == "register") {
                    client.auth.signUpWith(Email) {
                        email = authEmail
                        password = state.password
                    }
                } else {
                    client.auth.signInWith(Email) {
                        email = authEmail
                        password = state.password
                    }
                }

                val user = client.auth.currentUserOrNull()
                val token = client.auth.currentAccessTokenOrNull()
                val userId = user?.id ?: ""
                if (token != null && userId.isNotBlank()) {
                    val profile = loadOrCreateProfile(userId)
                    if (profile == null) {
                        try { client.auth.signOut() } catch (_: Exception) { }
                        session.clear()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "登录失败：无法读取账号设备状态，请稍后重试"
                        )
                        return@launch
                    }
                    val pairId = profile.pairId
                    session.setSession(token, userId, pairId)
                    if (!profileRepo.canStartDeviceSession(profile)) {
                        try { client.auth.signOut() } catch (_: Exception) { }
                        session.clear()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            canSwitchDeviceByEmail = AccountIdentifier.isRealEmail(state.email),
                            errorMessage = if (AccountIdentifier.isRealEmail(state.email)) {
                                "账号已在其他设备登录。请先在原设备退出，或通过邮箱验证切换到当前设备。"
                            } else {
                                "账号已在其他设备登录，请先在原设备退出登录"
                            }
                        )
                        return@launch
                    }
                    val claimedProfile = profileRepo.claimCurrentDeviceSession(profile)
                    if (claimedProfile == null) {
                        try { client.auth.signOut() } catch (_: Exception) { }
                        session.clear()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "登录失败：无法绑定当前设备，请稍后重试"
                        )
                        return@launch
                    }
                    session.saveRememberedCredentials(
                        email = state.email.trim(),
                        password = state.password,
                        remember = state.rememberCredentials
                    )
                    session.saveNickname(claimedProfile.nickname)
                    session.saveAvatar(claimedProfile.avatarUrl ?: "")
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
                        "账号或密码不正确；如果刚注册，请先确认邮箱后再登录"
                    msg.contains("Email not confirmed", ignoreCase = true) ||
                    msg.contains("email_not_confirmed", ignoreCase = true) ->
                        "邮箱未验证，请检查邮件确认链接"
                    msg.contains("Invalid password", ignoreCase = true) ||
                    msg.contains("wrong password", ignoreCase = true) ->
                        "密码错误，请重试"
                    isAccountAlreadyExistsMessage(msg) ->
                        "账号已存在，请直接登录"
                    else -> "请求失败，请稍后重试"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    fun sendPasswordResetEmail(email: String = _uiState.value.email) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入注册邮箱")
            return
        }
        if (!AccountIdentifier.isRealEmail(normalizedEmail)) {
            _uiState.value = _uiState.value.copy(errorMessage = "手机号或账号暂不支持邮箱找回，请使用注册密码登录")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isResetEmailSent = false)
            try {
                client.auth.resetPasswordForEmail(
                    email = normalizedEmail,
                    redirectUrl = PASSWORD_RESET_REDIRECT_URL
                )
                session.saveEmail(normalizedEmail)
                _uiState.value = _uiState.value.copy(
                    email = normalizedEmail,
                    isLoading = false,
                    isResetEmailSent = true,
                    errorMessage = "重置邮件已发送，请打开邮箱继续修改密码"
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "发送失败，请确认邮箱后重试"
                )
            }
        }
    }

    fun sendDeviceSwitchEmail(email: String = _uiState.value.email) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || !AccountIdentifier.isRealEmail(normalizedEmail)) {
            _uiState.value = _uiState.value.copy(errorMessage = "请使用注册邮箱验证切换设备")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isResetEmailSent = false)
            try {
                client.auth.resetPasswordForEmail(
                    email = normalizedEmail,
                    redirectUrl = DEVICE_SWITCH_REDIRECT_URL
                )
                session.saveEmail(normalizedEmail)
                _uiState.value = _uiState.value.copy(
                    email = normalizedEmail,
                    isLoading = false,
                    isResetEmailSent = true,
                    canSwitchDeviceByEmail = true,
                    errorMessage = "验证邮件已发送，请在当前设备打开邮件链接完成切换"
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "发送失败，请确认邮箱后重试"
                )
            }
        }
    }

    fun switchDeviceFromDeepLink(deepLink: String) {
        if (deepLink.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "验证链接无效，请重新发送邮件")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isDeviceSwitchComplete = false)
            try {
                val recoverySession = client.auth.parseSessionFromUrl(deepLink)
                client.auth.importSession(recoverySession, autoRefresh = true)
                val user = client.auth.currentUserOrNull()
                val token = client.auth.currentAccessTokenOrNull()
                val userId = user?.id.orEmpty()
                if (token == null || userId.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "验证失败，请重新发送邮件"
                    )
                    return@launch
                }
                val profile = loadOrCreateProfile(userId)
                if (profile == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "验证成功，但无法读取账号设备状态，请稍后重试"
                    )
                    return@launch
                }
                session.setSession(token, userId, profile.pairId)
                val claimedProfile = profileRepo.claimCurrentDeviceSession(profile)
                if (claimedProfile == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "验证成功，但无法绑定当前设备，请稍后重试"
                    )
                    return@launch
                }
                session.saveNickname(claimedProfile.nickname)
                session.saveAvatar(claimedProfile.avatarUrl ?: "")
                dishRepo.syncFromCloud()
                profileRepo.loadFromCloud()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isDeviceSwitchComplete = true,
                    errorMessage = "设备切换成功"
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "链接已失效，请重新发送验证邮件"
                )
            }
        }
    }

    fun resetPasswordFromDeepLink(deepLink: String, newPassword: String, confirmPassword: String) {
        val password = newPassword.trim()
        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(errorMessage = "密码最少8位")
            return
        }
        if (password != confirmPassword.trim()) {
            _uiState.value = _uiState.value.copy(errorMessage = "两次密码输入不一致")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isPasswordResetComplete = false)
            try {
                val recoverySession = client.auth.parseSessionFromUrl(deepLink)
                client.auth.importSession(recoverySession, autoRefresh = true)
                client.auth.modifyUser {
                    this.password = password
                }
                try { client.auth.signOut() } catch (_: Exception) { }
                session.clear()
                _uiState.value = AuthUiState(
                    isPasswordResetComplete = true,
                    errorMessage = "密码已修改，请重新登录"
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "链接已失效，请重新发送重置邮件"
                )
            }
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
            null
        }
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            profileRepo.releaseCurrentDeviceSession()
            try { client.auth.signOut() } catch (_: Exception) { }
            session.clear()
            _uiState.value = AuthUiState()
            onLoggedOut()
        }
    }

    companion object {
        const val PASSWORD_RESET_REDIRECT_URL = "orderdisk://auth/reset-password"
        const val DEVICE_SWITCH_REDIRECT_URL = "orderdisk://auth/switch-device"
    }
}
