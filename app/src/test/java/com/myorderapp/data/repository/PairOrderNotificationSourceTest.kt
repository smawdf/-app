package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class PairOrderNotificationSourceTest {

    @Test
    fun `orders carry pair and buyer metadata for pair notifications`() {
        val orderModel = readMainSource("domain/model/OrderRecord.kt")
        val entity = readMainSource("data/local/entity/OrderEntity.kt")
        val repository = readMainSource("data/repository/SupabaseOrderRepository.kt")
        val database = readMainSource("data/local/AppDatabase.kt")
        val sql = readProjectFile("table/11_pair_order_notifications.sql")

        listOf("pairId", "buyerName", "buyerAvatarUrl", "buyerRole").forEach { field ->
            assertTrue(orderModel.contains(field))
            assertTrue(entity.contains(field))
        }

        assertTrue(repository.contains("@SerialName(\"pair_id\") val pairId"))
        assertTrue(repository.contains("@SerialName(\"buyer_name\") val buyerName"))
        assertTrue(repository.contains("if (pairId != null) eq(\"pair_id\", pairId)"))
        assertTrue(repository.contains("orderDao.upsertOrderWithItems("))
        assertTrue(repository.contains("profileRepository.getProfile().firstOrNull()"))
        assertTrue(database.contains("version = 13"))
        assertTrue(database.contains("MIGRATION_6_7"))
        assertTrue(database.contains("MIGRATION_7_8"))
        assertTrue(database.contains("MIGRATION_8_9"))
        assertTrue(database.contains("MIGRATION_9_10"))
        assertTrue(database.contains("MIGRATION_10_11"))
        assertTrue(database.contains("MIGRATION_11_12"))
        assertTrue(database.contains("MIGRATION_12_13"))
        assertTrue(sql.contains("add column if not exists pair_id"))
        assertTrue(sql.contains("Users can read pair orders"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )

        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath from ${Paths.get("").toAbsolutePath()}")

        return Files.readString(sourcePath)
    }

    private fun readProjectFile(relativePath: String): String {
        val candidates = listOf(
            Paths.get(relativePath),
            Paths.get("../").resolve(relativePath),
            Paths.get("../../").resolve(relativePath)
        )

        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Project file not found: $relativePath from ${Paths.get("").toAbsolutePath()}")

        return Files.readString(sourcePath)
    }
}
