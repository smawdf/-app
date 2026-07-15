package com.myorderapp.data.repository

import android.content.Context
import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class UserPreferencesRepository(
    context: Context,
    private val session: SessionManager,
    private val cloudErrorLogger: CloudErrorLogger? = null
) {
    private val client by lazy { SupabaseClientProvider.client }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _orderNotificationsEnabled = MutableStateFlow(loadLocalPreference())
    val orderNotificationsEnabled: StateFlow<Boolean> = _orderNotificationsEnabled.asStateFlow()

    suspend fun loadFromCloud() {
        if (ensureLocalOwner()) _orderNotificationsEnabled.value = true
        if (!session.isLoggedIn.value || session.currentUserId.isBlank()) return
        try {
            val remote = client.from("user_preferences").select {
                filter { eq("user_id", session.currentUserId) }
            }.decodeList<RemoteUserPreferences>().firstOrNull()
            if (remote != null) {
                val localUpdatedAt = prefs.getString(KEY_UPDATED_AT, "").orEmpty()
                if (remote.updatedAt.isAfter(localUpdatedAt)) saveLocal(remote.orderNotificationsEnabled, remote.updatedAt)
                else syncToCloud(_orderNotificationsEnabled.value)
            } else {
                syncToCloud(_orderNotificationsEnabled.value)
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("preferences", "load", e)
        }
    }

    suspend fun setOrderNotificationsEnabled(enabled: Boolean) {
        ensureLocalOwner()
        saveLocal(enabled, Instant.now().toString())
        syncToCloud(enabled)
    }

    private fun saveLocal(enabled: Boolean, updatedAt: String) {
        prefs.edit().putBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED, enabled).putString(KEY_UPDATED_AT, updatedAt).apply()
        _orderNotificationsEnabled.value = enabled
    }

    private suspend fun syncToCloud(enabled: Boolean) {
        if (!session.isLoggedIn.value || session.currentUserId.isBlank()) return
        try {
            client.from("user_preferences").upsert(
                RemoteUserPreferences(
                    userId = session.currentUserId,
                    orderNotificationsEnabled = enabled,
                    updatedAt = prefs.getString(KEY_UPDATED_AT, "").orEmpty().ifBlank { Instant.now().toString() }
                )
            ) { select() }
        } catch (e: Exception) {
            cloudErrorLogger?.log("preferences", "sync", e, "orderNotificationsEnabled=$enabled")
        }
    }

    private companion object {
        const val PREFS_NAME = "profile_screen_prefs"
        const val KEY_ORDER_NOTIFICATIONS_ENABLED = "order_notifications_enabled"
        const val KEY_UPDATED_AT = "order_notifications_updated_at"
        const val KEY_OWNER_USER_ID = "preferences_owner_user_id"
    }

    private fun loadLocalPreference(): Boolean {
        ensureLocalOwner()
        return prefs.getBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED, true)
    }

    private fun ensureLocalOwner(): Boolean {
        val currentUserId = session.currentUserId.ifBlank { "guest" }
        val ownerUserId = prefs.getString(KEY_OWNER_USER_ID, "").orEmpty()
        return when {
            ownerUserId.isBlank() -> {
                prefs.edit().putString(KEY_OWNER_USER_ID, currentUserId).apply()
                false
            }
            ownerUserId != currentUserId -> {
                prefs.edit().clear().putString(KEY_OWNER_USER_ID, currentUserId).apply()
                true
            }
            else -> false
        }
    }
}

@Serializable
private data class RemoteUserPreferences(
    @SerialName("user_id") val userId: String,
    @SerialName("order_notifications_enabled") val orderNotificationsEnabled: Boolean = true,
    @SerialName("updated_at") val updatedAt: String = ""
)

private fun String.isAfter(other: String): Boolean {
    val left = runCatching { Instant.parse(this) }.getOrNull() ?: return false
    val right = runCatching { Instant.parse(other) }.getOrNull() ?: return true
    return left.isAfter(right)
}
