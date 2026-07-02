package com.myorderapp.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeColorTest {
    @Test
    fun `primary theme color is low saturation ios blue`() {
        assertEquals(Color(0xFF5F95B5), Primary)
        assertNotEquals(Color(0xFF2F6B9A), Primary)
    }
}
