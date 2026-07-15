package com.myorderapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.myorderapp.data.local.entity.OrderEntity
import com.myorderapp.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE pairId = :pairId ORDER BY createdAt DESC")
    fun observeOrdersByPair(pairId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeOrdersByUser(userId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :orderId AND pairId = :pairId LIMIT 1")
    suspend fun getOrderByIdForPair(orderId: String, pairId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :orderId AND userId = :userId LIMIT 1")
    suspend fun getOrderByIdForUser(orderId: String, userId: String): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY id ASC")
    suspend fun getOrderItems(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM orders WHERE userId = :userId AND syncState LIKE 'pending_%' ORDER BY createdAt ASC")
    suspend fun getPendingOrders(userId: String): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun upsertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        upsertOrder(order)
        upsertItems(items)
    }

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId AND pairId = :pairId")
    suspend fun updateOrderStatusForPair(orderId: String, pairId: String, status: String)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId AND userId = :userId")
    suspend fun updateOrderStatusForUser(orderId: String, userId: String, status: String)

    @Query("UPDATE orders SET syncState = :syncState WHERE id = :orderId AND userId = :userId")
    suspend fun updateSyncState(orderId: String, userId: String, syncState: String)

    @Query("UPDATE orders SET status = :status, syncState = :syncState WHERE id = :orderId AND userId = :userId")
    suspend fun updateStatusAndSyncStateForUser(orderId: String, userId: String, status: String, syncState: String)

    @Query("UPDATE orders SET status = :status, syncState = :syncState WHERE id = :orderId AND pairId = :pairId")
    suspend fun updateStatusAndSyncStateForPair(orderId: String, pairId: String, status: String, syncState: String)
}
