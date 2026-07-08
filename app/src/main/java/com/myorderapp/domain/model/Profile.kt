package com.myorderapp.domain.model

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Profile(
    val id: String = "",
    @SerialName("user_id") @Json(name = "user_id") val userId: String = "",
    @SerialName("pair_id") @Json(name = "pair_id") val pairId: String = "",
    val nickname: String = "",
    @SerialName("avatar_url") @Json(name = "avatar_url") val avatarUrl: String? = null,
    @SerialName("taste_prefs") @Json(name = "taste_prefs") val tastePrefs: DietaryPreference = DietaryPreference(),
    val allergies: List<String> = emptyList(),
    @SerialName("created_at") @Json(name = "created_at") val createdAt: String = "",
    @SerialName("updated_at") @Json(name = "updated_at") val updatedAt: String = "",
    @SerialName("candy_coins") @Json(name = "candy_coins") val candyCoins: Int = 66,
    @Transient @Json(ignore = true) val pairedAt: String = "",
    @Transient @Json(ignore = true) val sessionId: String = ""
)

data class PairInfo(
    val partnerName: String = "",
    val isPaired: Boolean = false,
    val isOnline: Boolean = false,
    val pairCode: String = "",
    val partnerCandyCoins: Int? = null
)
