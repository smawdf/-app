package com.myorderapp.data.remote.recipe

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface JuheRecipeApi {

    @FormUrlEncoded
    @POST("fapigx/caipu/query")
    suspend fun searchRecipes(
        @Field("key") apiKey: String,
        @Field("word") word: String,
        @Field("num") num: Int = 20,
        @Field("page") page: Int = 1
    ): JuheRecipeResponse
}
