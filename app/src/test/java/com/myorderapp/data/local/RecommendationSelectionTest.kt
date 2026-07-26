package com.myorderapp.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationSelectionTest {

    @Test
    fun `selection stays stable during the same day`() {
        val epochDay = 20_930L

        assertEquals(
            stableRecommendationIndex(epochDay, salt = 101L, size = 500),
            stableRecommendationIndex(epochDay, salt = 101L, size = 500)
        )
    }

    @Test
    fun `selection varies across days and recommendation types`() {
        val dailySelections = (0L until 30L)
            .map { stableRecommendationIndex(it, salt = 101L, size = 500) }
            .toSet()

        assertTrue(dailySelections.size >= 20)
        assertNotEquals(
            stableRecommendationIndex(epochDay = 20_930L, salt = 101L, size = 500),
            stableRecommendationIndex(epochDay = 20_930L, salt = 202L, size = 500)
        )
    }

    @Test
    fun `selection always stays inside candidate range`() {
        (1..50).forEach { size ->
            val index = stableRecommendationIndex(epochDay = -20_930L, salt = 101L, size = size)
            assertTrue(index in 0 until size)
        }
    }
}
