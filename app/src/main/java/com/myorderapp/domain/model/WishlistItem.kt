package com.myorderapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WishlistItem(
    val id: String = "",
    @SerialName("pair_id") val pairId: String = "",
    @SerialName("dish_id") val dishId: String = "",
    @SerialName("dish_name") val dishName: String = "",
    @SerialName("dish_category") val dishCategory: String = "",
    @SerialName("dish_image_url") val dishImageUrl: String? = null,
    @SerialName("external_source") val externalSource: String? = null,
    @SerialName("added_by") val addedBy: String = "",
    @SerialName("added_by_name") val addedByName: String = "",
    val status: String = "pending",
    val notes: String = "",
    @SerialName("created_at") val createdAt: String = ""
)
