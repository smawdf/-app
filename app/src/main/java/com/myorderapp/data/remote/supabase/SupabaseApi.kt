package com.myorderapp.data.remote.supabase

import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.model.Meal
import com.myorderapp.domain.model.MealItem
import com.myorderapp.domain.model.Profile
import retrofit2.http.*

interface SupabaseApi {

    // ── Profiles ──
    @GET("profiles")
    suspend fun getProfile(
        @Query("user_id") userId: String,
        @Header("Authorization") token: String
    ): List<Profile>

    @GET("profiles")
    suspend fun getProfilesByPairId(
        @Query("pair_id") pairId: String,
        @Header("Authorization") token: String
    ): List<Profile>

    @POST("profiles")
    suspend fun createProfile(
        @Body profile: Profile,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Profile>

    @PATCH("profiles")
    suspend fun updateProfile(
        @Query("user_id") userId: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Profile>

    @POST("profiles")
    suspend fun upsertProfile(
        @Body profile: Profile,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Profile>

    // ── Dishes ──
    @GET("dishes")
    suspend fun getDishes(
        @Query("pair_id") pairId: String,
        @Header("Authorization") token: String,
        @Query("select") select: String = "*"
    ): List<Dish>

    @GET("dishes")
    suspend fun getAllDishes(
        @Header("Authorization") token: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<Dish>

    @POST("dishes")
    suspend fun createDish(
        @Body dish: Dish,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Dish>

    @PATCH("dishes")
    suspend fun updateDish(
        @Query("id") dishId: String,
        @Body dish: Dish,
        @Header("Authorization") token: String
    ): List<Dish>

    @DELETE("dishes")
    suspend fun deleteDish(
        @Query("id") dishId: String,
        @Header("Authorization") token: String
    )

    // ── Meals ──
    @GET("meals")
    suspend fun getMeals(
        @Query("pair_id") pairId: String,
        @Header("Authorization") token: String,
        @Query("select") select: String = "*"
    ): List<Meal>

    @POST("meals")
    suspend fun createMeal(
        @Body meal: Meal,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Meal>

    // ── Meal Items ──
    @GET("meal_items")
    suspend fun getMealItems(
        @Query("meal_id") mealId: String,
        @Header("Authorization") token: String
    ): List<MealItem>

    @POST("meal_items")
    suspend fun createMealItem(
        @Body item: MealItem,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<MealItem>
}
