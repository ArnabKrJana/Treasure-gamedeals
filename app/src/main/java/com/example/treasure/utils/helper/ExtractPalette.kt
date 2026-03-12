package com.example.treasure.utils.helper



import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.example.treasure.utils.PaletteCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getDynamicColor(
    context: Context,
    imageUrl: String?,
    fallbackColor: Color,
    isDark: Boolean
): Color = withContext(Dispatchers.Default) {

    if (imageUrl.isNullOrBlank()) return@withContext fallbackColor

    // 1. Check Cache
    val cachedPalette = PaletteCache.get(imageUrl)
    if (cachedPalette != null) {
        return@withContext extractColorFromPalette(cachedPalette, isDark, fallbackColor)
    }

    // 2. Fetch Bitmap using Coil
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .allowHardware(false) // Palette needs a software bitmap
        .size(100) // Optimization: Small images are faster to process
        .build()

    val result = loader.execute(request)

    if (result is SuccessResult) {
        val bitmap = result.image.toBitmap()

        // 3. Generate Palette
        val palette = Palette.from(bitmap).generate()

        // 4. Cache it
        PaletteCache.put(imageUrl, palette)

        return@withContext extractColorFromPalette(palette, isDark, fallbackColor)
    }

    return@withContext fallbackColor
}

// Helper to pick the right color based on Theme (Dark/Light)
private fun extractColorFromPalette(
    palette: Palette,
    isDark: Boolean,
    fallback: Color
): Color {
    val swatch = if (isDark) {
        palette.dominantSwatch
            ?: palette.vibrantSwatch
            ?: palette.mutedSwatch

    } else {
        palette.lightVibrantSwatch
            ?: palette.vibrantSwatch
            ?: palette.lightMutedSwatch

    }

    return if (swatch != null) Color(swatch.rgb) else fallback
}