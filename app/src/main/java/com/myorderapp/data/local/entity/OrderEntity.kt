package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val shopId: String,
    val shopName: String,
    val shopCoverUrl: String,
    val status: String,
    val addressSnapshot: String,
    val buyerNote: String,
    val subtotal: Double,
    val deliveryFee: Double,
    val totalPrice: Double,
    val createdAt: String
)
