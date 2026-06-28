package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist_items")
data class WishlistEntity(
    @PrimaryKey val id: String,
    val pairId: String = "",
    val dishId: String = "",
    val dishName: String = "",
    val dishCategory: String = "",
    val dishImageUrl: String? = null,
    val externalSource: String? = null,
    val addedBy: String = "",
    val addedByName: String = "",
    val status: String = "pending",
    val notes: String = "",
    val createdAt: String = ""
)
