package com.myorderapp.data.local.dao

import androidx.room.*
import com.myorderapp.data.local.entity.MealEntity
import com.myorderapp.data.local.entity.MealItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE pairId = :pairId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestMeal(pairId: String): Flow<MealEntity?>

    @Query("SELECT * FROM meals WHERE pairId = :pairId ORDER BY createdAt DESC LIMIT :limit")
    fun getMealHistory(pairId: String, limit: Int): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealById(id: String): MealEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity)

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItem(item: MealItemEntity)

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId")
    suspend fun getMealItems(mealId: String): List<MealItemEntity>

    @Query("DELETE FROM meal_items WHERE id = :id")
    suspend fun deleteMealItem(id: String)
}
