package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class UnpairHistorySourceTest {

    @Test
    fun `unpair archives shared records into each personal scope`() {
        val sql = readProjectFile("table/38_unpair_personal_history.sql")
        val repository = readMainSource("data/repository/SupabaseOrderRepository.kt")
        val orderScreen = readMainSource("ui/orders/OrdersScreen.kt")
        val detailScreen = readMainSource("ui/orders/OrderDetailScreen.kt")
        val profileScreen = readMainSource("ui/profile/ProfileScreen.kt")

        listOf(
            "viewer_user_ids",
            "auth.uid() = any(viewer_user_ids)",
            "insert into public.shop_settings",
            "insert into public.menu_dishes",
            "insert into public.anniversaries",
            "personal_scope := 'user:' || member_id::text"
        ).forEach { marker -> assertTrue("Missing unpair archive marker: $marker", sql.contains(marker)) }

        assertTrue(repository.contains("observeOrdersVisibleToUser"))
        assertTrue(repository.contains("viewerUserIdsJson"))
        assertTrue(orderScreen.contains("OrderShopAvatar(order = order)"))
        assertTrue(orderScreen.contains("order.shopCoverUrl.takeIf { it.isNotBlank() }"))
        assertTrue(detailScreen.contains("order.shopCoverUrl.takeIf { it.isNotBlank() }"))
        assertTrue(detailScreen.contains("model = order.buyerAvatarUrl"))
        assertTrue(detailScreen.contains("isHistoricalOrder"))
        assertTrue(profileScreen.contains("共同订单、店铺和纪念日会各自保留"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first { Files.exists(it) })
    }

    private fun readProjectFile(relativePath: String): String {
        val candidates = listOf(Paths.get(relativePath), Paths.get("..").resolve(relativePath))
        return Files.readString(candidates.first { Files.exists(it) })
    }
}
