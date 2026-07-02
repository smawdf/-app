package com.myorderapp.domain.model

data class OrderItem(
    val id: String,
    val orderId: String,
    val menuItemId: String,
    val menuItemName: String,
    val menuItemImageUrl: String,
    val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double
)

data class OrderTimelineEntry(
    val title: String,
    val timestamp: String,
    val isCompleted: Boolean
)

data class OrderRecord(
    val id: String,
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
    val createdAt: String,
    val items: List<OrderItem> = emptyList(),
    val timeline: List<OrderTimelineEntry> = emptyList()
)
