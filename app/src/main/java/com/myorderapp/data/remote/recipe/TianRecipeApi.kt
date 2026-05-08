package com.myorderapp.data.remote.recipe

import retrofit2.http.GET
import retrofit2.http.Query

interface TianRecipeApi {

    @GET("caipu/index")
    suspend fun searchRecipes(
        @Query("key") apiKey: String,
        @Query("word") word: String,
        @Query("num") num: Int = 20,
        @Query("page") page: Int = 1
    ): TianRecipeResponse
}
