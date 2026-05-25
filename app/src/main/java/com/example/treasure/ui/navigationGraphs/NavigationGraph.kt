package com.example.treasure.ui.navigationGraphs

import kotlinx.serialization.Serializable

@Serializable
sealed interface RootGraphDestination{
    @Serializable data object WelcomeScreenRoute: RootGraphDestination
    @Serializable data object MainScreenRoute: RootGraphDestination
    @Serializable data object NotificationRoute: RootGraphDestination
    @Serializable data object CartRoute: RootGraphDestination
}

@Serializable
sealed interface NestedGraphDestination{
    @Serializable data object HomeGraph: NestedGraphDestination
    @Serializable data object SearchGraph: NestedGraphDestination
    @Serializable data object WishlistGraph: NestedGraphDestination
    @Serializable data object SettingGraph: NestedGraphDestination

}

@Serializable
sealed interface LeafDestination {

    // Screens inside Home Graph
    @Serializable data object HomeList : LeafDestination

    // Screens inside Search Graph
    @Serializable data object SearchInput : LeafDestination

    // Screens inside Wishlist Graph
    @Serializable data object WishlistList : LeafDestination

    // Screens inside Setting Graph
    @Serializable data object SettingScreen : LeafDestination

    // Shared Detail Screen (Used by Home, Search, Wishlist)
    @Serializable
    data class Detail(val itemId: String, val fromTab: String) : LeafDestination
}

