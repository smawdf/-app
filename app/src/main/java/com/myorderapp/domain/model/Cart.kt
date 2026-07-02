package com.myorderapp.domain.model

data class CartItem(
    val id: String,
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
    val note: String = "",
    val addedAt: String = ""
)

data class CartState(
    val shopId: String? = null,
    val shopName: String = "",
    val shopCoverUrl: String = "",
    val minOrderPrice: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val items: List<CartItem> = emptyList()
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val subtotal: Double get() = items.sumOf { it.unitPrice * it.quantity }
    val totalPrice: Double get() = subtotal + if (items.isEmpty()) 0.0 else deliveryFee
    val isEmpty: Boolean get() = items.isEmpty()
}

sealed interface CartMutationResult {
    data object Success : CartMutationResult
    data class ShopConflict(
        val currentShopId: String,
        val currentShopName: String,
        val incomingShopId: String,
        val incomingShopName: String
    ) : CartMutationResult
}
