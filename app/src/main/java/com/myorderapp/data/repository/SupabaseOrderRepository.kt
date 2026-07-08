package com.myorderapp.data.repository

import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.EntityMapper.toEntity
import com.myorderapp.data.local.dao.OrderDao
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.domain.model.Address
import com.myorderapp.domain.model.CartState
import com.myorderapp.domain.model.OrderItem
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.math.ceil
import java.util.UUID

@Serializable
private data class RemoteOrderPayload(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("pair_id") val pairId: String = "",
    @SerialName("buyer_name") val buyerName: String = "",
    @SerialName("buyer_avatar_url") val buyerAvatarUrl: String = "",
    @SerialName("buyer_role") val buyerRole: String = "",
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("shop_cover_url") val shopCoverUrl: String,
    val status: String,
    @SerialName("address_snapshot") val addressSnapshot: String,
    @SerialName("buyer_note") val buyerNote: String,
    val subtotal: Double,
    @SerialName("delivery_fee") val deliveryFee: Double,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("candy_coins_spent") val candyCoinsSpent: Int = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
private data class RemoteOrderItemPayload(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("menu_item_id") val menuItemId: String,
    @SerialName("menu_item_name") val menuItemName: String,
    @SerialName("menu_item_image_url") val menuItemImageUrl: String,
    @SerialName("unit_price") val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double
)

class SupabaseOrderRepository(
    private val sessionManager: SessionManager,
    private val orderDao: OrderDao,
    private val menuRepository: RoomMenuRepository,
    private val profileRepository: ProfileRepository
) : OrderRepository {

    private val client = SupabaseClientProvider.client

    override fun observeOrders(): Flow<List<OrderRecord>> {
        return orderDao.observeOrders().map { entities ->
            entities.map { order ->
                val items = orderDao.getOrderItems(order.id).map { it.toDomain() }
                order.toDomain(items)
            }
        }
    }

    override suspend fun getOrderById(orderId: String): OrderRecord? {
        syncRemoteOrders()
        val entity = orderDao.getOrderById(orderId) ?: return null
        val items = orderDao.getOrderItems(orderId).map { it.toDomain() }
        return entity.toDomain(items)
    }

    override suspend fun submitOrder(cart: CartState, address: Address, note: String): String {
        val orderId = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        val userId = sessionManager.currentUserId.ifBlank { "guest" }
        val profile = profileRepository.getProfile().firstOrNull()
        val pairId = profile?.pairId?.ifBlank { sessionManager.currentPairId }.orEmpty()
        val buyerName = profile?.nickname?.ifBlank { "我" } ?: "我"
        val buyerAvatarUrl = profile?.avatarUrl.orEmpty()
        val candyCost = cart.totalPrice.toCandyCoinCost()
        if (!profileRepository.spendCandyCoins(candyCost)) {
            throw IllegalStateException("NOT_ENOUGH_CANDY_COINS")
        }
        val addressSnapshot = "${address.contactName} ${address.contactPhone} ${address.addressLine1} ${address.addressLine2}".trim()
        val order = OrderRecord(
            id = orderId,
            userId = userId,
            pairId = pairId,
            buyerName = buyerName,
            buyerAvatarUrl = buyerAvatarUrl,
            buyerRole = "eater",
            shopId = cart.shopId.orEmpty(),
            shopName = cart.shopName,
            shopCoverUrl = cart.shopCoverUrl,
            status = "submitted",
            addressSnapshot = addressSnapshot,
            buyerNote = note,
            subtotal = cart.subtotal,
            deliveryFee = cart.deliveryFee,
            totalPrice = cart.totalPrice,
            candyCoinsSpent = candyCost,
            createdAt = createdAt,
            items = cart.items.map {
                OrderItem(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    menuItemId = it.menuItemId,
                    menuItemName = it.menuItemName,
                    menuItemImageUrl = it.menuItemImageUrl,
                    unitPrice = it.unitPrice,
                    quantity = it.quantity,
                    subtotal = it.unitPrice * it.quantity
                )
            }
        )

        orderDao.upsertOrder(order.toEntity())
        orderDao.upsertItems(order.items.map { it.toEntity() })
        menuRepository.recordSales(order.items.groupingBy { it.menuItemId }.fold(0) { total, item -> total + item.quantity })

        if (sessionManager.isLoggedIn.value) {
            try {
                client.from("orders").insert(order.toRemotePayload()) { select() }
                client.from("order_items").insert(order.items.map { it.toRemotePayload() }) { select() }
            } catch (_: Exception) {
                // Local snapshot remains the source of truth when remote write fails.
            }
        }

        return orderId
    }

    override suspend fun updateOrderStatus(orderId: String, status: String) {
        val normalizedStatus = status.takeIf { it in ORDER_STATUSES } ?: return
        val previousOrder = orderDao.getOrderById(orderId)
        orderDao.updateOrderStatus(orderId, normalizedStatus)
        if (normalizedStatus == "cancelled" && previousOrder != null && previousOrder.status != "cancelled") {
            profileRepository.addCandyCoins(previousOrder.candyCoinsSpent)
        }
        if (sessionManager.isLoggedIn.value) {
            try {
                client.from("orders").update(
                    mapOf("status" to normalizedStatus)
                ) {
                    filter { eq("id", orderId) }
                }
            } catch (_: Exception) {
                // Local status remains the source of truth when remote sync fails.
            }
        }
    }

    override suspend fun refreshOrders() {
        syncRemoteOrders()
    }

    private suspend fun syncRemoteOrders() {
        if (!sessionManager.isLoggedIn.value) return
        val pairId = profileRepository.getProfile().firstOrNull()?.pairId
            ?.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }
            ?: sessionManager.currentPairId.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }
            ?: return

        try {
            val remoteOrders = client.from("orders").select {
                filter { eq("pair_id", pairId) }
            }.decodeList<RemoteOrderPayload>()

            remoteOrders.forEach { payload ->
                val orderItems = client.from("order_items").select {
                    filter { eq("order_id", payload.id) }
                }.decodeList<RemoteOrderItemPayload>().map { it.toDomain() }
                val order = payload.toDomain(orderItems)
                orderDao.upsertOrder(order.toEntity())
                orderDao.upsertItems(order.items.map { it.toEntity() })
            }
        } catch (_: Exception) {
            // Local orders remain usable if cloud fields have not been migrated yet.
        }
    }

    private fun OrderRecord.toRemotePayload() = RemoteOrderPayload(
        id = id,
        userId = userId,
        pairId = pairId,
        buyerName = buyerName,
        buyerAvatarUrl = buyerAvatarUrl,
        buyerRole = buyerRole,
        shopId = shopId,
        shopName = shopName,
        shopCoverUrl = shopCoverUrl,
        status = status,
        addressSnapshot = addressSnapshot,
        buyerNote = buyerNote,
        subtotal = subtotal,
        deliveryFee = deliveryFee,
        totalPrice = totalPrice,
        candyCoinsSpent = candyCoinsSpent,
        createdAt = createdAt
    )

    private fun RemoteOrderPayload.toDomain(items: List<OrderItem>) = OrderRecord(
        id = id,
        userId = userId,
        pairId = pairId,
        buyerName = buyerName,
        buyerAvatarUrl = buyerAvatarUrl,
        buyerRole = buyerRole,
        shopId = shopId,
        shopName = shopName,
        shopCoverUrl = shopCoverUrl,
        status = status,
        addressSnapshot = addressSnapshot,
        buyerNote = buyerNote,
        subtotal = subtotal,
        deliveryFee = deliveryFee,
        totalPrice = totalPrice,
        candyCoinsSpent = candyCoinsSpent,
        createdAt = createdAt,
        items = items
    )

    private fun OrderItem.toRemotePayload() = RemoteOrderItemPayload(
        id = id,
        orderId = orderId,
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        menuItemImageUrl = menuItemImageUrl,
        unitPrice = unitPrice,
        quantity = quantity,
        subtotal = subtotal
    )

    private fun RemoteOrderItemPayload.toDomain() = OrderItem(
        id = id,
        orderId = orderId,
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        menuItemImageUrl = menuItemImageUrl,
        unitPrice = unitPrice,
        quantity = quantity,
        subtotal = subtotal
    )

    private companion object {
        const val DEFAULT_PAIR_ID = "00000000-0000-0000-0000-000000000000"
        val ORDER_STATUSES = setOf("submitted", "confirmed", "delivering", "completed", "cancelled")
    }
}

private fun Double.toCandyCoinCost(): Int = ceil(this).toInt().coerceAtLeast(1)
