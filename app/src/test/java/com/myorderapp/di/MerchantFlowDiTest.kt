package com.myorderapp.di

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantFlowDiTest {

    @Test
    fun `single shop dependencies are registered in app module`() {
        val source = readMainSource("di/AppModule.kt")

        assertTrue(source.contains("single<SingleShopRepository>"))
        assertTrue(source.contains("single<ShopRepository> { get<SingleShopRepository>() }"))
        assertTrue(source.contains("single<MenuRepository> { get<SingleShopRepository>() }"))
        assertTrue(source.contains("viewModelOf(::OrderingViewModel)"))
        assertTrue(source.contains("viewModelOf(::MenuManagementViewModel)"))
        assertTrue(source.contains("viewModelOf(::CartViewModel)"))
        assertTrue(source.contains("viewModelOf(::CheckoutViewModel)"))
        assertTrue(source.contains("viewModel { OrdersViewModel(get(), get()) }"))
        assertFalse(source.contains("SampleShopRepository"))
        assertFalse(source.contains("SampleMenuRepository"))
        assertFalse(source.contains("MerchantViewModel"))
        assertFalse(source.contains("ShopDetailViewModel"))
        assertFalse(source.contains("ShopSettingsViewModel"))
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
}
