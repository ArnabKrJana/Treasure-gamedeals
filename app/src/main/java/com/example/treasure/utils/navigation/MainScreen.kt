package com.example.treasure.utils.navigation

import androidx.annotation.DrawableRes
import com.example.treasure.R
import com.example.treasure.ui.navigationGraphs.NestedGraphDestination

sealed class MainScreen(
    val route: NestedGraphDestination,
    val title: String,
    @get:DrawableRes val unselectedIcon: Int,
    @get:DrawableRes val selectedIcon: Int,
    val hasNews: Boolean? = false
) {
    data object Home : MainScreen(
        route = NestedGraphDestination.HomeGraph, // Passing the Object
        title = "Home",
        unselectedIcon = R.drawable.home_outlined,
        selectedIcon = R.drawable.home_filled,
    )
    data object Search : MainScreen(
        route = NestedGraphDestination.SearchGraph,
        title = "Search",
        unselectedIcon = R.drawable.search_outlined,
        selectedIcon = R.drawable.search_filled,
    )
    data object Wishlist : MainScreen(
        route = NestedGraphDestination.WishlistGraph,
        title = "Wishlist",
        unselectedIcon = R.drawable.wishlist_outlined,
        selectedIcon = R.drawable.wishlist_filled
    )
    data object Setting : MainScreen(
        route = NestedGraphDestination.SettingGraph,
        title = "Setting",
        unselectedIcon = R.drawable.setting_outlined,
        selectedIcon = R.drawable.setting_filled,
    )
}