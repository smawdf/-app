package com.myorderapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myorderapp.data.local.entity.MenuDishEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDishDao {
    @Query("SELECT * FROM menu_dishes ORDER BY sortOrder ASC, updatedAt DESC")
    fun observeAll(): Flow<List<MenuDishEntity>>

    @Query("SELECT * FROM menu_dishes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MenuDishEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dish: MenuDishEntity)

    @Query("UPDATE menu_dishes SET category = :newName, updatedAt = :updatedAt WHERE category = :oldName")
    suspend fun renameCategory(oldName: String, newName: String, updatedAt: String)

    @Query("UPDATE menu_dishes SET isAvailable = :isAvailable, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setAvailability(id: String, isAvailable: Boolean, updatedAt: String)

    @Query("UPDATE menu_dishes SET isAvailable = :isAvailable, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun setAvailability(ids: List<String>, isAvailable: Boolean, updatedAt: String)

    @Query("UPDATE menu_dishes SET category = :category, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun moveToCategory(ids: List<String>, category: String, updatedAt: String)

    @Query("UPDATE menu_dishes SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: String)

    @Query("UPDATE menu_dishes SET monthlySales = monthlySales + :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun incrementMonthlySales(id: String, quantity: Int, updatedAt: String)

    @Query("DELETE FROM menu_dishes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM menu_dishes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
