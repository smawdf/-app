package com.myorderapp.domain.model

data class MenuItem(
    val id: String,
    val shopId: String,
    val categoryId: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val imageUrl: String,
    val price: Double,
    val originPrice: Double? = null,
    val monthlySales: Int = 0,
    val tags: List<String> = emptyList()
)
