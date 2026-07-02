package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val menuItemId: String,
    val menuItemName: String,
    val menuItemImageUrl: String,
    val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double
)
