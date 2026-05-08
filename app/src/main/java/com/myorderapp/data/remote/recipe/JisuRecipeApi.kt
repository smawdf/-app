package com.myorderapp.data.remote.recipe

import retrofit2.http.GET
import retrofit2.http.Query

interface JisuRecipeApi {

    @GET("recipe/search")
    suspend fun searchRecipes(
        @Query("keyword") keyword: String,
        @Query("num") num: Int = 20,
        @Query("start") start: Int = 0,
        @Query("appkey") appkey: String
    ): JisuRecipeResponse
}
