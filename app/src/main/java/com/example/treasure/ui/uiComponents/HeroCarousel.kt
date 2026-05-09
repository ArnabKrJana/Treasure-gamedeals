package com.example.treasure.ui.uiComponents

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import com.example.treasure.R // Assuming this is your R file location

// 1. Data Model Updated for Upcoming Games
data class UpcomingGame(
    val title: String,
    val imageRes: Int, // Using Int for offline drawable resources
    val releaseDate: String,
    val genres: List<String>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TreasureUpcomingCarousel(games: List<UpcomingGame>) {
    val pagerState = rememberPagerState(pageCount = { games.size })

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
//            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) { page ->
            // Adding Sliding Animation (Scale and Alpha fade)
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Shrink the unfocused cards slightly
                        val scale = 1f - (pageOffset * 0.1f).coerceIn(0f, 0.1f)
                        scaleX = scale
                        scaleY = scale
                        // Fade out the unfocused cards slightly
                        alpha = 1f - (pageOffset * 0.3f).coerceIn(0f, 0.3f)
                    }
            ) {
                UpcomingCarouselItem(game = games[page])
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pagination Dots adapting to Light/Dark theme
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val isActive = pagerState.currentPage == iteration
                val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                val width = if (isActive) 18.dp else 6.dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        // 1. Background Image (Offline Drawable)
        Image(
            painter = painterResource(id = game.imageRes),
            contentDescription = game.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Gradient Overlay (Always dark to ensure white text readability over images)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.95f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        startY = Float.POSITIVE_INFINITY,
                        endY = 0f
                    )
                )
        )

        // 3. Content Area (Bottom Left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Expected Release Date
            Text(
                text = "EXPECTED: ${game.releaseDate.uppercase()}",
                color = MaterialTheme.colorScheme.primaryContainer, // Pops against dark gradient
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Title
            Text(
                text = game.title,
                color = Color.White, // Always white because of the dark gradient overlay
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 34.sp,
                textAlign= TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Genres Row (Max 3)
            Row(
                modifier = Modifier.padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                game.genres.take(3).forEach { genre ->
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = genre,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action: Learn More (Capsule)
                Button(
                    onClick = { /* TODO: Navigate to detail screen */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Learn More",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Learn More",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Secondary Action: Notify Me (+)
                IconButton(
                    onClick = { /* TODO: Set reminder/notification */ },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, // Or Icons.Default.Notifications
                        contentDescription = "Notify me when arrived",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// --- Mocks and Previews ---

val mockUpcomingGames = listOf(
    UpcomingGame(
        title = "S.T.A.L.K.E.R. 2: Heart of Chornobyl",
        imageRes = R.drawable.bg_0013, // Using your actual drawable filenames
        releaseDate = "Q3 2026",
        genres = listOf("Shooter", "Survival", "Horror")
    ),
    UpcomingGame(
        title = "Grand Theft Auto VI",
        imageRes = R.drawable.bg_0014,
        releaseDate = "Fall 2026",
        genres = listOf("Action", "Open World", "Adventure", "Crime") // 4th will be ignored by .take(3)
    ),
    UpcomingGame(
        title = "The Witcher 4: Polaris",
        imageRes = R.drawable.bg_0015,
        releaseDate = "TBA",
        genres = listOf("RPG", "Fantasy")
    )
)

// Preview for Light Theme
@Preview(name = "Light Theme", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PreviewLightTreasureUpcomingCarousel() {
    MaterialTheme { // Wrap in MaterialTheme to resolve colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
//                .padding(vertical = 16.dp)
        ) {
            TreasureUpcomingCarousel(games = mockUpcomingGames)
        }
    }
}

// Preview for Dark Theme
@Preview(name = "Dark Theme", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDarkTreasureUpcomingCarousel() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
//                .padding(vertical = 16.dp)
        ) {
            TreasureUpcomingCarousel(games = mockUpcomingGames)
        }
    }
}