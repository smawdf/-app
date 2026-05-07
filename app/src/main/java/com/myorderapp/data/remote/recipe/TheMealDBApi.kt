package com.myorderapp.data.remote.recipe

import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDBApi {
    @GET("search.php")
    suspend fun searchMeals(@Query("s") name: String): TheMealDBSearchResponse

    @GET("random.php")
    suspend fun randomMeal(): TheMealDBSearchResponse

    @GET("lookup.php")
    suspend fun lookupMeal(@Query("i") id: String): TheMealDBSearchResponse
}
