package com.myorderapp.domain.model

import com.squareup.moshi.Json

data class Meal(
    val id: String = "",
    @Json(name = "pair_id") val pairId: String = "",
    @Json(name = "meal_type") val mealType: String = "lunch",
    val date: String = "",
    val status: String = "ordering",
    @Json(name = "created_by") val createdBy: String = "",
    @Json(name = "confirmed_at") val confirmedAt: String? = null,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(ignore = true) val items: List<MealItem> = emptyList()
)

data class MealItem(
    val id: String = "",
    @Json(name = "meal_id") val mealId: String = "",
    @Json(name = "dish_id") val dishId: String = "",
    @Json(name = "dish_name") val dishName: String = "",
    @Json(name = "dish_category") val dishCategory: String = "",
    @Json(name = "dish_image_url") val dishImageUrl: String? = null,
    @Json(name = "cook_time_min") val cookTimeMin: Int = 0,
    val difficulty: Int = 1,
    @Json(name = "chosen_by") val chosenBy: String = "",
    @Json(name = "chosen_by_name") val chosenByName: String = "",
    val quantity: Int = 1,
    val notes: String = ""
)
