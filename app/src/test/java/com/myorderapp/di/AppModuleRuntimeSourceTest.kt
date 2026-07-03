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

        listOf(
            "single<ShopRepository>",
            "single<MenuRepository>",
            "single<CartRepository>",
            "single<AddressRepository>",
            "single<OrderRepository>",
            "viewModelOf(::OrderingViewModel)",
            "viewModelOf(::MenuManagementViewModel)",
            "viewModel { OrdersViewModel(get()) }",
            "viewModel { OrderDetailViewModel(get()) }"
        ).forEach { expected ->
            assertTrue("主流程运行时依赖缺失：$expected", appModule.contains(expected))
        }

        listOf(
            "RealtimeService",
            "InMemoryMealRepository",
            "SupabaseMealRepository",
            "MealRepository",
            "wishlistDao()"
        ).forEach { legacy ->
            assertFalse("运行时 DI 不应再注入旧兼容模块：$legacy", appModule.contains(legacy))
        }

        assertFalse("登录后不应再同步旧餐次模块", authViewModel.contains("mealRepo"))
        assertFalse("注册后不应再同步旧餐次模块", onboardingViewModel.contains("mealRepo"))
        assertTrue("登录仍需同步当前菜品数据", authViewModel.contains("dishRepo.syncFromCloud()"))
        assertTrue("注册仍需同步当前菜品数据", onboardingViewModel.contains("dishRepo.syncFromCloud()"))
        assertTrue("登录仍需加载用户资料", authViewModel.contains("profileRepo.loadFromCloud()"))
        assertTrue("注册仍需加载用户资料", onboardingViewModel.contains("profileRepo.loadFromCloud()"))
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
