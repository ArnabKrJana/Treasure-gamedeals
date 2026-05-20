package com.example.treasure.ui.uiComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.example.treasure.ui.theme.TreasureTheme

@Composable
fun GameHeader(
    bannerUrl: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier.height(280.dp)) {
        // Banner Image
        AsyncImage(
            model = bannerUrl,
            contentDescription = null,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )

        // Top Gradient for App Bar text visibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            bgColor.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Thumbnail Image
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .padding(start = 16.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(8.dp),
                    clip = false
                )
                .size(100.dp, 140.dp)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
            ,
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GameHeaderPreview() {
    TreasureTheme {
        GameHeader(
            bannerUrl = "https://cdn.akamai.steamstatic.com/steam/apps/1091500/header.jpg",
            thumbnailUrl = "https://cdn.akamai.steamstatic.com/steam/apps/1091500/capsule_617x353.jpg",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
