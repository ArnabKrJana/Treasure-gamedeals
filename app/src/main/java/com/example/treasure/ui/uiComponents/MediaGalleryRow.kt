package com.example.treasure.ui.uiComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.treasure.ui.theme.TreasureTheme

/**
 * Horizontally scrolling media gallery row composable displaying video trailer thumbnails and screenshot cards.
 *
 * Implements a 16:9 aspect ratio standard with rounded corners (12dp), rendering a dedicated video trailer card
 * with play button overlay when [trailerUrl] is available, followed by interactive screenshot tiles.
 *
 * @param trailerUrl Optional video stream URL. If non-null, renders the trailer launch card.
 * @param thumbnailUrl Remote URL for the trailer's thumbnail poster image.
 * @param screenshots Optional list of high-definition in-game screenshot URLs.
 * @param onTrailerClick Callback invoked when tapping the trailer launch card.
 * @param onScreenshotClick Callback invoked when tapping a screenshot, receiving the item's index.
 * @param modifier Composable modifier for gallery container styling.
 */
@Composable
fun MediaGalleryRow(
    trailerUrl: String?,
    thumbnailUrl: String?,
    screenshots: List<String>?,
    onTrailerClick: () -> Unit,
    onScreenshotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (screenshots.isNullOrEmpty() && trailerUrl == null) return

    LazyRow(
        contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        if (trailerUrl != null) {
            item {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTrailerClick() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = "Trailer thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play trailer",
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                }
            }
        }

        itemsIndexed(screenshots ?: emptyList()) { index, screenshotUrl ->
            AsyncImage(
                model = screenshotUrl,
                contentDescription = null,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .width(280.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onScreenshotClick(index) },
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MediaGalleryRowPreview() {
    TreasureTheme {
        MediaGalleryRow(
            trailerUrl = "https://example.com/trailer.mp4",
            thumbnailUrl = "https://example.com/thumb.jpg",
            screenshots = listOf(
                "https://example.com/ss1.jpg",
                "https://example.com/ss2.jpg",
                "https://example.com/ss3.jpg"
            ),
            onTrailerClick = {},
            onScreenshotClick = {}
        )
    }
}
