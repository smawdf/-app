package com.myorderapp.ui.discover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoverSearchMemoryCacheTest {
    @Test
    fun cacheNormalizesQueriesAndExpiresEntries() {
        var now = 1_000L
        val cache = DiscoverSearchMemoryCache(maxEntries = 2, ttlMillis = 100L) { now }
        val result = CachedDiscoverSearch(
            results = listOf(DiscoverDishSearchItem("1", "番茄炒蛋", "", null, "local")),
            partialNetworkUnavailable = false
        )

        cache.put(" 番茄 炒蛋 ", result)
        assertEquals(result, cache.get("番茄炒蛋"))
        now += 101L
        assertNull(cache.get("番茄炒蛋"))
    }

    @Test
    fun cacheEvictsLeastRecentlyUsedEntry() {
        val cache = DiscoverSearchMemoryCache(maxEntries = 2)
        val value = CachedDiscoverSearch(emptyList(), false)
        cache.put("a", value)
        cache.put("b", value)
        cache.get("a")
        cache.put("c", value)

        assertNull(cache.get("b"))
        assertEquals(value, cache.get("a"))
    }
}
