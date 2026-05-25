package com.example.treasure.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.treasure.R
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.ui.navigationGraphs.LeafDestination
import com.example.treasure.ui.navigationGraphs.NestedGraphDestination
import com.example.treasure.ui.navigationGraphs.RootGraphDestination
import com.example.treasure.ui.uiComponents.BottomNavBar
import com.example.treasure.ui.uiComponents.TopAppBarComponent
import com.example.treasure.ui.viewModels.NotificationViewModel
import com.example.treasure.utils.ColorCode
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.treasure.data.local.entity.enums.Role
import com.example.treasure.domain.uiModels.User
import kotlinx.coroutines.flow.flowOf
import com.example.treasure.ui.Transitions
import com.example.treasure.ui.Transitions.slideInFromLeft
import com.example.treasure.ui.Transitions.slideInFromRight
import com.example.treasure.ui.Transitions.slideOutToLeft
import com.example.treasure.ui.Transitions.slideOutToRight
import com.example.treasure.ui.viewModels.AuthViewModel
import com.example.treasure.ui.viewModels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenComposable(
    rootNavController: NavHostController,
    mainViewModel: MainViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    // 1. Observe real user data
    val currentUser by mainViewModel.currentUser.collectAsStateWithLifecycle()

    val bottomNavController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val notificationCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()

    // --- IDENTIFY ACTIVE SCREENS ---
    val isHomeScreen = currentDestination?.hasRoute<LeafDestination.HomeList>() == true
    val isSearchScreen = currentDestination?.hasRoute<LeafDestination.SearchInput>() == true
    val isDetailScreen = currentDestination?.hasRoute<LeafDestination.Detail>() == true
    val isSettingScreen = currentDestination?.hasRoute<LeafDestination.SettingScreen>() == true

    val topBarTitle = if (isDetailScreen) {
        stringResource(R.string.details_title)
    } else {
        when {
            isHomeScreen -> "Treasure"
            isSearchScreen -> "Search"
            currentDestination?.hasRoute<LeafDestination.WishlistList>() == true -> "Wishlist"
            isSettingScreen -> "Settings"
            else -> "Treasure"
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            if (!isSearchScreen) {
                TopAppBarComponent(
                    title = topBarTitle,
                    canNavigateBack = isDetailScreen,
                    isSettingScreen = isSettingScreen,
                    notificationCount = notificationCount,
                    scrollBehavior = scrollBehavior,
                    onBackBtnClick = { bottomNavController.popBackStack() },
                    onNotificationBtnClick = {
                        rootNavController.navigate(RootGraphDestination.NotificationRoute)
                    },
                    // 2. Wire the real data and auth triggers
                    currentUser = currentUser,
                    onLogoutClick = { authViewModel.logout() },
                    onDeleteAccountClick = { authViewModel.deleteMyAccount() }
                )
            }
        },
        bottomBar = {
            if (!isDetailScreen) {
                BottomNavBar(bottomNavController)
            }
        }
    ) { innerPadding ->
        val topPadding = if (isHomeScreen || isSearchScreen || isDetailScreen) {
            0.dp
        } else {
            innerPadding.calculateTopPadding()
        }

        NavHost(
            navController = bottomNavController,
            startDestination = NestedGraphDestination.HomeGraph,
            modifier = Modifier.padding(
                top = topPadding,
                bottom = innerPadding.calculateBottomPadding()
            ),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(220)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(220)) }
        ) {

            // --- HOME TAB ---
            navigation<NestedGraphDestination.HomeGraph>(startDestination = LeafDestination.HomeList) {
                composable<LeafDestination.HomeList>(
                    enterTransition = { fadeIn(tween(500)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    HomeScreen(
                        onCardClick = { gameId ->
                            bottomNavController.navigate(
                                LeafDestination.Detail(itemId = gameId, fromTab = "Home")
                            )
                        }
                    )
                }

                composable<LeafDestination.Detail>(
                    enterTransition = { slideInFromRight() },
                    exitTransition = { slideOutToLeft() },
                    popEnterTransition = { slideInFromLeft() },
                    popExitTransition = { slideOutToRight() }
                ) {
                    DetailScreen()
                }
            }

            // --- SEARCH TAB ---
            navigation<NestedGraphDestination.SearchGraph>(startDestination = LeafDestination.SearchInput) {
                composable<LeafDestination.SearchInput>(
                    enterTransition = { fadeIn(tween(250)) },
                    exitTransition = { fadeOut(tween(250)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { fadeOut(tween(250)) }
                ) {
                    SearchScreen(
                        onItemClick = { gameId ->
                            bottomNavController.navigate(
                                LeafDestination.Detail(itemId = gameId, fromTab = "Search")
                            )
                        },
                        onBackClick = {
                            bottomNavController.navigate(LeafDestination.HomeList) {
                                popUpTo(LeafDestination.HomeList) { inclusive = true }
                            }
                        }
                    )
                }

                composable<LeafDestination.Detail>(
                    enterTransition = { slideInFromRight() },
                    exitTransition = { slideOutToLeft() },
                    popEnterTransition = { slideInFromLeft() },
                    popExitTransition = { slideOutToRight() }
                ) {
                    DetailScreen()
                }
            }

            // --- WISHLIST TAB ---
            navigation<NestedGraphDestination.WishlistGraph>(startDestination = LeafDestination.WishlistList) {
                composable<LeafDestination.WishlistList>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "treasure://wishlist" }
                    ),
                    enterTransition = { fadeIn(tween(500)) },
                    exitTransition = { fadeOut(tween(500)) },
                ) {
                    WishlistScreen { gameId ->
                        bottomNavController.navigate(
                            LeafDestination.Detail(itemId = gameId, fromTab = "Wishlist")
                        )
                    }
                }

                composable<LeafDestination.Detail>(
                    enterTransition = { slideInFromRight() },
                    exitTransition = { slideOutToLeft() },
                    popEnterTransition = { slideInFromLeft() },
                    popExitTransition = { slideOutToRight() }
                ) {
                    DetailScreen()
                }
            }

            // --- SETTING TAB ---
            navigation<NestedGraphDestination.SettingGraph>(startDestination = LeafDestination.SettingScreen) {
                composable<LeafDestination.SettingScreen>(
                    enterTransition = { fadeIn(tween(250)) },
                    exitTransition = { fadeOut(tween(250)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { fadeOut(tween(250)) }
                ) {
                    SettingScreen()
                }
            }
        }
    }
}

@Preview(name = "Main - Home View")
@Composable
fun MainScreenHomePreview() {
    val sampleGames = listOf(
        GameCardItem(
            "1",
            1,
            "The Witcher 3",
            null,
            "Steam",
            UpVotes("95%", ColorCode.GREEN),
            Price(39.99, 9.99)
        )
    )
    val hotDeals = flowOf(PagingData.from(sampleGames)).collectAsLazyPagingItems()

    MaterialTheme {
        Surface {
            HomeScreenContent(
                hotDeals = hotDeals,
                lowestPriceDeals = hotDeals,
                favoriteIds = emptySet(),
                onToggleFavorite = {},
                onCardClick = {},
                modifier = Modifier,
                anticipatedGames = emptyList()
            )
        }
    }
}

@Preview(name = "Main - Detail View")
@Composable
fun MainScreenDetailPreview() {
    MaterialTheme {
        Surface {
            DetailScreenContent(
                gameDetail = DealEntity(
                    id = "1",
                    listingIndex = 0,
                    title = "Senua's Saga: Hellblade II",
                    thumbnail = null,
                    storeId = "steam",
                    originalPrice = 59.99,
                    currentPrice = 29.99,
                    discountPercent = 50,
                    upVotes = "Very Positive (92%)",
                    upVoteColor = "green",
                    category = DealCategory.HOT_DEALS,
                    description = "The sequel to the award-winning Hellblade: Senua's Sacrifice...",
                    genres = listOf("Action", "Adventure"),
                    developer = "Ninja Theory",
                    publisher = "Xbox Game Studios",
                    franchise = "Ninja Theory, Hellblade Franchise",
                    releaseDate = "21 May, 2024",
                    maturityRating = "Mature 17+",
                    screenshots = emptyList(),
                    trailerUrl = null
                )
            )
        }
    }
}

@Preview(name = "Main - Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenDarkPreview() {
    MainScreenHomePreview()
}