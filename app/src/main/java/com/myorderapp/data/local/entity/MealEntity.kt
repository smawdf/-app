package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val mealType: String = "lunch",
    val date: String = "",
    val status: String = "ordering",
    val createdBy: String = "",
    val confirmedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)
