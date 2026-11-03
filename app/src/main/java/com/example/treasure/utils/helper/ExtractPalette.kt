package com.example.treasure.utils.helper

import android.content.Context
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

/**
 * Asynchronously derives a dynamic accent [Color] from a remote game artwork image URL.
 *
 * Utilizes a multi-tiered pipeline:
 * 1. Checks the in-memory [PaletteCache] for previously parsed [Palette] instances.
 * 2. Fetches and downsamples the image bitmap via Coil 3 (forcing software bitmaps via `allowHardware(false)`
 *    to permit color quantization reading by AndroidX Palette).
 * 3. Generates a [Palette] on [Dispatchers.Default] to prevent main-thread UI frame dropping.
 * 4. Persists the result to [PaletteCache] and extracts the optimal swatch based on dark/light theme context.
 *
 * @param context Android context used to initialize the Coil [ImageLoader].
 * @param imageUrl The remote image web URL. If null or blank, [fallbackColor] is returned immediately.
 * @param fallbackColor Default [Color] returned in the event of missing URLs, network errors, or swatch absence.
 * @param isDark `true` if the UI is currently rendering in dark mode, `false` for light mode.
 * @return An extracted dynamic Compose [Color], or [fallbackColor] if extraction cannot be performed.
 */
suspend fun getDynamicColor(
    context: Context,
    imageUrl: String?,
    fallbackColor: Color,
    isDark: Boolean
): Color = withContext(Dispatchers.Default) {
    if (imageUrl.isNullOrBlank()) return@withContext fallbackColor

    val cachedPalette = PaletteCache.get(imageUrl)
    if (cachedPalette != null) {
        return@withContext extractColorFromPalette(cachedPalette, isDark, fallbackColor)
    }

    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .allowHardware(false)
        .size(100)
        .build()

    val result = loader.execute(request)

    if (result is SuccessResult) {
        val bitmap = result.image.toBitmap()
        val palette = Palette.from(bitmap).generate()
        PaletteCache.put(imageUrl, palette)
        return@withContext extractColorFromPalette(palette, isDark, fallbackColor)
    }

    return@withContext fallbackColor
}

/**
 * Selects an optimal [Palette.Swatch] from a generated [Palette] tailored to the active theme brightness.
 *
 * For Dark Mode, prioritization favours [Palette.getDominantSwatch], [Palette.getVibrantSwatch],
 * then [Palette.getMutedSwatch] for sufficient contrast against dark surfaces.
 * For Light Mode, prioritization favours [Palette.getLightVibrantSwatch], [Palette.getVibrantSwatch],
 * then [Palette.getLightMutedSwatch] to prevent harsh glare.
 *
 * @param palette The parsed [Palette] containing analyzed color swatches.
 * @param isDark `true` if the current UI theme is dark, `false` if light.
 * @param fallback Default [Color] returned if none of the prioritized swatches are present in the palette.
 * @return Resolved Compose [Color] matching the palette swatch or [fallback].
 */
internal fun extractColorFromPalette(
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
