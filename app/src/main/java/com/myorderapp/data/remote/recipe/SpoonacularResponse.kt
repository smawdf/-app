package com.myorderapp.data.remote.recipe

import com.squareup.moshi.Json

data class SpoonacularSearchResponse(
    @Json(name = "results") val results: List<SpoonacularRecipe> = emptyList()
)

data class SpoonacularRecipe(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "title") val title: String = "",
    @Json(name = "image") val image: String? = null,
    @Json(name = "imageType") val imageType: String? = null
)
