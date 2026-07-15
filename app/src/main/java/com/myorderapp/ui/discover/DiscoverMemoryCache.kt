package com.myorderapp.ui.discover

internal data class CachedDiscoverSearch(
    val results: List<DiscoverDishSearchItem>,
    val partialNetworkUnavailable: Boolean
)

internal class DiscoverSearchMemoryCache(
    private val maxEntries: Int = 20,
    private val ttlMillis: Long = 5 * 60_000L,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private data class Entry(
        val value: CachedDiscoverSearch,
        val savedAt: Long
    )

    private val entries = object : LinkedHashMap<String, Entry>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean {
            return size > maxEntries
        }
    }

    @Synchronized
    fun get(query: String): CachedDiscoverSearch? {
        val key = query.cacheKey()
        val entry = entries[key] ?: return null
        if (nowMillis() - entry.savedAt > ttlMillis) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    fun put(query: String, value: CachedDiscoverSearch) {
        entries[query.cacheKey()] = Entry(value, nowMillis())
    }

    private fun String.cacheKey(): String = trim().lowercase().replace(Regex("\\s+"), "")
}
