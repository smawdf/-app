package com.myorderapp.data.remote.supabase

import android.os.Build
import com.myorderapp.BuildConfig
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CloudErrorLogger(
    private val session: SessionManager
) {
    private val client by lazy { SupabaseClientProvider.client }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun log(
        area: String,
        action: String,
        throwable: Throwable,
        detail: String = ""
    ) {
        val userId = session.currentUserId.ifBlank { return }
        val entry = ClientErrorLog(
            id = UUID.randomUUID().toString(),
            userId = userId,
            pairId = session.currentPairId,
            sessionId = session.currentSessionId,
            area = area.take(64),
            action = action.take(96),
            message = throwable.fullLogMessage().take(1200),
            detail = detail.take(2000),
            appVersion = BuildConfig.VERSION_NAME,
            device = "${Build.MANUFACTURER} ${Build.MODEL}".take(120),
            osVersion = "Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
            createdAt = Instant.now().toString()
        )
        scope.launch {
            try {
                client.from("client_error_logs").insert(entry)
            } catch (_: Exception) {
                // Logging must never break the user flow.
            }
        }
    }

    private fun Throwable.fullLogMessage(): String {
        return sequenceOf(
            javaClass.simpleName,
            message,
            localizedMessage,
            cause?.javaClass?.simpleName,
            cause?.message,
            stackTraceToString()
        )
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" | ")
    }
}

@Serializable
private data class ClientErrorLog(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("pair_id") val pairId: String,
    @SerialName("session_id") val sessionId: String,
    val area: String,
    val action: String,
    val message: String,
    val detail: String,
    @SerialName("app_version") val appVersion: String,
    val device: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("created_at") val createdAt: String
)
