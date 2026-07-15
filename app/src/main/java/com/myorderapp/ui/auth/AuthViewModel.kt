package com.myorderapp.ui.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.data.repository.SupabaseProfileRepository
import com.myorderapp.data.sync.CloudSyncCoordinator
import com.myorderapp.domain.model.Profile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.parseSessionFromUrl
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.OTP
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.from
import java.time.Instant

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
    private val profileRepo: SupabaseProfileRepository,
    private val cloudSyncCoordinator: CloudSyncCoordinator,
    private val cloudErrorLogger: CloudErrorLogger
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
                    session.setSession(token, userId, "")
                    val profile = try {
                        loadOrCreateProfile(userId)
                    } catch (profileError: Exception) {
                        try { client.auth.signOut() } catch (_: Exception) { }
                        session.clear()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "登录失败：无法读取账号资料：${profileError.fullAuthErrorText().compactAuthErrorDetail()}"
                        )
                        return@launch
                    }
                    val pairId = profile.pairId
                    session.setPairId(pairId)
                    val completedProfile = profileRepo.applyPendingRegistration(state.email, profile)
                    session.saveRememberedCredentials(
                        email = state.email.trim(),
                        password = state.password,
                        remember = state.rememberCredentials
                    )
                    session.saveNickname(completedProfile.nickname)
                    session.saveAvatar(completedProfile.avatarUrl?.takeIfCloudAvatarUrl().orEmpty())
                    _uiState.value = _uiState.value.copy(isLoggedIn = true, isLoading = false)
                    cloudSyncCoordinator.syncInBackground()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "登录失败：未获取到用户信息"
                    )
                }
            } catch (e: Exception) {
                cloudErrorLogger.log("auth", "submit", e, "mode=${state.mode}")
                val msg = e.fullAuthErrorText()
                val errorMsg = when {
                    msg.contains("Invalid login credentials", ignoreCase = true) ||
                    msg.contains("User not found", ignoreCase = true) ||
                    msg.contains("AuthApiError", ignoreCase = true) && msg.contains("invalid", ignoreCase = true) ||
                    msg.contains("invalid_grant", ignoreCase = true) ->
                        "账号或密码不正确，请检查后重试。"
                    msg.contains("Email not confirmed", ignoreCase = true) ||
                    msg.contains("email confirmation", ignoreCase = true) ||
                    msg.contains("not confirmed", ignoreCase = true) ||
                    msg.contains("email_not_confirmed", ignoreCase = true) ->
                        "该账号未完成注册。当前不使用邮箱验证，请联系管理员关闭 Supabase Confirm email。"
                    msg.contains("Invalid password", ignoreCase = true) ||
                    msg.contains("wrong password", ignoreCase = true) ->
                        "密码错误，请重试"
                    msg.contains("timeout", ignoreCase = true) ||
                    msg.contains("Unable to resolve host", ignoreCase = true) ||
                    msg.contains("Failed to connect", ignoreCase = true) ||
                    msg.contains("Network is unreachable", ignoreCase = true) ->
                        "网络连接失败，请检查网络后重试。"
                    isAccountAlreadyExistsMessage(msg) ->
                        "账号已存在，请直接登录"
                    else -> "请求失败：${msg.compactAuthErrorDetail()}"
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
            _uiState.value = _uiState.value.copy(errorMessage = "普通账号暂不支持邮箱找回，请使用注册密码登录。")
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
                    errorMessage = "重置邮件已发送。请在当前设备打开邮件链接，再回到 App 修改密码。"
                )
            } catch (e: Exception) {
                cloudErrorLogger.log("auth", "send_password_reset", e, "email=$normalizedEmail")
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
            _uiState.value = _uiState.value.copy(errorMessage = "只有邮箱账号支持验证切换设备。普通账号请先在原设备退出。")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isResetEmailSent = false)
            try {
                client.auth.signInWith(OTP, redirectUrl = DEVICE_SWITCH_REDIRECT_URL) {
                    this.email = normalizedEmail
                    this.createUser = false
                }
                session.saveEmail(normalizedEmail)
                _uiState.value = _uiState.value.copy(
                    email = normalizedEmail,
                    isLoading = false,
                    isResetEmailSent = true,
                    canSwitchDeviceByEmail = true,
                    errorMessage = "切换验证邮件已发送。请在当前设备打开邮件链接，验证后会自动接管登录。"
                )
            } catch (e: Exception) {
                val errorText = e.fullAuthErrorText()
                Log.e("OrderDiskAuth", "send_device_switch_email failed: $errorText", e)
                cloudErrorLogger.log(
                    "auth",
                    "send_device_switch_email",
                    e,
                    "emailDomain=${normalizedEmail.substringAfterLast('@', missingDelimiterValue = "unknown")}"
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = deviceSwitchEmailErrorMessage(errorText)
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
                val recoverySession = restoreSessionFromDeepLink(deepLink)
                val token = recoverySession.accessToken
                val userId = recoverySession.user?.id.orEmpty()
                if (token.isBlank() || userId.isBlank()) {
                    cloudErrorLogger.log(
                        "auth",
                        "switch_device_missing_session",
                        IllegalStateException("Email callback did not contain a usable session"),
                        deepLink.safeAuthCallbackMetadata()
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "验证失败，请重新发送邮件"
                    )
                    return@launch
                }
                val profile = try {
                    loadOrCreateProfile(userId)
                } catch (profileError: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "验证成功，但无法读取账号资料：${profileError.fullAuthErrorText().compactAuthErrorDetail()}"
                    )
                    return@launch
                }
                session.setSession(token, userId, profile.pairId)
                session.saveNickname(profile.nickname)
                session.saveAvatar(profile.avatarUrl?.takeIfCloudAvatarUrl().orEmpty())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isDeviceSwitchComplete = true,
                    errorMessage = "设备切换成功"
                )
                cloudSyncCoordinator.syncInBackground()
            } catch (e: Exception) {
                cloudErrorLogger.log("auth", "switch_device_from_deep_link", e)
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
                restoreSessionFromDeepLink(deepLink)
                client.auth.modifyUser {
                    this.password = password
                }
                try { client.auth.signOut() } catch (_: Exception) { }
                session.clear()
                _uiState.value = AuthUiState(
                    isPasswordResetComplete = true,
                    errorMessage = "密码已修改，请重新登录"
                )
            } catch (e: Exception) {
                cloudErrorLogger.log("auth", "reset_password_from_deep_link", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "链接已失效，请重新发送重置邮件"
                )
            }
        }
    }

    private suspend fun restoreSessionFromDeepLink(deepLink: String): UserSession {
        val uri = Uri.parse(deepLink)
        val code = uri.getQueryParameter("code")?.takeIf { it.isNotBlank() }
        return if (code != null) {
            client.auth.exchangeCodeForSession(code)
        } else {
            client.auth.parseSessionFromUrl(deepLink).also { parsedSession ->
                client.auth.importSession(parsedSession, autoRefresh = true)
            }
        }
    }

    private fun String.safeAuthCallbackMetadata(): String {
        val uri = Uri.parse(this)
        val hasCode = !uri.getQueryParameter("code").isNullOrBlank()
        val hasFragment = !uri.fragment.isNullOrBlank()
        return "scheme=${uri.scheme},host=${uri.host},path=${uri.path}," +
            "hasCode=$hasCode,hasFragment=$hasFragment"
    }

    private fun deviceSwitchEmailErrorMessage(errorText: String): String {
        return when {
            listOf("rate limit", "too many", "429", "email rate").any {
                errorText.contains(it, ignoreCase = true)
            } -> "发送太频繁，请等待几分钟后再试"
            listOf("redirect", "redirect_to", "not allowed").any {
                errorText.contains(it, ignoreCase = true)
            } -> "验证链接配置异常，请检查 Supabase Redirect URLs"
            listOf("user not found", "invalid login", "signup is disabled").any {
                errorText.contains(it, ignoreCase = true)
            } -> "没有找到这个邮箱账号，请确认注册邮箱"
            listOf("unknownhost", "timeout", "network", "connect").any {
                errorText.contains(it, ignoreCase = true)
            } -> "网络连接失败，请检查网络后重试"
            else -> "发送失败：${errorText.compactAuthErrorDetail()}"
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

    private fun Throwable.fullAuthErrorText(): String {
        return sequenceOf(
            message,
            localizedMessage,
            toString(),
            cause?.message,
            cause?.localizedMessage,
            cause?.toString()
        )
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" | ")
            .ifBlank { "未知错误" }
    }

    private fun String.compactAuthErrorDetail(): String {
        return replace(Regex("\\s+"), " ")
            .take(96)
            .ifBlank { "请检查网络或稍后重试" }
    }

    private suspend fun loadOrCreateProfile(userId: String): Profile {
        val profiles = client.from("profiles").select {
            filter { eq("user_id", userId) }
        }.decodeList<Profile>()
        return profiles.firstOrNull() ?: run {
            val localNick = session.getSavedNickname()
            val localAvatar = session.getSavedAvatar().takeIfCloudAvatarUrl().orEmpty()
            val defaultPairId = "00000000-0000-0000-0000-000000000000"
            val now = Instant.now().toString()
            client.from("profiles").insert(
                mapOf(
                    "user_id" to userId,
                    "pair_id" to defaultPairId,
                    "nickname" to localNick,
                    "avatar_url" to localAvatar.ifBlank { null },
                    "selected_role" to "",
                    "updated_at" to now
                )
            ) { select() }
            Profile(
                userId = userId,
                pairId = defaultPairId,
                nickname = localNick,
                avatarUrl = localAvatar.ifBlank { null },
                updatedAt = now
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

    companion object {
        const val PASSWORD_RESET_REDIRECT_URL = "orderdisk://auth/reset-password"
        const val DEVICE_SWITCH_REDIRECT_URL = "orderdisk://auth/switch-device"
    }

    private fun String.takeIfCloudAvatarUrl(): String? {
        return trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }
}
