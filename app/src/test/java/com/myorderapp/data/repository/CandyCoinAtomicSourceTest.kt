package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandyCoinAtomicSourceTest {
    @Test
    fun `partner recharge uses atomic rpc and writes ledger`() {
        val repository = readSource("data/repository/SupabaseProfileRepository.kt")
        val sql = Files.readString(projectPath("table/24_atomic_partner_candy_coins.sql"))

        assertTrue(repository.contains("add_partner_candy_coins_with_record"))
        assertTrue(repository.contains("add_partner_candy_coins"))
        assertTrue(repository.contains("isMissingPartnerRechargeRpc"))
        assertTrue(repository.contains("session.currentPairId"))
        assertFalse(repository.contains("filter { eq(\"user_id\", partner.userId) }"))
        assertTrue(sql.contains("caller_role <> 'caretaker'"))
        assertTrue(sql.contains("for update"))
        assertTrue(sql.contains("pg_advisory_xact_lock"))
        assertTrue(sql.contains("insert into public.candy_coin_records"))
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
