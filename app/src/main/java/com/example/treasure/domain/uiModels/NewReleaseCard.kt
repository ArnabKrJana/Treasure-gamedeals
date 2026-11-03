package com.example.treasure.domain.uiModels

/**
 * Presentation model representing newly released video game titles sourced from IGDB.
 *
 * Supports asynchronous pricing enrichment wherein price information may resolve after
 * the initial game card renders in the feed.
 *
 * @property igdbId Unique numeric game identifier from the Internet Game Database (IGDB).
 * @property title Primary title of the game.
 * @property coverUrl Remote URL of the official box art cover image.
 * @property releaseDate Human-readable release date string.
 * @property rating Aggregate critic/player rating score out of 100, or `null` if unrated.
 * @property genres Curated category and genre classification tags.
 * @property price Enriched [Price] information, or `null` if yet to be resolved or unavailable.
 * @property priceState The asynchronous loading state of the deal price ([PriceState]).
 */
data class NewReleaseCard(
    val igdbId: Int,
    val title: String,
    val coverUrl: String?,
    val releaseDate: String,
    val rating: Double?,
    val genres: List<String>,
    val price: Price?,
    val priceState: PriceState
)

/**
 * Lifecycle state of asynchronous price resolution for newly cataloged games.
 */
enum class PriceState {
    /**
     * Price enrichment network lookup is actively executing.
     */
    LOADING,

    /**
     * Active pricing has been successfully located and parsed.
     */
    AVAILABLE,

    /**
     * No active commercial deals were located across supported digital storefronts.
     */
    NOT_FOUND
}
