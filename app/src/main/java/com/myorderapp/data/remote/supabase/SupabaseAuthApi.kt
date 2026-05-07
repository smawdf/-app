package com.myorderapp.data.remote.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

data class AuthBody(
    val email: String,
    val password: String
)

data class AuthResponse(
    @Json(name = "access_token") val accessToken: String = "",
    @Json(name = "refresh_token") val refreshToken: String = "",
    @Json(name = "user") val user: AuthUser? = null
)

data class AuthUser(
    @Json(name = "id") val id: String = "",
    @Json(name = "email") val email: String = ""
)

interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    @Headers("Content-Type: application/json")
    suspend fun signUp(
        @Body body: AuthBody,
        @Header("apikey") apiKey: String
    ): AuthResponse

    @POST("auth/v1/token?grant_type=password")
    @Headers("Content-Type: application/json")
    suspend fun signIn(
        @Body body: AuthBody,
        @Header("apikey") apiKey: String
    ): AuthResponse

    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("Authorization") token: String,
        @Header("apikey") apiKey: String
    )
}
