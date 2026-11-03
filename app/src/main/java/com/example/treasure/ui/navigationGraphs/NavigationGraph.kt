package com.example.treasure.ui.navigationGraphs

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destination endpoints for the root navigation graph.
 */
@Serializable
sealed interface RootGraphDestination {

    /**
     * Welcome and onboarding screen with video background and authentication prompt.
     */
    @Serializable
    data object WelcomeScreenRoute : RootGraphDestination

    /**
     * Primary application container hosting the bottom navigation bar and nested sub-graphs.
     */
    @Serializable
    data object MainScreenRoute : RootGraphDestination

    /**
     * Full-screen notification inbox displaying price alerts and discount updates.
     */
    @Serializable
    data object NotificationRoute : RootGraphDestination

    /**
     * Full-screen shopping cart and staging view.
     */
    @Serializable
    data object CartRoute : RootGraphDestination
}

/**
 * Type-safe navigation graph identifiers for the four main tabs in the bottom navigation bar.
 */
@Serializable
sealed interface NestedGraphDestination {

    /**
     * Sub-graph encompassing the Home feed and its child detail screens.
     */
    @Serializable
    data object HomeGraph : NestedGraphDestination

    /**
     * Sub-graph encompassing the Search interface and its child detail screens.
     */
    @Serializable
    data object SearchGraph : NestedGraphDestination

    /**
     * Sub-graph encompassing the Wishlist collection and its child detail screens.
     */
    @Serializable
    data object WishlistGraph : NestedGraphDestination

    /**
     * Sub-graph encompassing the application Settings and preference screens.
     */
    @Serializable
    data object SettingGraph : NestedGraphDestination
}

/**
 * Type-safe leaf destination routes hosted within the nested tab sub-graphs.
 */
@Serializable
sealed interface LeafDestination {

    /**
     * Primary catalog list screen inside the Home sub-graph.
     */
    @Serializable
    data object HomeList : LeafDestination

    /**
     * Search query input and results grid screen inside the Search sub-graph.
     */
    @Serializable
    data object SearchInput : LeafDestination

    /**
     * Saved favorites and wishlist grid screen inside the Wishlist sub-graph.
     */
    @Serializable
    data object WishlistList : LeafDestination

    /**
     * User preferences and configuration screen inside the Setting sub-graph.
     */
    @Serializable
    data object SettingScreen : LeafDestination

    /**
     * Comprehensive game detail view shared across Home, Search, and Wishlist graphs.
     *
     * @property itemId Unique identifier of the selected game deal.
     * @property fromTab Originating tab label (e.g., "Home", "Search", "Wishlist").
     */
    @Serializable
    data class Detail(val itemId: String, val fromTab: String) : LeafDestination
}
