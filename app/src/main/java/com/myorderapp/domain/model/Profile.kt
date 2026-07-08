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
    @SerialName("session_id") @Json(name = "session_id") val sessionId: String = "",
    @SerialName("session_updated_at") @Json(name = "session_updated_at") val sessionUpdatedAt: String = "",
    @Transient @Json(ignore = true) val pairedAt: String = ""
)

data class PairInfo(
    val partnerName: String = "",
    val isPaired: Boolean = false,
    val isOnline: Boolean = false,
    val pairCode: String = "",
    val partnerCandyCoins: Int? = null
)

data class PairInvitePreview(
    val code: String,
    val inviterName: String,
    val inviterRole: String
) {
    val inviteeRole: String
        get() = if (inviterRole == ROLE_CARETAKER) ROLE_EATER else ROLE_CARETAKER

    val promptText: String
        get() = if (inviterRole == ROLE_CARETAKER) {
            "$inviterName 邀请你加入他的小餐桌"
        } else {
            "$inviterName 邀请你去下厨"
        }

    val confirmText: String
        get() = if (inviterRole == ROLE_CARETAKER) "去点餐" else "去做饭"
}

const val ROLE_CARETAKER = "caretaker"
const val ROLE_EATER = "eater"
