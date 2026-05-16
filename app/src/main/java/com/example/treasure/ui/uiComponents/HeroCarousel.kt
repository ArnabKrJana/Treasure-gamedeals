package com.example.treasure.ui.uiComponents

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
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
import com.example.treasure.ui.theme.TreasureTheme
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import kotlin.math.absoluteValue

data class UpcomingGame(
    val id: String,
    val title: String,
    val imageUrl: String,
    val releaseDate: String,
    val genres: List<String>
)

@SuppressLint("FrequentlyChangingValue")
@Composable
fun TreasureUpcomingCarousel(
    games: List<UpcomingGame>,
    modifier: Modifier = Modifier
) {
    if (games.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { games.size })

    // Box allows the pagination dots to overlay directly on the image
    Box(
        modifier = modifier, // Takes fillMaxSize() from HomeScreen
        contentAlignment = Alignment.BottomCenter
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 0.dp, // 1. NO MORE GAPS
            modifier = Modifier.fillMaxSize() // 2. NO MORE 500dp HARDCODE
        ) { page ->

            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 3. Removed scaleX/scaleY so images sit perfectly flush
                        alpha = 1f - (pageOffset * 0.4f).coerceIn(0f, 0.4f)
                    }
            ) {
                UpcomingCarouselItem(game = games[page])
            }
        }

        // Pagination Dots - Lifted slightly above the overlapping sheet
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

@Composable
fun UpcomingCarouselItem(game: UpcomingGame) {
    val bgColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize()) {

        // Custom Shimmer Implementation
        SubcomposeAsyncImage(
            model = game.imageUrl,
            contentDescription = game.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        ) {
            val state = painter.state
            if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                HeroSkeletonShimmer()
            } else {
                SubcomposeAsyncImageContent()
            }
        }

        // TOP GRADIENT: Protects the Top App Bar text visibility
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

        // BOTTOM GRADIENT: Protects the Carousel text
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

        // Content Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // LIFT CONTENT UP so it doesn't get swallowed by the 7% overlap of the Hot Deals list
                .padding(bottom = 64.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EXPECTED: ${game.releaseDate.uppercase()}",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = game.title,
                color = Color.White,
                fontSize = 32.sp, // Cinematic size
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                game.genres.take(3).forEach { genre ->
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = genre,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { /* Navigate to detail */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Learn More", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { /* Notify logic */ },
                    modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Notify me", tint = Color.White)
                }
            }
        }
    }
}

// Custom Shimmer Background for the Hero Image
@Composable
fun HeroSkeletonShimmer() {
    val transition = rememberInfiniteTransition(label = "hero_shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color.DarkGray.copy(alpha = 0.6f),
            Color.Gray.copy(alpha = 0.4f),
            Color.DarkGray.copy(alpha = 0.6f)
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(modifier = Modifier.fillMaxSize().background(brush))
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun TreasureUpcomingCarouselPreview() {
    val dummyUpcomingGames = listOf(
        UpcomingGame(
            "1",
            "Forza Horizon 6",
            "https://assets.xboxservices.com/assets/22/4d/224d155f-8d3c-4f63-a810-d4fad0cf374e.jpg?n=0399951111277_Wallpaper_Tablet_2048x2048_01.jpg",
            "Coming 19 May 2026",
            listOf("Shooter", "Survival")
        ),
        UpcomingGame(
            "2",
            "GTA VI",
            "https://www.rockstargames.com/VI/_next/image?url=%2FVI%2F_next%2Fstatic%2Fmedia%2FJason_and_Lucia_02_With_Logos_square.b022b2d6.jpg&w=3024&q=75",
            "Fall 2026",
            listOf("Action", "Open World")
        )
    )

    TreasureTheme {
        Surface {
            TreasureUpcomingCarousel(games = dummyUpcomingGames, modifier = Modifier.height(600.dp))
        }
    }
}