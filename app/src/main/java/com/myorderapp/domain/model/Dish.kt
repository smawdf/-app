package com.myorderapp.domain.model

import com.squareup.moshi.Json

data class Dish(
    val id: String = "",
    @Json(name = "pair_id") val pairId: String = "",
    val name: String = "",
    val source: String = "custom",
    @Json(name = "external_id") val externalId: String? = null,
    @Json(name = "external_source") val externalSource: String? = null,
    val category: String = "",
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "cook_steps") val cookSteps: List<CookStep> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val difficulty: Int = 1,
    @Json(name = "cook_time_min") val cookTimeMin: Int = 0,
    @Json(name = "who_likes") val whoLikes: List<String> = emptyList(),
    val rating: Float = 0f,
    val notes: String = "",
    @Json(name = "created_by") val createdBy: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

data class CookStep(
    val step: Int = 1,
    val description: String = "",
    val tip: String? = null,
    val imageUrl: String? = null
)

data class DishTag(
    val id: String = "",
    val dishId: String = "",
    val pairId: String = "",
    val name: String = "",
    val color: String = "#FF6B6B"
)

data class DietaryPreference(
    val spicy: Boolean = false,
    val sweet: Boolean = false,
    val sour: Boolean = false,
    val salty: Boolean = false,
    val light: Boolean = false,
    val heavy: Boolean = false
)
