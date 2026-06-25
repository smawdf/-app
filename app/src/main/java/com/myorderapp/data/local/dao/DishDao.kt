package com.myorderapp.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.myorderapp.data.local.entity.DishEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DishDao {
    @Query("SELECT * FROM dishes WHERE pairId = :pairId ORDER BY createdAt DESC")
    fun getDishesByPair(pairId: String): Flow<List<DishEntity>>

    @Query("SELECT * FROM dishes WHERE pairId = :pairId AND name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchDishes(pairId: String, query: String): Flow<List<DishEntity>>

    @Query("SELECT * FROM dishes WHERE id = :id")
    suspend fun getDishById(id: String): DishEntity?

    @Query("SELECT * FROM dishes WHERE pairId = :pairId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentDishes(pairId: String, limit: Int): Flow<List<DishEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dishes: List<DishEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dish: DishEntity)

    @Update
    suspend fun update(dish: DishEntity)

    @Query("DELETE FROM dishes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM dishes WHERE pairId = :pairId")
    suspend fun deleteByPair(pairId: String)

    @Query("SELECT * FROM dishes WHERE pairId LIKE :pairId ORDER BY createdAt DESC")
    fun pagingSource(pairId: String): PagingSource<Int, DishEntity>
}
