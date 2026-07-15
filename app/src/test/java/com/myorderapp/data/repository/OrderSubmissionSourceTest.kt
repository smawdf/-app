package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderSubmissionSourceTest {
    @Test
    fun orderSubmissionUsesMutexAndRoomTransaction() {
        val repository = readMainSource("data/repository/SupabaseOrderRepository.kt")
        val dao = readMainSource("data/local/dao/OrderDao.kt")

        assertTrue(repository.contains("submitMutex.withLock"))
        assertTrue(repository.contains("upsertOrderWithItems"))
        assertTrue(repository.contains("profileRepository.spendCandyCoins(candyCost, orderId)"))
        assertTrue(repository.contains("profileRepository.refundCandyCoins(candyCost, orderId)"))
        assertTrue(repository.contains("from(\"orders\").upsert"))
        assertTrue(dao.contains("@Transaction"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first { Files.exists(it) })
    }
}
