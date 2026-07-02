package com.myorderapp.domain.model

data class Shop(
    val id: String,
    val name: String,
    val logoUrl: String,
    val coverUrl: String,
    val rating: Double,
    val monthlySales: Int,
    val deliveryFee: Double,
    val minOrderPrice: Double,
    val avgDeliveryMinutes: Int,
    val announcement: String,
    val tags: List<String>,
    val status: String = "营业中",
    val distanceKm: Double = 0.0,
    val promoText: String = ""
)
