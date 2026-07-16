package com.myorderapp.data.remote.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionComparatorTest {
    @Test
    fun `minor and patch updates are detected`() {
        assertTrue(AppVersionComparator.isNewer("1.1.0", "1.0.9"))
        assertTrue(AppVersionComparator.isNewer("1.0.10", "1.0.9"))
    }

    @Test
    fun `same or older versions are ignored`() {
        assertFalse(AppVersionComparator.isNewer("1.0.1", "1.0.1"))
        assertFalse(AppVersionComparator.isNewer("1.0.0", "1.0.1"))
        assertFalse(AppVersionComparator.isNewer("v1.0.1", "1.0.1.0"))
    }
}
