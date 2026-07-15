package com.myorderapp.data.repository

import android.content.Context
import com.myorderapp.core.worker.OrderSyncWorker
import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.EntityMapper.toEntity
import com.myorderapp.data.local.dao.OrderDao
import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.domain.model.Address
import com.myorderapp.domain.model.CartState
import com.myorderapp.domain.model.OrderItem
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.model.ROLE_CARETAKER
import com.myorderapp.domain.model.ROLE_EATER
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

@Serializable
private data class CancelOrderResult(
    @SerialName("order_status") val orderStatus: String,
    @SerialName("refunded_coins") val refundedCoins: Int
)

class SupabaseOrderRepository(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val orderDao: OrderDao,
    private val menuRepository: RoomMenuRepository,
    private val profileRepository: ProfileRepository,
    private val cloudErrorLogger: CloudErrorLogger? = null
) : OrderRepository {

    private val client = SupabaseClientProvider.client
    private val submitMutex = Mutex()

    override fun observeOrders(): Flow<List<OrderRecord>> {
        val source = activePairId()?.let(orderDao::observeOrdersByPair)
            ?: orderDao.observeOrdersByUser(activeUserId())
        return source.map { entities ->
            entities.map { order ->
                val items = orderDao.getOrderItems(order.id).map { it.toDomain() }
                order.toDomain(items)
            }
        }
    }

    override suspend fun getOrderById(orderId: String): OrderRecord? {
        syncRemoteOrders()
        val entity = scopedOrder(orderId) ?: return null
        val items = orderDao.getOrderItems(orderId).map { it.toDomain() }
        return entity.toDomain(items)
    }

    override suspend fun submitOrder(cart: CartState, address: Address, note: String): String {
        return submitMutex.withLock {
            submitOrderLocked(cart, address, note)
        }
    }

    private suspend fun submitOrderLocked(cart: CartState, address: Address, note: String): String {
        val orderId = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        val userId = sessionManager.currentUserId.ifBlank { "guest" }
        val profile = profileRepository.getProfile().firstOrNull()
        if (profile?.selectedRole != ROLE_EATER) {
            throw IllegalStateException("EATER_ROLE_REQUIRED")
        }
        val pairId = profile?.pairId?.ifBlank { sessionManager.currentPairId }.orEmpty()
        val buyerName = profile?.nickname?.ifBlank { "我" } ?: "我"
        val buyerAvatarUrl = profile?.avatarUrl.orEmpty()
        val candyCost = cart.totalPrice.toCandyCoinCost()
        if (!profileRepository.spendCandyCoins(candyCost, orderId)) {
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

        try {
            orderDao.upsertOrderWithItems(
                order.toEntity().copy(syncState = if (sessionManager.isLoggedIn.value) SYNC_PENDING_CREATE else SYNC_LOCAL_ONLY),
                order.items.map { it.toEntity() }
            )
        } catch (error: Exception) {
            profileRepository.refundCandyCoins(candyCost, orderId)
            throw error
        }
        runCatching {
            menuRepository.recordSales(order.items.groupingBy { it.menuItemId }.fold(0) { total, item -> total + item.quantity })
        }.onFailure { error ->
            cloudErrorLogger?.log("orders", "record_sales", error, "orderId=$orderId")
        }

        if (sessionManager.isLoggedIn.value) {
            try {
                client.from("orders").upsert(order.toRemotePayload()) { select() }
                client.from("order_items").upsert(order.items.map { it.toRemotePayload() }) { select() }
                orderDao.updateSyncState(orderId, userId, SYNC_SYNCED)
            } catch (e: Exception) {
                cloudErrorLogger?.log("orders", "submit", e, "orderId=$orderId pairId=$pairId")
                enqueueSync()
            }
        }

        return orderId
    }

    override suspend fun updateOrderStatus(orderId: String, status: String) {
        val normalizedStatus = status.takeIf { it in ORDER_STATUSES } ?: return
        if (normalizedStatus != "cancelled") {
            val selectedRole = profileRepository.getProfile().firstOrNull()?.selectedRole
            if (selectedRole != ROLE_CARETAKER) {
                throw IllegalStateException("CARETAKER_ROLE_REQUIRED")
            }
        }
        val previousOrder = scopedOrder(orderId)
        if (previousOrder == null || previousOrder.status == normalizedStatus) return
        if (sessionManager.isLoggedIn.value) {
            try {
                if (normalizedStatus == "cancelled") {
                    client.postgrest.rpc(
                        function = "cancel_order_and_refund",
                        parameters = mapOf("target_order_id" to orderId)
                    ).decodeSingle<CancelOrderResult>()
                    profileRepository.refreshCandyWalletBalance()
                } else {
                    client.postgrest.rpc(
                        function = "transition_order_status",
                        parameters = mapOf("target_order_id" to orderId, "new_status" to normalizedStatus)
                    ).decodeAs<String>()
                }
                updateLocalStatus(orderId, normalizedStatus, SYNC_SYNCED)
                return
            } catch (e: Exception) {
                cloudErrorLogger?.log("orders", "update_status", e, "orderId=$orderId status=$normalizedStatus")
            }
        }
        updateLocalStatus(orderId, normalizedStatus, SYNC_PENDING_STATUS)
        enqueueSync()
    }

    override suspend fun refreshOrders() {
        runCatching { syncPendingOrders() }
        syncRemoteOrders()
    }

    suspend fun syncPendingOrders() {
        if (!sessionManager.isLoggedIn.value) return
        val userId = activeUserId()
        orderDao.getPendingOrders(userId).forEach { entity ->
            runCatching {
                val items = orderDao.getOrderItems(entity.id).map { it.toDomain() }
                val order = entity.toDomain(items)
                if (entity.syncState == SYNC_PENDING_CREATE) {
                    val initialOrder = if (order.status == "cancelled") order.copy(status = "submitted") else order
                    client.from("orders").upsert(initialOrder.toRemotePayload()) { select() }
                    client.from("order_items").upsert(order.items.map { it.toRemotePayload() }) { select() }
                }
                if (order.status == "cancelled") {
                    client.postgrest.rpc(
                        function = "cancel_order_and_refund",
                        parameters = mapOf("target_order_id" to order.id)
                    ).decodeSingle<CancelOrderResult>()
                    profileRepository.refreshCandyWalletBalance()
                } else if (entity.syncState == SYNC_PENDING_STATUS) {
                    client.postgrest.rpc(
                        function = "transition_order_status",
                        parameters = mapOf("target_order_id" to order.id, "new_status" to order.status)
                    ).decodeAs<String>()
                }
                orderDao.updateSyncState(order.id, userId, SYNC_SYNCED)
            }.onFailure { error ->
                cloudErrorLogger?.log("orders", "retry_sync", error, "orderId=${entity.id}")
                throw error
            }
        }
    }

    private suspend fun syncRemoteOrders() {
        if (!sessionManager.isLoggedIn.value) return
        val pairId = profileRepository.getProfile().firstOrNull()?.pairId
            ?.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }
            ?: sessionManager.currentPairId.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }

        try {
            val remoteOrders = client.from("orders").select {
                filter {
                    if (pairId != null) eq("pair_id", pairId)
                    else eq("user_id", activeUserId())
                }
            }.decodeList<RemoteOrderPayload>()

            remoteOrders.forEach { payload ->
                if (orderDao.getOrderById(payload.id)?.syncState?.startsWith("pending_") == true) {
                    return@forEach
                }
                val orderItems = client.from("order_items").select {
                    filter { eq("order_id", payload.id) }
                }.decodeList<RemoteOrderItemPayload>().map { it.toDomain() }
                val order = payload.toDomain(orderItems)
                orderDao.upsertOrder(order.toEntity())
                orderDao.upsertItems(order.items.map { it.toEntity() })
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("orders", "refresh", e, "pairId=${pairId.orEmpty()} userId=${activeUserId()}")
            // Local orders remain usable if cloud fields have not been migrated yet.
        }
    }

    private suspend fun scopedOrder(orderId: String) = activePairId()?.let { pairId ->
        orderDao.getOrderByIdForPair(orderId, pairId)
    } ?: orderDao.getOrderByIdForUser(orderId, activeUserId())

    private fun activePairId(): String? {
        return sessionManager.currentPairId
            .takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }
    }

    private fun activeUserId(): String = sessionManager.currentUserId.ifBlank { "guest" }

    private suspend fun updateLocalStatus(orderId: String, status: String, syncState: String) {
        activePairId()?.let { pairId ->
            orderDao.updateStatusAndSyncStateForPair(orderId, pairId, status, syncState)
        } ?: orderDao.updateStatusAndSyncStateForUser(orderId, activeUserId(), status, syncState)
    }

    private fun enqueueSync() {
        val userId = sessionManager.currentUserId
        val sessionId = sessionManager.currentSessionId
        if (userId.isNotBlank() && sessionId.isNotBlank()) {
            OrderSyncWorker.enqueue(context, userId, sessionId)
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
        val ORDER_STATUSES = setOf("submitted", "confirmed", "preparing", "delivering", "completed", "cancelled")
        const val SYNC_SYNCED = "synced"
        const val SYNC_LOCAL_ONLY = "local_only"
        const val SYNC_PENDING_CREATE = "pending_create"
        const val SYNC_PENDING_STATUS = "pending_status"
    }
}

private fun Double.toCandyCoinCost(): Int = ceil(this).toInt().coerceAtLeast(1)
