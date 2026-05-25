package com.example.treasure.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.ui.uiComponents.AnimatedShimmer
import com.example.treasure.ui.uiComponents.GameCard
import com.example.treasure.ui.uiComponents.TreasureUpcomingCarousel
import com.example.treasure.ui.viewModels.HomeScreenViewModel
import com.example.treasure.utils.ColorCode
import kotlinx.coroutines.flow.flowOf

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    onCardClick: (String) -> Unit
) {
    val hotDeals = viewModel.hotDeals.collectAsLazyPagingItems()
    val lowestPriceDeals = viewModel.lowestPriceDeals.collectAsLazyPagingItems()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    // 1. Observe the Anticipated Games from the ViewModel
    val anticipatedGames by viewModel.anticipatedGames.collectAsStateWithLifecycle()

    HomeScreenContent(
        modifier = modifier,
        hotDeals = hotDeals,
        lowestPriceDeals = lowestPriceDeals,
        anticipatedGames = anticipatedGames, // 2. Pass it down
        favoriteIds = favoriteIds,
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onCardClick = onCardClick
    )
}

@SuppressLint("FrequentlyChangingValue")
@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    hotDeals: LazyPagingItems<GameCardItem>,
    lowestPriceDeals: LazyPagingItems<GameCardItem>,
    anticipatedGames: List<GameCardItem>, // 3. Accept the new parameter
    favoriteIds: Set<String>,
    onToggleFavorite: (GameCardItem) -> Unit,
    onCardClick: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val hotError = hotDeals.loadState.refresh as? LoadState.Error
    val lowestError = lowestPriceDeals.loadState.refresh as? LoadState.Error

    LaunchedEffect(hotError, lowestError) {
        if (hotError != null || lowestError != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Failed to load deals",
                actionLabel = "Retry",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                hotDeals.retry()
                lowestPriceDeals.retry()
            }
        }
    }

    val mainListState = rememberSaveable(key = "home_vertical", saver = LazyListState.Saver) {
        LazyListState()
    }
    val bottomPadding = 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Calculate Dynamic Screen Height (Carousel takes 82% of screen)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val heroHeight = screenHeight * 0.82f

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = mainListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {

            // --- 1. THE HERO CAROUSEL ---
            item(key = "hero_carousel") {
                TreasureUpcomingCarousel(
                    games = anticipatedGames, // 4. Use the real backend data
                    onGameClick = onCardClick, // 5. Wire up the navigation
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .graphicsLayer {
                            // Parallax: Slowly moves down as user scrolls up
                            val scrollOffset = if (mainListState.firstVisibleItemIndex == 0) {
                                mainListState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                heroHeight.toPx()
                            }
                            translationY = scrollOffset * 0.5f
                            // Fade effect
                            alpha = 1f - (scrollOffset / (heroHeight.toPx() * 0.8f)).coerceIn(0f, 1f)
                        }
                )
            }

            // --- 2. HOT DEALS ---
            item(key = "header_hot") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 16.dp)
                ) {
                    SectionHeader(title = "Hot Deals", subtitle = "Trending right now")
                    GameHorizontalList(
                        deals = hotDeals,
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        storageKey = "list_hot",
                        onCardClick = onCardClick
                    )
                }
            }

            // --- 3. LOWEST PRICE EVER ---
            item(key = "header_lowest") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "Lowest Price Ever",
                        subtitle = "Historic low prices you won't see again"
                    )
                    GameHorizontalList(
                        deals = lowestPriceDeals,
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        storageKey = "list_lowest",
                        onCardClick = onCardClick
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GameHorizontalList(
    deals: LazyPagingItems<GameCardItem>,
    favoriteIds: Set<String>,
    onToggleFavorite: (GameCardItem) -> Unit,
    storageKey: String,
    onCardClick: (String) -> Unit
) {
    val loadState = deals.loadState.refresh

    if (loadState is LoadState.Loading && deals.itemCount == 0) {
        ShimmerRow()
    } else {
        RealGameRow(deals, favoriteIds, onToggleFavorite, storageKey, onCardClick)
    }
}

@Composable
fun ShimmerRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        userScrollEnabled = false
    ) {
        items(5) {
            AnimatedShimmer()
        }
    }
}

@Composable
fun RealGameRow(
    deals: LazyPagingItems<GameCardItem>,
    favoriteIds: Set<String>,
    onToggleFavorite: (GameCardItem) -> Unit,
    storageKey: String,
    onCardClick: (String) -> Unit
) {
    val lazyListState = rememberSaveable(key = storageKey, saver = LazyListState.Saver) {
        LazyListState()
    }

    LazyRow(
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        items(
            count = deals.itemCount,
            key = deals.itemKey { it.id }
        ) { index ->
            val game = deals[index]
            if (game != null) {
                GameCard(
                    modifier = Modifier
                        .width(175.dp)
                        .fillMaxHeight(),
                    game = game,
                    isScrolling = lazyListState.isScrollInProgress,
                    isFavorite = game.id in favoriteIds,
                    onToggleFavorite = { onToggleFavorite(game) },
                    onItemClick = onCardClick
                )
            }
        }

        if (deals.loadState.append is LoadState.Loading) {
            item {
                AnimatedShimmer()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val sampleGames = listOf(
        GameCardItem(
            id = "1",
            listingIndex = 1,
            title = "The Witcher 3: Wild Hunt",
            thumbnail = null,
            store = "Steam",
            upVotes = UpVotes("95%", ColorCode.GREEN),
            price = Price(39990.99, 9999.99),
            genres = listOf("RPG", "Open World")
        ),
        GameCardItem(
            id = "2",
            listingIndex = 2,
            title = "Cyberpunk 2077",
            thumbnail = null,
            store = "GOG",
            upVotes = UpVotes("80%", ColorCode.YELLOW),
            price = Price(59.99, 29.99),
            genres = listOf("RPG", "Sci-fi")
        )
    )

    val hotDealsFlow = flowOf(PagingData.from(sampleGames))
    val hotDeals = hotDealsFlow.collectAsLazyPagingItems()

    MaterialTheme {
        Surface {
            HomeScreenContent(
                hotDeals = hotDeals,
                lowestPriceDeals = hotDeals,
                anticipatedGames = sampleGames, // Provide sample data for preview
                favoriteIds = setOf("1"),
                onToggleFavorite = {},
                onCardClick = {}
            )
        }
    }
}