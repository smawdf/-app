package com.myorderapp.ui.checkout

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutAddressFieldsTest {

    @Test
    fun `checkout confirms cart and remark before submit`() {
        val screen = readMainSource("ui/checkout/CheckoutScreen.kt")
        val viewModel = readMainSource("ui/checkout/CheckoutViewModel.kt")

        assertTrue(screen.contains("确认点菜"))
        assertTrue(screen.contains("今晚这顿安排好啦"))
        assertTrue(screen.contains("点单明细"))
        assertTrue(screen.contains("共 \${cart.itemCount} 件"))
        assertTrue(screen.contains("给后厨的悄悄话"))
        assertTrue(screen.contains("口味、忌口、想说的话都可以写在这里"))
        assertTrue(screen.contains("errorMessage"))
        assertTrue(screen.contains("提交中..."))
        assertTrue(screen.contains("提交点菜 · 消耗 \$cost 糖糖币"))
        assertTrue(screen.contains("CheckoutCandyCoinsCard"))
        assertTrue(screen.contains("糖糖币"))
        assertTrue(screen.contains("不是金币"))
        assertTrue(screen.contains("点菜成功"))
        assertTrue(screen.contains("已经告诉对方啦～"))
        assertTrue(screen.contains("containerColor = if (enoughCandyCoins) Color(0xFFFF9FB7) else Color(0xFFFFDCE6)"))
        assertTrue(screen.contains("disabledContainerColor = Color(0xFFE7E2DC)"))
        assertTrue(screen.contains("statusBarsPadding()"))
        assertTrue(screen.contains("enabled = enabled"))
        assertFalse(screen.contains("收餐信息"))
        assertFalse(screen.contains("联系人"))
        assertFalse(screen.contains("联系电话"))
        assertFalse(screen.contains("收餐地址"))
        assertFalse(screen.contains("门牌或补充说明"))
        assertFalse(screen.contains("KeyboardType.Phone"))
        assertFalse(screen.contains("onContactNameChange"))
        assertFalse(screen.contains("onAddressLine1Change"))

        assertTrue(viewModel.contains("contactName = state.contactName.trim().ifBlank { \"到店取餐\" }"))
        assertTrue(viewModel.contains("addressLine1 = state.addressLine1.trim().ifBlank { \"本店\" }"))
        assertTrue(viewModel.contains("isSubmitting"))
        assertTrue(viewModel.contains("购物篮还是空的，先去点菜吧"))
        assertFalse(viewModel.contains("请填写联系人和收餐地址"))
        assertTrue(viewModel.contains("糖糖币不够啦，找饲养员撒点糖再点菜"))
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
