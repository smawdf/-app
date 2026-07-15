package com.myorderapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.data.local.entity.MenuDishDeletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDishDao {
    @Query("SELECT * FROM menu_dishes ORDER BY sortOrder ASC, updatedAt DESC")
    fun observeAll(): Flow<List<MenuDishEntity>>

    @Query("SELECT * FROM menu_dishes WHERE pairId = :pairId ORDER BY sortOrder ASC, updatedAt DESC")
    fun observeByPair(pairId: String): Flow<List<MenuDishEntity>>

    @Query("SELECT * FROM menu_dishes WHERE pairId = :pairId ORDER BY sortOrder ASC, updatedAt DESC")
    suspend fun getAllByPair(pairId: String): List<MenuDishEntity>

    @Query("SELECT * FROM menu_dishes WHERE id = :id AND pairId = :pairId LIMIT 1")
    suspend fun getById(id: String, pairId: String): MenuDishEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dish: MenuDishEntity)

    @Query("UPDATE menu_dishes SET category = :newName, updatedAt = :updatedAt WHERE category = :oldName AND pairId = :pairId")
    suspend fun renameCategory(oldName: String, newName: String, updatedAt: String, pairId: String)

    @Query("UPDATE menu_dishes SET isAvailable = :isAvailable, updatedAt = :updatedAt WHERE id = :id AND pairId = :pairId")
    suspend fun setAvailability(id: String, isAvailable: Boolean, updatedAt: String, pairId: String)

    @Query("UPDATE menu_dishes SET isAvailable = :isAvailable, updatedAt = :updatedAt WHERE id IN (:ids) AND pairId = :pairId")
    suspend fun setAvailability(ids: List<String>, isAvailable: Boolean, updatedAt: String, pairId: String)

    @Query("UPDATE menu_dishes SET category = :category, updatedAt = :updatedAt WHERE id IN (:ids) AND pairId = :pairId")
    suspend fun moveToCategory(ids: List<String>, category: String, updatedAt: String, pairId: String)

    @Query("UPDATE menu_dishes SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id AND pairId = :pairId")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: String, pairId: String)

    @Query("UPDATE menu_dishes SET monthlySales = monthlySales + :quantity, updatedAt = :updatedAt WHERE id = :id AND pairId = :pairId")
    suspend fun incrementMonthlySales(id: String, quantity: Int, updatedAt: String, pairId: String)

    @Query("UPDATE menu_dishes SET imageUrl = :imageUrl, updatedAt = :updatedAt WHERE id = :id AND pairId = :pairId")
    suspend fun updateImage(id: String, imageUrl: String, updatedAt: String, pairId: String)

    @Query("DELETE FROM menu_dishes WHERE id = :id AND pairId = :pairId")
    suspend fun deleteById(id: String, pairId: String)

    @Query("DELETE FROM menu_dishes WHERE id IN (:ids) AND pairId = :pairId")
    suspend fun deleteByIds(ids: List<String>, pairId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markDeleted(deletion: MenuDishDeletionEntity)

    @Query("SELECT * FROM menu_dish_deletions WHERE pairId = :pairId")
    suspend fun getDeletions(pairId: String): List<MenuDishDeletionEntity>

    @Query("DELETE FROM menu_dish_deletions WHERE id = :id AND pairId = :pairId")
    suspend fun clearDeletion(id: String, pairId: String)
}
