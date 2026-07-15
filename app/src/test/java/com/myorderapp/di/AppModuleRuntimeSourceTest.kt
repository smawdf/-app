package com.myorderapp.di

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppModuleRuntimeSourceTest {
    @Test
    fun `runtime graph follows couple ordering main flow without legacy meal bindings`() {
        val appModule = readMainSource("di/AppModule.kt")
        val authViewModel = readMainSource("ui/auth/AuthViewModel.kt")
        val onboardingViewModel = readMainSource("ui/onboarding/OnboardingViewModel.kt")
        val cloudSyncCoordinator = readMainSource("data/sync/CloudSyncCoordinator.kt")

        listOf(
            "single<ShopRepository>",
            "single<MenuRepository>",
            "single<CartRepository>",
            "single<AddressRepository>",
            "single<OrderRepository>",
            "single { CloudSyncCoordinator(",
            "viewModelOf(::OrderingViewModel)",
            "viewModelOf(::MenuManagementViewModel)",
            "viewModel { OrdersViewModel(get(), get()) }",
            "viewModel { OrderDetailViewModel(get(), get()) }"
        ).forEach { expected ->
            assertTrue("Missing runtime dependency: $expected", appModule.contains(expected))
        }

        listOf(
            "RealtimeService",
            "InMemoryMealRepository",
            "SupabaseMealRepository",
            "MealRepository",
            "wishlistDao()"
        ).forEach { legacy ->
            assertFalse("Legacy runtime dependency remains: $legacy", appModule.contains(legacy))
        }

        assertFalse(authViewModel.contains("mealRepo"))
        assertFalse(onboardingViewModel.contains("mealRepo"))
        assertTrue(authViewModel.contains("cloudSyncCoordinator.syncInBackground()"))
        assertTrue(onboardingViewModel.contains("cloudSyncCoordinator.syncInBackground()"))
        assertTrue(cloudSyncCoordinator.contains("dishRepository.syncFromCloud()"))
        assertTrue(cloudSyncCoordinator.contains("profileRepository.loadFromCloud()"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath")
        return Files.readString(sourcePath)
    }
}
