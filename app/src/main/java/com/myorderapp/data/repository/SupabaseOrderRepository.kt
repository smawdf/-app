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
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
private data class RemoteOrderPayload(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("shop_cover_url") val shopCoverUrl: String,
    val status: String,
    @SerialName("address_snapshot") val addressSnapshot: String,
    @SerialName("buyer_note") val buyerNote: String,
    val subtotal: Double,
    @SerialName("delivery_fee") val deliveryFee: Double,
    @SerialName("total_price") val totalPrice: Double,
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
    private val menuRepository: RoomMenuRepository
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
        val entity = orderDao.getOrderById(orderId) ?: return null
        val items = orderDao.getOrderItems(orderId).map { it.toDomain() }
        return entity.toDomain(items)
    }

    override suspend fun submitOrder(cart: CartState, address: Address, note: String): String {
        val orderId = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        val userId = sessionManager.currentUserId.ifBlank { "guest" }
        val addressSnapshot = "${address.contactName} ${address.contactPhone} ${address.addressLine1} ${address.addressLine2}".trim()
        val order = OrderRecord(
            id = orderId,
            userId = userId,
            shopId = cart.shopId.orEmpty(),
            shopName = cart.shopName,
            shopCoverUrl = cart.shopCoverUrl,
            status = "submitted",
            addressSnapshot = addressSnapshot,
            buyerNote = note,
            subtotal = cart.subtotal,
            deliveryFee = cart.deliveryFee,
            totalPrice = cart.totalPrice,
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
        orderDao.updateOrderStatus(orderId, normalizedStatus)
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

    private fun OrderRecord.toRemotePayload() = RemoteOrderPayload(
        id = id,
        userId = userId,
        shopId = shopId,
        shopName = shopName,
        shopCoverUrl = shopCoverUrl,
        status = status,
        addressSnapshot = addressSnapshot,
        buyerNote = buyerNote,
        subtotal = subtotal,
        deliveryFee = deliveryFee,
        totalPrice = totalPrice,
        createdAt = createdAt
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

    private companion object {
        val ORDER_STATUSES = setOf("submitted", "confirmed", "delivering", "completed", "cancelled")
    }
}
