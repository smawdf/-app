package com.myorderapp.data.repository

import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.EntityMapper.toEntity
import com.myorderapp.data.local.dao.DishDao
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDishRepository(
    private val dishDao: DishDao
) : DishRepository {

    override fun getAllDishes(): Flow<List<Dish>> =
        dishDao.getDishesByPair("%").map { list -> list.map { it.toDomain() } }

    override fun getDishesByCategory(category: String): Flow<List<Dish>> =
        dishDao.getDishesByPair("%").map { list ->
            list.map { it.toDomain() }.filter { it.category == category }
        }

    override fun searchDishes(query: String): Flow<List<Dish>> =
        if (query.isBlank()) getAllDishes()
        else dishDao.searchDishes("%", query).map { list -> list.map { it.toDomain() } }

    override suspend fun getDishById(id: String): Dish? =
        dishDao.getDishById(id)?.toDomain()

    override suspend fun cacheSearchResult(dish: Dish) {
        dishDao.insert(dish.toEntity())
    }

    override suspend fun addDish(dish: Dish): String {
        val id = if (dish.id.isBlank()) "dish_${System.currentTimeMillis()}" else dish.id
        dishDao.insert(dish.copy(id = id).toEntity())
        return id
    }

    override suspend fun updateDish(dish: Dish) {
        dishDao.update(dish.toEntity())
    }

    override suspend fun deleteDish(id: String) {
        dishDao.deleteById(id)
    }

    override fun getRecentDishes(limit: Int): Flow<List<Dish>> =
        dishDao.getRecentDishes("%", limit).map { list -> list.map { it.toDomain() } }
}
