package com.example.treasure.utils.navigation

import androidx.annotation.DrawableRes
import com.example.treasure.R
import com.example.treasure.ui.navigationGraphs.NestedGraphDestination

/**
 * Encapsulates the metadata and navigation destinations for the top-level persistent bottom navigation bar.
 *
 * Each destination holds its target [NestedGraphDestination] route, localized display title,
 * and distinct icon drawable resources representing selected and unselected interaction states.
 *
 * @property route The strongly-typed navigation destination route parsed by Navigation Compose.
 * @property title The localized text label displayed below the navigation item icon.
 * @property unselectedIcon Drawable resource ID displayed when the screen is inactive.
 * @property selectedIcon Drawable resource ID displayed when the screen is actively selected.
 * @property hasNews Optional indicator flag signifying pending updates or badge alerts for the screen.
 */
sealed class MainScreen(
    val route: NestedGraphDestination,
    val title: String,
    @get:DrawableRes val unselectedIcon: Int,
    @get:DrawableRes val selectedIcon: Int,
    val hasNews: Boolean? = false
) {
    /**
     * Home tab destination displaying hero deal carousels, trending games, and store discounts.
     */
    data object Home : MainScreen(
        route = NestedGraphDestination.HomeGraph,
        title = "Home",
        unselectedIcon = R.drawable.home_outlined,
        selectedIcon = R.drawable.home_filled,
    )

    /**
     * Search tab destination providing live catalog search, filters, and store query results.
     */
    data object Search : MainScreen(
        route = NestedGraphDestination.SearchGraph,
        title = "Search",
        unselectedIcon = R.drawable.search_outlined,
        selectedIcon = R.drawable.search_filled,
    )

    /**
     * Wishlist tab destination displaying saved game titles, price drop watches, and local notifications.
     */
    data object Wishlist : MainScreen(
        route = NestedGraphDestination.WishlistGraph,
        title = "Wishlist",
        unselectedIcon = R.drawable.wishlist_outlined,
        selectedIcon = R.drawable.wishlist_filled
    )

    /**
     * Settings tab destination exposing theme toggles, auth session details, and cloud drive backup options.
     */
    data object Setting : MainScreen(
        route = NestedGraphDestination.SettingGraph,
        title = "Setting",
        unselectedIcon = R.drawable.setting_outlined,
        selectedIcon = R.drawable.setting_filled,
    )
}
