package com.myorderapp.data.repository

import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.domain.model.Meal
import com.myorderapp.domain.model.MealItem
import com.myorderapp.domain.repository.MealRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseMealRepository(
    private val session: SessionManager,
    private val localFallback: InMemoryMealRepository
) : MealRepository {

    private val client = SupabaseClientProvider.client
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
                val created = client.from("meals").insert(meal) {
                    select()
                }.decodeSingle<Meal>()
                _todayMeal.value = created
                return created.id
            }
            localFallback.createMeal(mealType, createdBy)
        } catch (_: Exception) {
            localFallback.createMeal(mealType, createdBy)
        }
    }

    override suspend fun addDishToMeal(mealId: String, item: MealItem) {
        try {
            if (session.isLoggedIn.value) {
                client.from("meal_items").insert(item) { select() }
            } else {
                localFallback.addDishToMeal(mealId, item)
            }
        } catch (_: Exception) {
            localFallback.addDishToMeal(mealId, item)
        }
    }

    override suspend fun removeDishFromMeal(mealId: String, itemId: String) {
        try {
            if (session.isLoggedIn.value) {
                client.from("meal_items").delete {
                    filter { eq("id", itemId) }
                }
            }
        } catch (_: Exception) { }
        localFallback.removeDishFromMeal(mealId, itemId)
    }

    override suspend fun getMealItems(mealId: String): List<MealItem> {
        return try {
            if (session.isLoggedIn.value) {
                client.from("meal_items").select {
                    filter { eq("meal_id", mealId) }
                }.decodeList<MealItem>()
            } else {
                localFallback.getMealItems(mealId)
            }
        } catch (_: Exception) {
            localFallback.getMealItems(mealId)
        }
    }

    override suspend fun submitSelection(mealId: String, chosenBy: String) {
        localFallback.submitSelection(mealId, chosenBy)
    }

    override suspend fun confirmMeal(mealId: String): Meal? {
        try {
            if (session.isLoggedIn.value) {
                val meal = _todayMeal.value?.copy(status = "completed") ?: return null
                client.from("meals").update(meal) {
                    select()
                    filter { eq("id", mealId) }
                }
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
        if (!session.isLoggedIn.value) {
            clearCloudCache()
            return
        }
        _todayMeal.value = null
        try {
            val meals = client.from("meals").select {
                filter { eq("pair_id", session.currentPairId) }
            }.decodeList<Meal>()
            if (meals.isNotEmpty()) {
                _todayMeal.value = meals.firstOrNull { it.status == "ordering" }
            }
        } catch (_: Exception) { }
    }

    fun clearCloudCache() {
        _todayMeal.value = null
    }
}
