package com.myorderapp.data.remote.supabase

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("orderdisk_session", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _pairId = MutableStateFlow("")
    val pairId: StateFlow<String> = _pairId.asStateFlow()

    var accessToken: String = ""
        private set

    var currentUserId: String = ""
        private set

    var currentPairId: String = ""
        private set

    private var _sessionId: String = ""
    val currentSessionId: String get() = _sessionId

    init {
        restoreSession()
    }

    fun setSession(token: String, userId: String, pairId: String = "") {
        accessToken = "Bearer $token"
        currentUserId = userId
        currentPairId = pairId.ifBlank { "00000000-0000-0000-0000-000000000000" }
        _isLoggedIn.value = true
        _userId.value = userId
        _pairId.value = this.currentPairId

        // 生成新 sessionId（用于单设备登录检测）
        _sessionId = java.util.UUID.randomUUID().toString()

        prefs.edit()
            .putString("token", token)
            .putString("user_id", userId)
            .putString("pair_id", this.currentPairId)
            .putString("session_id", _sessionId)
            .apply()
    }

    fun setPairId(pairId: String) {
        currentPairId = pairId
        _pairId.value = pairId
        prefs.edit().putString("pair_id", pairId).apply()
    }

    fun saveEmail(email: String) {
        prefs.edit().putString("saved_email", email).apply()
    }

    fun getSavedEmail(): String {
        return prefs.getString("saved_email", "") ?: ""
    }

    fun saveRememberedCredentials(email: String, password: String, remember: Boolean) {
        prefs.edit()
            .putString("saved_email", email)
            .putBoolean("remember_credentials", remember)
            .apply {
                if (remember) putString("saved_password", password) else remove("saved_password")
            }
            .apply()
    }

    fun isRememberCredentialsEnabled(): Boolean {
        return prefs.getBoolean("remember_credentials", false)
    }

    fun getSavedPassword(): String {
        return if (isRememberCredentialsEnabled()) prefs.getString("saved_password", "") ?: "" else ""
    }

    fun saveNickname(nickname: String) {
        prefs.edit().putString("saved_nickname", nickname).apply()
    }

    fun getSavedNickname(): String {
        return prefs.getString("saved_nickname", "") ?: ""
    }

    fun saveAvatar(avatarUrl: String) {
        prefs.edit().putString("saved_avatar", avatarUrl).apply()
    }

    fun getSavedAvatar(): String {
        return prefs.getString("saved_avatar", "") ?: ""
    }

    fun clear() {
        val savedEmail = getSavedEmail()
        val savedPassword = prefs.getString("saved_password", "") ?: ""
        val rememberCredentials = isRememberCredentialsEnabled()

        accessToken = ""
        currentUserId = ""
        currentPairId = ""
        _sessionId = ""
        _isLoggedIn.value = false
        _userId.value = ""
        _pairId.value = ""
        prefs.edit().clear().apply()
        if (rememberCredentials) {
            saveRememberedCredentials(savedEmail, savedPassword, true)
        } else if (savedEmail.isNotBlank()) {
            saveEmail(savedEmail)
        }
    }

    private fun restoreSession() {
        val token = prefs.getString("token", null)
        val uid = prefs.getString("user_id", null)
        val pid = prefs.getString("pair_id", null)
        val sid = prefs.getString("session_id", null)
        if (token != null && uid != null) {
            accessToken = "Bearer $token"
            currentUserId = uid
            currentPairId = pid ?: "00000000-0000-0000-0000-000000000000"
            _sessionId = sid ?: ""
            _isLoggedIn.value = true
            _userId.value = uid
            _pairId.value = this.currentPairId
        }
    }
}
