package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_items",
    foreignKeys = [ForeignKey(
        entity = MealEntity::class,
        parentColumns = ["id"],
        childColumns = ["mealId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MealItemEntity(
    @PrimaryKey val id: String,
    val mealId: String,
    val dishId: String,
    val dishName: String,
    val dishCategory: String = "",
    val dishImageUrl: String? = null,
    val cookTimeMin: Int = 0,
    val difficulty: Int = 1,
    val chosenBy: String = "",
    val chosenByName: String = "",
    val quantity: Int = 1,
    val notes: String = ""
)
