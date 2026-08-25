package com.example.treasure.utils

import androidx.palette.graphics.Palette
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

/**
 * Tests for PaletteCache singleton.
 * Verifies that the LRU cache correctly stores and retrieves Palette objects.
 */
class PaletteCacheTest {

    @Before
    fun setup() {
        PaletteCache.clear()
    }

    @Test
    fun `put and get returns the same palette object`() {
        val key = "https://example.com/image.jpg"
        val mockPalette = mockk<Palette>()
        
        PaletteCache.put(key, mockPalette)
        val retrieved = PaletteCache.get(key)
        
        assertThat(retrieved).isSameInstanceAs(mockPalette)
    }

    @Test
    fun `get returns null for non-existent key`() {
        val key = "non_existent_key"
        val retrieved = PaletteCache.get(key)
        
        assertThat(retrieved).isNull()
    }

    @Test
    fun `put overwrites existing value for the same key`() {
        val key = "shared_key"
        val firstPalette = mockk<Palette>()
        val secondPalette = mockk<Palette>()
        
        PaletteCache.put(key, firstPalette)
        PaletteCache.put(key, secondPalette)
        
        val retrieved = PaletteCache.get(key)
        assertThat(retrieved).isSameInstanceAs(secondPalette)
        assertThat(retrieved).isNotSameInstanceAs(firstPalette)
    }
}
