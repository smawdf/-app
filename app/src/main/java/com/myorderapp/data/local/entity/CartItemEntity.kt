package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val shopName: String,
    val shopCoverUrl: String,
    val minOrderPrice: Double,
    val deliveryFee: Double,
    val menuItemId: String,
    val menuItemName: String,
    val menuItemImageUrl: String,
    val unitPrice: Double,
    val quantity: Int,
    val note: String,
    val addedAt: String
)
