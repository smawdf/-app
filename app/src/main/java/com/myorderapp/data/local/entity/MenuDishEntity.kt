package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_dishes")
data class MenuDishEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val originPrice: Double? = null,
    val imageUrl: String,
    val category: String,
    val description: String,
    val sortOrder: Int,
    val monthlySales: Int = 0,
    val stock: Int = 0,
    val isAvailable: Boolean = true,
    val isSignature: Boolean = false,
    val updatedAt: String
)
