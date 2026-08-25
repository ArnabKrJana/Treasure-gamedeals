package com.example.treasure.utils

import androidx.collection.LruCache
import androidx.palette.graphics.Palette

object PaletteCache {
    private const val CACHE_SIZE = 20


    private val cache = LruCache<String, Palette>(CACHE_SIZE)

    fun get(key: String): Palette? = cache[key]
    fun put(key: String, value: Palette) = cache.put(key, value)

    internal fun clear() = cache.evictAll()
}