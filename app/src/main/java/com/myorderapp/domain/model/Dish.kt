package com.myorderapp.domain.model

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Dish(
    val id: String = "",
    @SerialName("pair_id") @Json(name = "pair_id") val pairId: String = "",
    val name: String = "",
    val source: String = "custom",
    @SerialName("external_id") @Json(name = "external_id") val externalId: String? = null,
    @SerialName("external_source") @Json(name = "external_source") val externalSource: String? = null,
    val category: String = "",
    @SerialName("image_url") @Json(name = "image_url") val imageUrl: String? = null,
    @SerialName("cook_steps") @Json(name = "cook_steps") val cookSteps: List<CookStep> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val difficulty: Int = 1,
    @SerialName("cook_time_min") @Json(name = "cook_time_min") val cookTimeMin: Int = 0,
    @SerialName("who_likes") @Json(name = "who_likes") val whoLikes: List<String> = emptyList(),
    val rating: Float = 0f,
    val notes: String = "",
    @SerialName("created_by") @Json(name = "created_by") val createdBy: String = "",
    @SerialName("created_at") @Json(name = "created_at") val createdAt: String = "",
    @SerialName("updated_at") @Json(name = "updated_at") val updatedAt: String = ""
)

@Serializable
data class CookStep(
    val step: Int = 1,
    val description: String = "",
    val tip: String? = null,
    @SerialName("image_url") @Json(name = "image_url") val imageUrl: String? = null
)

@Serializable
data class DishTag(
    val id: String = "",
    @SerialName("dish_id") @Json(name = "dish_id") val dishId: String = "",
    @SerialName("pair_id") @Json(name = "pair_id") val pairId: String = "",
    val name: String = "",
    val color: String = "#FF6B6B"
)

@Serializable
data class DietaryPreference(
    val spicy: Boolean = false,
    val sweet: Boolean = false,
    val sour: Boolean = false,
    val salty: Boolean = false,
    val light: Boolean = false,
    val heavy: Boolean = false,
    val custom: List<String> = emptyList()
)
