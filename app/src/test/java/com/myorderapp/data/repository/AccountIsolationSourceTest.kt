package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountIsolationSourceTest {

    @Test
    fun `pairing is server validated and invitation profiles cannot be enumerated`() {
        val repository = readMainSource("data/repository/SupabaseProfileRepository.kt")
        val sql = readProjectFile("table/22_secure_pairing.sql")

        assertTrue(repository.contains("create_pair_invite"))
        assertTrue(repository.contains("preview_pair_invite"))
        assertTrue(repository.contains("join_pair_invite"))
        assertFalse(repository.contains("val chars = \"ABCDEFGHJKLMNPQRSTUVWXYZ23456789\""))
        assertTrue(sql.contains("drop policy if exists \"Authenticated users can preview pair invites\""))
        assertTrue(sql.contains("member_count <> 1"))
        assertTrue(sql.contains("Pair members can view pair profiles"))
    }

    @Test
    fun `local account data and deferred uploads are scoped`() {
        val orderDao = readMainSource("data/local/dao/OrderDao.kt")
        val addressDao = readMainSource("data/local/dao/AddressDao.kt")
        val shop = readMainSource("data/repository/SingleShopRepository.kt")
        val preferences = readMainSource("data/repository/UserPreferencesRepository.kt")
        val worker = readMainSource("core/worker/CloudImageUploadWorker.kt")

        assertTrue(orderDao.contains("observeOrdersByPair"))
        assertTrue(orderDao.contains("observeOrdersByUser"))
        assertTrue(addressDao.contains("WHERE userId = :userId"))
        assertTrue(menuDaoWritesArePairScoped())
        assertTrue(shop.contains("KEY_OWNER_USER_ID"))
        assertTrue(preferences.contains("KEY_OWNER_USER_ID"))
        assertTrue(worker.contains("KEY_USER_ID"))
        assertTrue(worker.contains("KEY_SESSION_ID"))
        assertTrue(worker.contains("session.currentUserId != expectedUserId"))
        assertTrue(worker.contains("session.currentSessionId != expectedSessionId"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first { Files.exists(it) })
    }

    private fun menuDaoWritesArePairScoped(): Boolean {
        val menuDao = readMainSource("data/local/dao/MenuDishDao.kt")
        return menuDao.contains("WHERE id = :id AND pairId = :pairId") &&
            menuDao.contains("WHERE id IN (:ids) AND pairId = :pairId") &&
            menuDao.contains("WHERE category = :oldName AND pairId = :pairId")
    }

    private fun readProjectFile(relativePath: String): String {
        val candidates = listOf(Paths.get(relativePath), Paths.get("..").resolve(relativePath))
        return Files.readString(candidates.first { Files.exists(it) })
    }
}
