package com.myorderapp.domain.model

data class MenuCategory(
    val id: String,
    val shopId: String,
    val name: String,
    val sortOrder: Int
)
