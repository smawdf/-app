package com.myorderapp.domain.model

import com.squareup.moshi.Json

data class Profile(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "pair_id") val pairId: String = "",
    val nickname: String = "",
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "taste_prefs") val tastePrefs: DietaryPreference = DietaryPreference(),
    val allergies: List<String> = emptyList(),
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(ignore = true) val sessionId: String = ""
)

data class PairInfo(
    val partnerName: String = "",
    val isPaired: Boolean = false,
    val isOnline: Boolean = false,
    val pairCode: String = ""
)
