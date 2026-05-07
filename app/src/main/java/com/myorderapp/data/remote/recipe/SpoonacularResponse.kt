package com.myorderapp.data.remote.recipe

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


data class SpoonacularSearchResponse(
    @Json(name = "results") val results: List<SpoonacularRecipe> = emptyList(),
    @Json(name = "offset") val offset: Int = 0,
    @Json(name = "number") val number: Int = 20,
    @Json(name = "totalResults") val totalResults: Int = 0
)


data class SpoonacularRecipe(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "title") val title: String = "",
    @Json(name = "image") val image: String? = null,
    @Json(name = "imageType") val imageType: String? = null,
    @Json(name = "readyInMinutes") val readyInMinutes: Int = 0,
    @Json(name = "servings") val servings: Int = 0,
    @Json(name = "sourceUrl") val sourceUrl: String? = null,
    @Json(name = "spoonacularScore") val spoonacularScore: Float = 0f,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "dishTypes") val dishTypes: List<String> = emptyList(),
    @Json(name = "cuisines") val cuisines: List<String> = emptyList(),
    @Json(name = "diets") val diets: List<String> = emptyList(),
    @Json(name = "analyzedInstructions") val analyzedInstructions: List<AnalyzedInstruction> = emptyList(),
    @Json(name = "extendedIngredients") val extendedIngredients: List<SpoonacularIngredient> = emptyList()
)


data class AnalyzedInstruction(
    @Json(name = "name") val name: String = "",
    @Json(name = "steps") val steps: List<AnalyzedStep> = emptyList()
)


data class AnalyzedStep(
    @Json(name = "number") val number: Int = 0,
    @Json(name = "step") val step: String = "",
    @Json(name = "ingredients") val ingredients: List<SpoonacularIngredientRef> = emptyList(),
    @Json(name = "equipment") val equipment: List<SpoonacularEquipment> = emptyList()
)


data class SpoonacularIngredientRef(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "image") val image: String? = null
)


data class SpoonacularEquipment(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "image") val image: String? = null
)


data class SpoonacularIngredient(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "amount") val amount: Float = 0f,
    @Json(name = "unit") val unit: String = "",
    @Json(name = "original") val original: String = "",
    @Json(name = "image") val image: String? = null
)


data class SpoonacularRecipeDetail(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "title") val title: String = "",
    @Json(name = "image") val image: String? = null,
    @Json(name = "readyInMinutes") val readyInMinutes: Int = 0,
    @Json(name = "servings") val servings: Int = 0,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "dishTypes") val dishTypes: List<String> = emptyList(),
    @Json(name = "cuisines") val cuisines: List<String> = emptyList(),
    @Json(name = "analyzedInstructions") val analyzedInstructions: List<AnalyzedInstruction> = emptyList(),
    @Json(name = "extendedIngredients") val extendedIngredients: List<SpoonacularIngredient> = emptyList()
)
