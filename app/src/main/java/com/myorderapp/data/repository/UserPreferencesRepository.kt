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
    private val _orderNotificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED, false))
    val orderNotificationsEnabled: StateFlow<Boolean> = _orderNotificationsEnabled.asStateFlow()

    suspend fun loadFromCloud() {
        if (!session.isLoggedIn.value || session.currentUserId.isBlank()) return
        try {
            val remote = client.from("user_preferences").select {
                filter { eq("user_id", session.currentUserId) }
            }.decodeList<RemoteUserPreferences>().firstOrNull()
            if (remote != null) {
                saveLocal(remote.orderNotificationsEnabled)
            } else {
                syncToCloud(_orderNotificationsEnabled.value)
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("preferences", "load", e)
        }
    }

    suspend fun setOrderNotificationsEnabled(enabled: Boolean) {
        saveLocal(enabled)
        syncToCloud(enabled)
    }

    private fun saveLocal(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED, enabled).apply()
        _orderNotificationsEnabled.value = enabled
    }

    private suspend fun syncToCloud(enabled: Boolean) {
        if (!session.isLoggedIn.value || session.currentUserId.isBlank()) return
        try {
            client.from("user_preferences").upsert(
                RemoteUserPreferences(
                    userId = session.currentUserId,
                    orderNotificationsEnabled = enabled,
                    updatedAt = Instant.now().toString()
                )
            ) { select() }
        } catch (e: Exception) {
            cloudErrorLogger?.log("preferences", "sync", e, "orderNotificationsEnabled=$enabled")
        }
    }

    private companion object {
        const val PREFS_NAME = "profile_screen_prefs"
        const val KEY_ORDER_NOTIFICATIONS_ENABLED = "order_notifications_enabled"
    }
}

@Serializable
private data class RemoteUserPreferences(
    @SerialName("user_id") val userId: String,
    @SerialName("order_notifications_enabled") val orderNotificationsEnabled: Boolean = false,
    @SerialName("updated_at") val updatedAt: String = ""
)
