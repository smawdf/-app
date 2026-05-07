package com.myorderapp.data.remote.recipe

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpoonacularApi {

    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String,
        @Query("number") number: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("addRecipeInformation") addInfo: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("instructionsRequired") instructionsRequired: Boolean = true
    ): SpoonacularSearchResponse

    @GET("recipes/{id}/information")
    suspend fun getRecipeDetail(
        @Path("id") recipeId: Int,
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean = false
    ): SpoonacularRecipeDetail
}
