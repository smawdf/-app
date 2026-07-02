package com.myorderapp.ui.checkout

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutAddressFieldsTest {

    @Test
    fun `checkout collects address and remark before submit`() {
        val screen = readMainSource("ui/checkout/CheckoutScreen.kt")
        val viewModel = readMainSource("ui/checkout/CheckoutViewModel.kt")

        assertTrue(screen.contains("收餐信息"))
        assertTrue(screen.contains("联系人"))
        assertTrue(screen.contains("联系电话"))
        assertTrue(screen.contains("收餐地址"))
        assertTrue(screen.contains("订单备注"))
        assertTrue(screen.contains("errorMessage"))
        assertTrue(screen.contains("提交中..."))
        assertTrue(viewModel.contains("contactName"))
        assertTrue(viewModel.contains("contactPhone"))
        assertTrue(viewModel.contains("addressLine1"))
        assertTrue(viewModel.contains("isSubmitting"))
        assertTrue(viewModel.contains("购物车为空，先去点菜吧"))
        assertTrue(viewModel.contains("请填写联系人和收餐地址"))
        assertTrue(viewModel.contains("提交失败，请稍后再试"))
        assertTrue(viewModel.contains("if (state.isSubmitting) return"))
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
