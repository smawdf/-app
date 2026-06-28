package com.myorderapp.data.repository

import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class HybridDishRepository(
    private val localRepo: RoomDishRepository,
    private val cloudRepo: SupabaseDishRepository,
    private val session: SessionManager
) : DishRepository {

    private val active: DishRepository get() = if (session.isLoggedIn.value) cloudRepo else localRepo

    override fun getAllDishes(): Flow<List<Dish>> {
        return combine(
            localRepo.getAllDishes(),
            cloudRepo.getAllDishes(),
            session.isLoggedIn
        ) { local, cloud, isLoggedIn ->
            DishMergePolicy.merge(
                local = local,
                cloud = cloud,
                includeCloud = isLoggedIn
            )
        }
    }

    override fun getDishesByCategory(category: String): Flow<List<Dish>> =
        active.getDishesByCategory(category)

    override fun searchDishes(query: String): Flow<List<Dish>> =
        active.searchDishes(query)

    override suspend fun getDishById(id: String): Dish? =
        cloudRepo.getDishById(id) ?: localRepo.getDishById(id)

    override suspend fun cacheSearchResult(dish: Dish) {
        localRepo.cacheSearchResult(dish)
        cloudRepo.cacheSearchResult(dish)
    }

    override suspend fun addDish(dish: Dish): String {
        return if (session.isLoggedIn.value) {
            val id = cloudRepo.addDish(dish)
            val created = cloudRepo.getDishById(id) ?: dish.copy(id = id)
            localRepo.cacheSearchResult(created)
            id
        } else {
            localRepo.addDish(dish)
        }
    }

    override suspend fun updateDish(dish: Dish) {
        if (session.isLoggedIn.value) {
            cloudRepo.updateDish(dish)
            val updated = cloudRepo.getDishById(dish.id) ?: dish
            localRepo.cacheSearchResult(updated)
        } else {
            localRepo.updateDish(dish)
        }
    }

    override suspend fun deleteDish(id: String) {
        if (session.isLoggedIn.value) {
            cloudRepo.deleteDish(id)
            localRepo.deleteDish(id)
        } else {
            localRepo.deleteDish(id)
        }
    }

    override fun getRecentDishes(limit: Int): Flow<List<Dish>> {
        return if (session.isLoggedIn.value) cloudRepo.getRecentDishes(limit)
        else localRepo.getRecentDishes(limit)
    }

    suspend fun syncFromCloud() {
        if (!session.isLoggedIn.value) {
            cloudRepo.clearCloudCache()
            return
        }

        cloudRepo.loadFromCloud()
        cloudRepo.getAllDishes().first().forEach { localRepo.cacheSearchResult(it) }
    }
}
