package com.myorderapp.ui.adddish

import org.junit.Assert.assertEquals
import org.junit.Test

class AddDishSaveMessagesTest {
    @Test
    fun `blank exception message uses fallback`() {
        val message = AddDishSaveMessages.primarySaveFailed(RuntimeException(""))

        assertEquals("保存失败，请重试", message)
    }

    @Test
    fun `exception message is trimmed`() {
        val message = AddDishSaveMessages.primarySaveFailed(RuntimeException("  网络不可用  "))

        assertEquals("保存失败：网络不可用", message)
    }

    @Test
    fun `long exception message is capped`() {
        val message = AddDishSaveMessages.primarySaveFailed(RuntimeException("a".repeat(120)))

        assertEquals("保存失败：${"a".repeat(80)}", message)
    }
}
