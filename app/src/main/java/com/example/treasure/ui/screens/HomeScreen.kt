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

/**
 * Stateful Composable screen hosting the home feed, displaying categorized paged deal streams,
 * an anticipated game hero carousel, and reactive favorite indicators.
 *
 * @param modifier Layout modifier applied to the home container.
 * @param viewModel Injected [HomeScreenViewModel] providing deal streams and interaction actions.
 * @param onCardClick Invoked when an individual game card is tapped, passing the game ID.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    onCardClick: (String) -> Unit
) {
    val hotDeals = viewModel.hotDeals.collectAsLazyPagingItems()
    val lowestPriceDeals = viewModel.lowestPriceDeals.collectAsLazyPagingItems()
    val macDeals = viewModel.macDeals.collectAsLazyPagingItems()
    val linuxDeals = viewModel.linuxDeals.collectAsLazyPagingItems()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val anticipatedGames by viewModel.anticipatedGames.collectAsStateWithLifecycle()

    HomeScreenContent(
        modifier = modifier,
        hotDeals = hotDeals,
        lowestPriceDeals = lowestPriceDeals,
        macDeals = macDeals,
        linuxDeals = linuxDeals,
        anticipatedGames = anticipatedGames,
        favoriteIds = favoriteIds,
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onCardClick = onCardClick
    )
}

/**
 * Stateless content presentation for the Home screen layout.
 *
 * Combines the parallax hero carousel at the top with multiple horizontal scrolling deal sections.
 *
 * @param modifier Layout modifier applied to the parent container.
 * @param hotDeals Paged collection of trending hot deals.
 * @param lowestPriceDeals Paged collection of historic lowest price deals.
 * @param macDeals Paged collection of macOS-compatible deals.
 * @param linuxDeals Paged collection of Linux/SteamOS-compatible deals.
 * @param anticipatedGames List of featured anticipated games for the carousel.
 * @param favoriteIds Set of game IDs currently saved in the user's wishlist.
 * @param onToggleFavorite Callback invoked to toggle a game's wishlist status.
 * @param onCardClick Callback invoked when a game card is selected.
 */
@SuppressLint("FrequentlyChangingValue")
@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    hotDeals: LazyPagingItems<GameCardItem>,
    lowestPriceDeals: LazyPagingItems<GameCardItem>,
    macDeals: LazyPagingItems<GameCardItem>,
    linuxDeals: LazyPagingItems<GameCardItem>,
    anticipatedGames: List<GameCardItem>,
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
    val bottomPadding =
        80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val heroHeight = screenHeight * 0.82f

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = mainListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            item(key = "hero_carousel") {
                TreasureUpcomingCarousel(
                    games = anticipatedGames,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = onToggleFavorite,
                    onGameClick = onCardClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .graphicsLayer {
                            val scrollOffset = if (mainListState.firstVisibleItemIndex == 0) {
                                mainListState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                heroHeight.toPx()
                            }
                            translationY = scrollOffset * 0.5f
                            alpha =
                                1f - (scrollOffset / (heroHeight.toPx() * 0.8f)).coerceIn(0f, 1f)
                        }
                )
            }

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

            item(key = "header_mac") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "Mac Deals",
                        subtitle = "Best games compatible with macOS"
                    )
                    GameHorizontalList(
                        deals = macDeals,
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        storageKey = "list_mac",
                        onCardClick = onCardClick
                    )
                }
            }

            item(key = "header_linux") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "Linux Deals",
                        subtitle = "Native Linux compatible games"
                    )
                    GameHorizontalList(
                        deals = linuxDeals,
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        storageKey = "list_linux",
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

/**
 * Section header displaying category title and descriptive subtitle.
 *
 * @param title Primary category heading text.
 * @param subtitle Descriptive subtitle text.
 */
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

/**
 * Horizontal deal list orchestrator that toggles between skeleton shimmer placeholders and populated cards.
 *
 * @param deals Paged collection of games to display.
 * @param favoriteIds Set of game IDs marked as favorite.
 * @param onToggleFavorite Callback invoked when toggling wishlist status.
 * @param storageKey Unique key maintaining scroll position across configuration changes.
 * @param onCardClick Callback invoked when a game card is selected.
 */
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

/**
 * Skeleton placeholder row displaying animated shimmer placeholders during initial network pagination.
 */
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

/**
 * Populated horizontal deal reel rendering individual [GameCard] composables from paged items.
 *
 * @param deals Paged items stream.
 * @param favoriteIds Active set of favorited game IDs.
 * @param onToggleFavorite Callback invoked when toggling favorite state.
 * @param storageKey Key used to retain scroll position in [LazyListState].
 * @param onCardClick Invoked with the game ID when a card is clicked.
 */
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

/**
 * Preview Composable for [HomeScreenContent].
 */
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
                macDeals = hotDeals,
                linuxDeals = hotDeals,
                anticipatedGames = sampleGames,
                favoriteIds = setOf("1"),
                onToggleFavorite = {},
                onCardClick = {}
            )
        }
    }
}
