package com.myorderapp.data.repository

import com.myorderapp.domain.model.Meal
import com.myorderapp.domain.model.MealItem
import com.myorderapp.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InMemoryMealRepository : MealRepository {

    private val _meals = MutableStateFlow(sampleMeals)
    private val _todayMeal = MutableStateFlow<Meal?>(null)

    override fun getTodayMeal(): Flow<Meal?> = _todayMeal

    override suspend fun createMeal(mealType: String, createdBy: String): String {
        val id = "meal_${System.currentTimeMillis()}"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val meal = Meal(
            id = id,
            mealType = mealType,
            date = today,
            status = "ordering",
            createdBy = createdBy,
            createdAt = today
        )
        _todayMeal.value = meal
        _meals.value = _meals.value + meal
        return id
    }

    override suspend fun addDishToMeal(mealId: String, dish: MealItem) {
        val meal = _todayMeal.value
        if (meal?.id == mealId) {
            _todayMeal.value = meal.copy(items = meal.items + dish)
        }
    }

    override suspend fun removeDishFromMeal(mealId: String, itemId: String) {
        val meal = _todayMeal.value
        if (meal?.id == mealId) {
            _todayMeal.value = meal.copy(items = meal.items.filter { it.id != itemId })
        }
    }

    override suspend fun submitSelection(mealId: String, chosenBy: String) {
        // Mark selection as submitted for this user
        // When both have submitted, status becomes "confirmed"
        val meal = _todayMeal.value
        if (meal?.id == mealId) {
            _todayMeal.value = meal.copy(status = "confirmed")
        }
    }

    override suspend fun confirmMeal(mealId: String): Meal? {
        val meal = _todayMeal.value
        if (meal?.id == mealId) {
            val confirmed = meal.copy(status = "completed")
            _todayMeal.value = confirmed
            _meals.value = _meals.value.map { if (it.id == mealId) confirmed else it }
            return confirmed
        }
        return null
    }

    override fun getMealHistory(limit: Int): Flow<List<Meal>> =
        _meals.map { list -> list.filter { it.status == "completed" }.take(limit) }

    override suspend fun getMealById(id: String): Meal? =
        _meals.value.find { it.id == id }

    companion object {
        val sampleMeals = emptyList<Meal>()
    }
}
