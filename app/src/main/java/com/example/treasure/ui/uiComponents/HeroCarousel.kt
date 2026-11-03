package com.example.treasure.ui.uiComponents

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.ui.theme.TreasureTheme
import com.example.treasure.utils.ColorCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Full-width hero carousel composable showcasing highly anticipated upcoming video game titles.
 *
 * Implements:
 * - [HorizontalPager] with page offset-driven alpha fading (`graphicsLayer`).
 * - Animated page indicator dots with dynamic pill width transitions.
 * - Skeleton shimmer fallback ([HeroSkeletonShimmer]) while data is loading.
 *
 * @param games List of [GameCardItem] titles to display in the carousel.
 * @param favoriteIds Set of favorited game IDs for O(1) membership checks.
 * @param onToggleFavorite Callback invoked when toggling favorite state on a carousel title.
 * @param onGameClick Callback invoked when selecting a carousel title to open details.
 * @param modifier Composable modifier for carousel container.
 */
@SuppressLint("FrequentlyChangingValue")
@Composable
fun TreasureUpcomingCarousel(
    games: List<GameCardItem>,
    favoriteIds: Set<String>,
    onToggleFavorite: (GameCardItem) -> Unit,
    onGameClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (games.isEmpty()) {
        Box(modifier = modifier) {
            HeroSkeletonShimmer()
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { games.size })

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 0.dp,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset =
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val currentGame = games[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - (pageOffset * 0.4f).coerceIn(0f, 0.4f)
                    }
            ) {
                UpcomingCarouselItem(
                    game = currentGame,
                    isFavorite = currentGame.id in favoriteIds,
                    onToggleFavorite = onToggleFavorite,
                    onGameClick = onGameClick
                )
            }
        }

        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val isActive = pagerState.currentPage == iteration
                val color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f)
                val width = if (isActive) 24.dp else 8.dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .height(6.dp)
                        .width(width)
                )
            }
        }
    }
}

/**
 * Individual page slide composable within [TreasureUpcomingCarousel].
 *
 * @param game The [GameCardItem] title data.
 * @param isFavorite `true` if the title is marked as a user favorite.
 * @param onToggleFavorite Callback invoked when toggling favorite state.
 * @param onGameClick Callback invoked when tapping "Know More".
 */
@Composable
fun UpcomingCarouselItem(
    game: GameCardItem,
    isFavorite: Boolean,
    onToggleFavorite: (GameCardItem) -> Unit,
    onGameClick: (String) -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize()) {
        SubcomposeAsyncImage(
            model = game.thumbnail,
            contentDescription = game.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Loading -> HeroSkeletonShimmer()
                is AsyncImagePainter.State.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF121212)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Image not available",
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                else -> SubcomposeAsyncImageContent()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EXPECTED: ${formatReleaseDate(game.releaseDate)}",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = game.title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                game.genres.take(2).forEach { genre ->
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = genre,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onGameClick(game.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Know More", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { onToggleFavorite(game) },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) Color(0xFFFF5252) else Color.White
                    )
                }
            }
        }
    }
}

/**
 * Full-bleed animated skeleton shimmer placeholder rendered while carousel artwork loads.
 */
@Composable
fun HeroSkeletonShimmer() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "hero_shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
    )
}

/**
 * Formats a Unix epoch timestamp (in seconds) into human-readable release date string ("dd MMM yyyy").
 *
 * @param timestamp Epoch timestamp in seconds.
 * @return Formatted release date string, or "TBA" if null or 0.
 */
private fun formatReleaseDate(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "TBA"
    return try {
        val date = Date(timestamp * 1000)
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        format.format(date).uppercase(Locale.getDefault())
    } catch (e: Exception) {
        "TBA"
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun TreasureUpcomingCarouselPreview() {
    val dummyUpcomingGames = listOf(
        GameCardItem(
            id = "1",
            listingIndex = 0,
            title = "Forza Horizon 6",
            thumbnail = "https://assets.xboxservices.com/assets/22/4d/224d155f-8d3c-4f63-a810-d4fad0cf374e.jpg",
            store = "Steam",
            price = Price(0.0, 0.0),
            genres = listOf("Racing", "Open World"),
            releaseDate = 1779148800L,
            upVotes = UpVotes("", ColorCode.GREEN)
        )
    )

    TreasureTheme {
        Surface {
            TreasureUpcomingCarousel(
                games = dummyUpcomingGames,
                favoriteIds = setOf("1"),
                onToggleFavorite = {},
                onGameClick = {},
                modifier = Modifier.height(600.dp)
            )
        }
    }
}
