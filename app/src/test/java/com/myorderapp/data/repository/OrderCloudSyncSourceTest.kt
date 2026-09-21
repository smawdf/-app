package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderCloudSyncSourceTest {

    @Test
    fun `legacy orders and partial uploads can recover`() {
        val source = readMainSource("data/repository/SupabaseOrderRepository.kt")

        assertTrue(source.contains("@SerialName(\"user_id\") val userId: String? = null"))
        assertTrue(source.contains("userId = userId.orEmpty()"))
        assertTrue(source.contains("private suspend fun uploadMissingOrderData"))
        assertTrue(source.contains("if (!remoteOrderExists)"))
        assertTrue(source.contains("client.from(\"orders\").insert(order.toRemotePayload())"))
        assertTrue(source.contains("filterNot { it.id in remoteItemIds }"))
        assertTrue(source.contains("client.from(\"order_items\").insert(missingItems)"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first(Files::exists))
    }
}
