package com.myorderapp.data.remote.supabase

import android.content.Context
import android.annotation.SuppressLint
import android.provider.Settings
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {

    data class PendingRegistration(val email: String, val nickname: String, val avatarPath: String)

    private val prefs = context.getSharedPreferences("orderdisk_session", Context.MODE_PRIVATE)
    private val cipher = KeystoreStringCipher()
    private val stableDeviceSessionId: String = buildStableDeviceSessionId(context)

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
    val currentSessionId: String get() = _sessionId.ifBlank { stableDeviceSessionId }
    val currentStableDeviceSessionId: String get() = stableDeviceSessionId

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

        // 同一台设备使用稳定占用 ID，卸载重装后仍可识别为原设备。
        _sessionId = stableDeviceSessionId

        prefs.edit()
            .putString("token_encrypted", cipher.encrypt(token))
            .remove("token")
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
                if (remember) {
                    putString("saved_password_encrypted", cipher.encrypt(password))
                    remove("saved_password")
                } else {
                    remove("saved_password")
                    remove("saved_password_encrypted")
                }
            }
            .apply()
    }

    fun isRememberCredentialsEnabled(): Boolean {
        return prefs.getBoolean("remember_credentials", false)
    }

    fun getSavedPassword(): String {
        if (!isRememberCredentialsEnabled()) return ""
        val encrypted = prefs.getString("saved_password_encrypted", "").orEmpty()
        if (encrypted.isNotBlank()) return cipher.decrypt(encrypted).orEmpty()
        return prefs.getString("saved_password", "").orEmpty().also { legacy ->
            if (legacy.isNotBlank()) saveRememberedCredentials(getSavedEmail(), legacy, true)
        }
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

    fun savePendingRegistration(email: String, nickname: String, avatarPath: String) {
        prefs.edit()
            .putString("pending_registration_email", email.trim().lowercase())
            .putString("pending_registration_nickname", nickname.trim())
            .putString("pending_registration_avatar_path", avatarPath)
            .apply()
    }

    fun getPendingRegistration(email: String): PendingRegistration? {
        val normalizedEmail = email.trim().lowercase()
        val pendingEmail = prefs.getString("pending_registration_email", "").orEmpty()
        if (pendingEmail.isBlank() || pendingEmail != normalizedEmail) return null
        return PendingRegistration(
            email = pendingEmail,
            nickname = prefs.getString("pending_registration_nickname", "").orEmpty(),
            avatarPath = prefs.getString("pending_registration_avatar_path", "").orEmpty()
        )
    }

    fun clearPendingRegistration() {
        prefs.edit()
            .remove("pending_registration_email")
            .remove("pending_registration_nickname")
            .remove("pending_registration_avatar_path")
            .apply()
    }

    fun clear() {
        val pendingEmail = prefs.getString("pending_registration_email", "").orEmpty()
        val pendingRegistration = getPendingRegistration(pendingEmail)
        val savedEmail = getSavedEmail()
        val savedPassword = getSavedPassword()
        val rememberCredentials = isRememberCredentialsEnabled()

        accessToken = ""
        currentUserId = ""
        currentPairId = ""
        _sessionId = ""
        _isLoggedIn.value = false
        _userId.value = ""
        _pairId.value = ""
        prefs.edit().clear().apply()
        pendingRegistration?.let { savePendingRegistration(it.email, it.nickname, it.avatarPath) }
        if (rememberCredentials) {
            saveRememberedCredentials(savedEmail, savedPassword, true)
        } else if (savedEmail.isNotBlank()) {
            saveEmail(savedEmail)
        }
    }

    fun migrateToStableDeviceSession() {
        _sessionId = stableDeviceSessionId
        prefs.edit().putString("session_id", _sessionId).apply()
    }

    private fun restoreSession() {
        val encryptedToken = prefs.getString("token_encrypted", "").orEmpty()
        val token = cipher.decrypt(encryptedToken)
            ?: prefs.getString("token", null)?.also { legacyToken ->
                prefs.edit()
                    .putString("token_encrypted", cipher.encrypt(legacyToken))
                    .remove("token")
                    .apply()
            }
        val uid = prefs.getString("user_id", null)
        val pid = prefs.getString("pair_id", null)
        val sid = prefs.getString("session_id", null)
        if (token != null && uid != null) {
            accessToken = "Bearer $token"
            currentUserId = uid
            currentPairId = pid ?: "00000000-0000-0000-0000-000000000000"
            _sessionId = sid ?: stableDeviceSessionId
            _isLoggedIn.value = true
            _userId.value = uid
            _pairId.value = this.currentPairId
        }
    }

    @SuppressLint("HardwareIds")
    private fun buildStableDeviceSessionId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
            ?: "unknown"
        val raw = "${context.packageName}:$androidId"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .take(16)
            .joinToString("") { "%02x".format(it) }
        return "device-$hash"
    }
}
