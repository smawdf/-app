package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dishes")
data class DishEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val name: String,
    val source: String = "custom",
    val externalId: String? = null,
    val externalSource: String? = null,
    val category: String = "",
    val imageUrl: String? = null,
    val cookStepsJson: String = "[]",  // Store as JSON string
    val ingredientsJson: String = "[]", // Store as JSON string
    val difficulty: Int = 1,
    val cookTimeMin: Int = 0,
    val whoLikesJson: String = "[]", // Store as JSON string
    val rating: Float = 0f,
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
