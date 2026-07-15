package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class EaterCandyWalletSourceTest {
    @Test
    fun `management checkout spending and refund share eater wallet`() {
        val profileRepository = readMainSource("data/repository/SupabaseProfileRepository.kt")
        val checkout = readMainSource("ui/checkout/CheckoutViewModel.kt")
        val management = readMainSource("ui/candy/CandyCoinsViewModel.kt")
        val screen = readMainSource("ui/candy/CandyCoinsScreen.kt")
        val sql = Files.readString(projectPath("table/31_eater_candy_wallet.sql"))

        assertTrue(profileRepository.contains("observeCandyWalletBalance"))
        assertTrue(profileRepository.contains("spend_eater_candy_coins"))
        assertTrue(profileRepository.contains("refund_eater_candy_coins"))
        assertTrue(profileRepository.contains("parameters = CandyCoinTransactionParams(amount, transactionId)"))
        assertTrue(profileRepository.contains("parameters = PartnerRechargeParams("))
        assertTrue(checkout.contains("observeCandyWalletBalance().collect"))
        assertTrue(management.contains("observeCandyWalletBalance().collect"))
        assertTrue(screen.contains("val balance = uiState.walletBalance"))
        assertTrue(sql.contains("selected_role = 'eater'"))
        assertTrue(sql.contains("'spend-order-' || record_id::text"))
        assertTrue(sql.contains("wallet_user_id := coalesce(wallet_user_id, target_order.user_id)"))
    }

    private fun readMainSource(relativePath: String): String = Files.readString(
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
