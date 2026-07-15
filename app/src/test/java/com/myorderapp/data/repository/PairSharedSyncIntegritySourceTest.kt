package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairSharedSyncIntegritySourceTest {

    @Test
    fun `cloud candy changes do not create duplicate local ledger records`() {
        val source = readMain("data/repository/SupabaseProfileRepository.kt")
        val start = source.indexOf("private suspend fun updateCandyWalletAfterCloudChange")
        val end = source.indexOf("private fun Throwable.isInsufficientCandyCoins", start)
        val function = source.substring(start, end)

        assertTrue(function.contains("cacheCandyBalanceLocally"))
        assertTrue(function.contains("candyCoinLedgerRepository.loadFromCloud"))
        assertFalse(function.contains("persistCandyCoinsLocally"))
        assertTrue(source.contains("decodeList<ProfileCandyBalanceRow>()"))
    }

    @Test
    fun `profile edits preserve pair role session and candy fields`() {
        val source = readMain("data/repository/SupabaseProfileRepository.kt")

        assertTrue(source.contains("persistEditableProfile(normalized)"))
        assertTrue(source.contains("ProfileEditableUpdate"))
        assertTrue(source.contains("profileExists"))
    }

    @Test
    fun `menu deletions and mutable dish fields synchronize across devices`() {
        val source = readMain("data/repository/RoomMenuRepository.kt")
        val sql = readProject("table/33_pair_sync_integrity.sql")

        assertTrue(source.contains("val deletedAt: String? = null"))
        assertTrue(source.contains("MenuDishDeletionUpdate"))
        assertTrue(source.contains("filter { it.deletedAt != null }"))
        assertTrue(source.contains("incrementMonthlySales"))
        assertTrue(source.contains("syncMenuDishToCloud(it)"))
        assertTrue(sql.contains("add column if not exists deleted_at"))
    }

    @Test
    fun `recharge ledger belongs to the eater wallet`() {
        val sql = readProject("table/33_pair_sync_integrity.sql")

        assertTrue(sql.contains("and selected_role = 'eater'"))
        assertTrue(sql.contains("record_id, caller_pair_id, partner_id, 'recharge'"))
    }

    @Test
    fun `shop cache is reset when pair scope changes`() {
        val source = readMain("data/repository/SingleShopRepository.kt")
        val branch = source.substring(source.indexOf("ownerScope != currentScope"))

        assertTrue(branch.contains("shopSyncJob?.cancel()"))
        assertTrue(branch.contains(".clear()"))
    }

    private fun readMain(relativePath: String): String = Files.readString(
        existingPath(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
    )

    private fun readProject(relativePath: String): String = Files.readString(
        existingPath(Paths.get(relativePath), Paths.get("..").resolve(relativePath))
    )

    private fun existingPath(vararg candidates: Path): Path = candidates.first(Files::exists)
}
