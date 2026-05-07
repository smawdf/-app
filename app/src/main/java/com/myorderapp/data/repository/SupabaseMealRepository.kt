package com.myorderapp.data.repository

import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseApi
import com.myorderapp.domain.model.Meal
import com.myorderapp.domain.model.MealItem
import com.myorderapp.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseMealRepository(
    private val api: SupabaseApi,
    private val session: SessionManager,
    private val localFallback: InMemoryMealRepository
) : MealRepository {

    private val _todayMeal = MutableStateFlow<Meal?>(null)

    override fun getTodayMeal(): Flow<Meal?> = _todayMeal.asStateFlow()

    override suspend fun createMeal(mealType: String, createdBy: String): String {
        return try {
            if (session.isLoggedIn.value) {
                val meal = Meal(
                    pairId = session.currentPairId,
                    mealType = mealType,
                    status = "ordering",
                    createdBy = session.currentUserId
                )
                val result = api.createMeal(meal, session.accessToken)
                val created = result.firstOrNull()
                if (created != null) {
                    _todayMeal.value = created
                    return created.id
                }
            }
            localFallback.createMeal(mealType, createdBy)
        } catch (_: Exception) {
            localFallback.createMeal(mealType, createdBy)
        }
    }

    override suspend fun addDishToMeal(mealId: String, item: MealItem) {
        try {
            if (session.isLoggedIn.value) {
                api.createMealItem(item, session.accessToken)
            } else {
                localFallback.addDishToMeal(mealId, item)
            }
        } catch (_: Exception) {
            localFallback.addDishToMeal(mealId, item)
        }
    }

    override suspend fun removeDishFromMeal(mealId: String, itemId: String) {
        localFallback.removeDishFromMeal(mealId, itemId)
    }

    override suspend fun submitSelection(mealId: String, chosenBy: String) {
        localFallback.submitSelection(mealId, chosenBy)
    }

    override suspend fun confirmMeal(mealId: String): Meal? {
        try {
            if (session.isLoggedIn.value) {
                val meal = _todayMeal.value?.copy(status = "completed") ?: return null
                api.createMeal(meal, session.accessToken)
            }
        } catch (_: Exception) { }
        return localFallback.confirmMeal(mealId)
    }

    override fun getMealHistory(limit: Int): Flow<List<Meal>> {
        return localFallback.getMealHistory(limit)
    }

    override suspend fun getMealById(id: String): Meal? {
        return localFallback.getMealById(id)
    }

    suspend fun syncFromCloud() {
        if (!session.isLoggedIn.value) return
        try {
            val meals = api.getMeals(session.currentPairId, session.accessToken)
            if (meals.isNotEmpty()) {
                _todayMeal.value = meals.firstOrNull { it.status == "ordering" }
            }
        } catch (_: Exception) { }
    }
}
