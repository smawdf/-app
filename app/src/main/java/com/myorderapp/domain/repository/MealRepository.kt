package com.myorderapp.domain.repository

import com.myorderapp.domain.model.Meal
import com.myorderapp.domain.model.MealItem
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    fun getTodayMeal(): Flow<Meal?>
    suspend fun createMeal(mealType: String, createdBy: String): String
    suspend fun addDishToMeal(mealId: String, dish: MealItem)
    suspend fun removeDishFromMeal(mealId: String, itemId: String)
    suspend fun getMealItems(mealId: String): List<MealItem>
    suspend fun submitSelection(mealId: String, chosenBy: String)
    suspend fun confirmMeal(mealId: String): Meal?
    fun getMealHistory(limit: Int = 30): Flow<List<Meal>>
    suspend fun getMealById(id: String): Meal?
}
