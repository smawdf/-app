package com.myorderapp.data.remote.recipe

import com.squareup.moshi.Json

data class TheMealDbSearchResponse(
    @Json(name = "meals") val meals: List<TheMealDbMeal>? = emptyList()
)

data class TheMealDbMeal(
    @Json(name = "idMeal") val idMeal: String = "",
    @Json(name = "strMeal") val strMeal: String = "",
    @Json(name = "strCategory") val strCategory: String? = null,
    @Json(name = "strInstructions") val strInstructions: String? = null,
    @Json(name = "strMealThumb") val strMealThumb: String? = null
)
