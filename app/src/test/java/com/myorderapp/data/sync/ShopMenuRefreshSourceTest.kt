package com.myorderapp.data.sync

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopMenuRefreshSourceTest {
    @Test
    fun `active shop screens refresh cloud shop and menu`() {
        val orderingScreen = readSource("ui/order/OrderingScreen.kt")
        val orderingViewModel = readSource("ui/order/OrderingViewModel.kt")
        val managementScreen = readSource("ui/menu/MenuManagementScreen.kt")
        val managementViewModel = readSource("ui/menu/MenuManagementViewModel.kt")
        val shopRepository = readSource("data/repository/SingleShopRepository.kt")

        assertTrue(orderingScreen.contains("viewModel.refreshShopAndMenuFromCloud()"))
        assertTrue(managementScreen.contains("viewModel.refreshShopAndMenuFromCloud()"))
        assertTrue(orderingViewModel.contains("singleShopRepository.loadFromCloud()"))
        assertTrue(orderingViewModel.contains("roomMenuRepository.loadFromCloud()"))
        assertTrue(managementViewModel.contains("singleShopRepository.loadFromCloud()"))
        assertTrue(managementViewModel.contains("menuRepository.loadFromCloud()"))
        assertTrue(shopRepository.contains("if (localUpdatedAt.isBlank())"))
        assertTrue(shopRepository.contains("SHOP_INITIAL_SYNC_RETRY_MS"))
        assertTrue(shopRepository.contains("else if (localUpdatedAt.isNotBlank())"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        ).first(Files::exists)
    )
}
