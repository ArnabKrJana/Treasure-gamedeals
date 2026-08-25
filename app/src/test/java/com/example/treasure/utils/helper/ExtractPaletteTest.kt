package com.example.treasure.utils.helper

import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for ExtractPalette.kt utility.
 * Focuses on testing the extraction logic and edge cases like null URLs.
 */
class ExtractPaletteTest {

    @Test
    fun `getDynamicColor returns fallback when imageUrl is null`() = runTest {
        val result = getDynamicColor(
            context = mockk(),
            imageUrl = null,
            fallbackColor = Color.Red,
            isDark = false
        )
        assertThat(result).isEqualTo(Color.Red)
    }

    @Test
    fun `getDynamicColor returns fallback when imageUrl is empty`() = runTest {
        val result = getDynamicColor(
            context = mockk(),
            imageUrl = "",
            fallbackColor = Color.Blue,
            isDark = true
        )
        assertThat(result).isEqualTo(Color.Blue)
    }

    @Test
    fun `extractColorFromPalette picks dominant color in dark mode`() {
        val mockPalette = mockk<Palette>()
        val mockSwatch = mockk<Palette.Swatch>()
        // Mocking a specific RGB value (e.g., Red with full alpha)
        val colorInt = 0xFFFF0000.toInt()
        every { mockSwatch.rgb } returns colorInt

        every { mockPalette.dominantSwatch } returns mockSwatch
        
        val result = extractColorFromPalette(mockPalette, isDark = true, fallback = Color.Black)
        
        // Truth assertion
        assertThat(result).isEqualTo(Color(colorInt))
    }

    @Test
    fun `extractColorFromPalette picks light vibrant color in light mode`() {
        val mockPalette = mockk<Palette>()
        val mockSwatch = mockk<Palette.Swatch>()
        val colorInt = 0xFF00FF00.toInt() // Green
        every { mockSwatch.rgb } returns colorInt

        // Light mode priorities: lightVibrant -> vibrant -> lightMuted
        every { mockPalette.lightVibrantSwatch } returns mockSwatch
        
        val result = extractColorFromPalette(mockPalette, isDark = false, fallback = Color.Black)
        
        assertThat(result).isEqualTo(Color(colorInt))
    }

    @Test
    fun `extractColorFromPalette falls back to vibrant when light vibrant is missing in light mode`() {
        val mockPalette = mockk<Palette>()
        val mockSwatch = mockk<Palette.Swatch>()
        val colorInt = 0xFF0000FF.toInt() // Blue
        every { mockSwatch.rgb } returns colorInt

        every { mockPalette.lightVibrantSwatch } returns null
        every { mockPalette.vibrantSwatch } returns mockSwatch
        
        val result = extractColorFromPalette(mockPalette, isDark = false, fallback = Color.Black)
        
        assertThat(result).isEqualTo(Color(colorInt))
    }

    @Test
    fun `extractColorFromPalette uses fallback when no swatches available`() {
        val mockPalette = mockk<Palette>()
        
        // Mocking all potential swatches as null
        every { mockPalette.dominantSwatch } returns null
        every { mockPalette.vibrantSwatch } returns null
        every { mockPalette.mutedSwatch } returns null
        every { mockPalette.lightVibrantSwatch } returns null
        every { mockPalette.lightMutedSwatch } returns null
        
        val result = extractColorFromPalette(mockPalette, isDark = true, fallback = Color.Yellow)
        
        assertThat(result).isEqualTo(Color.Yellow)
    }
}
