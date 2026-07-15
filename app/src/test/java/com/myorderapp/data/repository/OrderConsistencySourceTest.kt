package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderConsistencySourceTest {
    @Test
    fun `failed order writes retry under the initiating session`() {
        val repository = readSource("data/repository/SupabaseOrderRepository.kt")
        val worker = readSource("core/worker/OrderSyncWorker.kt")

        listOf("pending_create", "pending_status", "syncPendingOrders", "OrderSyncWorker.enqueue").forEach {
            assertTrue(repository.contains(it))
        }
        assertTrue(repository.contains("orderDao.getPendingOrders(userId)"))
        assertTrue(repository.contains("val sessionId = sessionManager.currentSessionId"))
        assertTrue(repository.contains("OrderSyncWorker.enqueue(context, userId, sessionId)"))
        assertTrue(repository.contains("syncState?.startsWith(\"pending_\")"))
        listOf("currentUserId != expectedUserId", "currentSessionId != expectedSessionId").forEach {
            assertTrue(worker.contains(it))
        }
        assertTrue(!worker.contains("checkSessionValid()"))
    }

    @Test
    fun `cancellation is performed by atomic buyer refund rpc`() {
        val repository = readSource("data/repository/SupabaseOrderRepository.kt")
        val sql = Files.readString(projectPath("table/23_atomic_order_transitions.sql"))

        assertTrue(repository.contains("cancel_order_and_refund"))
        assertTrue(repository.contains("transition_order_status"))
        assertTrue(sql.contains("drop policy if exists \"Users can update pair orders\""))
        assertTrue(sql.contains("invalid order transition"))
        assertTrue(sql.contains("for update"))
        assertTrue(sql.contains("target_order.user_id"))
        assertTrue(sql.contains("candy_coins = least(9999"))
        assertTrue(sql.contains("'refund-order-' || target_order.id::text"))
        assertTrue(sql.contains("insert into public.candy_coin_records"))
        assertTrue(sql.contains("grant execute on function public.cancel_order_and_refund(uuid) to authenticated"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        ).first(Files::exists)
    )

    private fun projectPath(relativePath: String) = listOf(
        Paths.get(relativePath),
        Paths.get("..").resolve(relativePath)
    ).first(Files::exists)
}
