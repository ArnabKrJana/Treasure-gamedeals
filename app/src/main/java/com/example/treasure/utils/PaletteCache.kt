package com.example.treasure.utils

import androidx.collection.LruCache
import androidx.palette.graphics.Palette

/**
 * In-memory Least-Recently-Used (LRU) cache dedicated to parsed Material [Palette] instances.
 *
 * Extracting dynamic color palettes from remote game cover artwork is computationally expensive
 * (involving image decompression and color quantization). This singleton cache avoids redundant
 * color extraction operations during rapid list scrolling and screen transitions.
 *
 * Thread Safety: Backed by [androidx.collection.LruCache], which provides synchronized access
 * for thread-safe concurrent reads and writes across background coroutines.
 */
object PaletteCache {
    /**
     * Maximum number of parsed [Palette] objects retained in memory.
     * Tuned to 20 items to balance instantaneous carousel responsiveness against memory consumption.
     */
    private const val CACHE_SIZE = 20

    private val cache = LruCache<String, Palette>(CACHE_SIZE)

    /**
     * Retrieves a previously cached [Palette] for the specified image URL key.
     *
     * @param key The remote image URL string used as the cache lookup key.
     * @return The cached [Palette] instance, or `null` if the key is not cached or was evicted.
     */
    fun get(key: String): Palette? = cache[key]

    /**
     * Stores a generated [Palette] associated with the given image URL key.
     *
     * @param key The remote image URL string.
     * @param value The parsed [Palette] object.
     */
    fun put(key: String, value: Palette) = cache.put(key, value)

    /**
     * Evicts all entries from the LRU cache.
     * Primarily utilized during low-memory conditions or when resetting UI state in automated tests.
     */
    internal fun clear() = cache.evictAll()
}
