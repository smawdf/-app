package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_dish_deletions")
data class MenuDishDeletionEntity(
    @PrimaryKey val id: String,
    val pairId: String,
    val deletedAt: String
)
