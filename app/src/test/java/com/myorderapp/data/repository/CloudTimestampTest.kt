package com.myorderapp.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudTimestampTest {
    @Test
    fun `offset timestamps compare with local z timestamps`() {
        assertTrue("2026-07-14T01:27:37.432+00:00".isCloudTimestampAfter(""))
        assertTrue(
            "2026-07-14T01:27:37.432+00:00"
                .isCloudTimestampAfter("2026-07-12T17:14:55.197Z")
        )
        assertFalse(
            "2026-07-12T17:14:55.197Z"
                .isCloudTimestampAfter("2026-07-14T01:27:37.432+00:00")
        )
    }
}
