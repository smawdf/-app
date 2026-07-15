package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val pairId: String,
    val buyerName: String,
    val buyerAvatarUrl: String,
    val buyerRole: String,
    val shopId: String,
    val shopName: String,
    val shopCoverUrl: String,
    val status: String,
    val addressSnapshot: String,
    val buyerNote: String,
    val subtotal: Double,
    val deliveryFee: Double,
    val totalPrice: Double,
    val candyCoinsSpent: Int = 0,
    val createdAt: String,
    val syncState: String = "synced"
)
