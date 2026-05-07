package com.myorderapp.domain.model

data class WishlistItem(
    val id: String = "",
    val pairId: String = "",
    val dishId: String = "",
    val dishName: String = "",
    val dishCategory: String = "",
    val dishImageUrl: String? = null,
    val externalSource: String? = null,
    val addedBy: String = "",
    val addedByName: String = "",
    val status: String = "pending", // pending, tried, rejected
    val notes: String = "",
    val createdAt: String = ""
)
