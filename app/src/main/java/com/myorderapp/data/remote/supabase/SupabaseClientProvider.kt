package com.myorderapp.data.remote.supabase

import android.util.Base64
import com.myorderapp.ApiConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object SupabaseClientProvider {
    private const val AUTH_REFRESH_SKEW_SECONDS = 60L
    private val authRefreshMutex = Mutex()

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = ApiConfig.SUPABASE_URL,
            supabaseKey = ApiConfig.SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
            )
            install(Postgrest)
            install(Auth) {
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
                autoSaveToStorage = true
            }
            install(Storage)
        }
    }

    suspend fun ensureFreshAuthSession(): Boolean {
        val auth = client.auth
        val initialToken = auth.currentAccessTokenOrNull() ?: return false
        if (!initialToken.isExpiredOrNearExpiry()) return true
        return authRefreshMutex.withLock {
            val latestToken = auth.currentAccessTokenOrNull() ?: return@withLock false
            if (!latestToken.isExpiredOrNearExpiry()) return@withLock true
            runCatching {
                auth.refreshCurrentSession()
                auth.currentAccessTokenOrNull()?.isExpiredOrNearExpiry() == false
            }.getOrDefault(false)
        }
    }

    private fun String.isExpiredOrNearExpiry(): Boolean {
        val expiresAt = jwtExpiresAtEpochSeconds() ?: return false
        val now = System.currentTimeMillis() / 1000L
        return expiresAt <= now + AUTH_REFRESH_SKEW_SECONDS
    }

    private fun String.jwtExpiresAtEpochSeconds(): Long? = runCatching {
        val payload = split('.').getOrNull(1) ?: return@runCatching null
        val decoded = Base64.decode(
            payload,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        ).toString(Charsets.UTF_8)
        Json.parseToJsonElement(decoded)
            .jsonObject["exp"]
            ?.jsonPrimitive
            ?.longOrNull
    }.getOrNull()
}
