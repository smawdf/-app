package com.myorderapp.domain.model

import com.squareup.moshi.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Meal(
    val id: String = "",
    @SerialName("pair_id") @Json(name = "pair_id") val pairId: String = "",
    @SerialName("meal_type") @Json(name = "meal_type") val mealType: String = "lunch",
    val date: String = "",
    val status: String = "ordering",
    @SerialName("created_by") @Json(name = "created_by") val createdBy: String = "",
    @SerialName("confirmed_at") @Json(name = "confirmed_at") val confirmedAt: String? = null,
    @SerialName("created_at") @Json(name = "created_at") val createdAt: String = "",
    @SerialName("updated_at") @Json(name = "updated_at") val updatedAt: String = "",
    @Transient @Json(ignore = true) val items: List<MealItem> = emptyList()
)

@Serializable
data class MealItem(
    val id: String = "",
    @SerialName("meal_id") @Json(name = "meal_id") val mealId: String = "",
    @SerialName("dish_id") @Json(name = "dish_id") val dishId: String = "",
    @SerialName("dish_name") @Json(name = "dish_name") val dishName: String = "",
    @SerialName("dish_category") @Json(name = "dish_category") val dishCategory: String = "",
    @SerialName("dish_image_url") @Json(name = "dish_image_url") val dishImageUrl: String? = null,
    @SerialName("cook_time_min") @Json(name = "cook_time_min") val cookTimeMin: Int = 0,
    val difficulty: Int = 1,
    @SerialName("chosen_by") @Json(name = "chosen_by") val chosenBy: String = "",
    @SerialName("chosen_by_name") @Json(name = "chosen_by_name") val chosenByName: String = "",
    val quantity: Int = 1,
    val notes: String = ""
)
