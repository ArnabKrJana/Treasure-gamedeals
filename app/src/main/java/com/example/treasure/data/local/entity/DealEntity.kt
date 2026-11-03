package com.example.treasure.data.local.entity

import androidx.room.Entity
import com.example.treasure.domain.uiModels.StoreDeal

/**
 * Local Room database entity representing a cached game deal.
 *
 * Utilizes a composite primary key composed of `["id", "category"]` to allow the same game deal
 * to exist across multiple feed categories simultaneously (e.g., both HOT_DEALS and MAC_DEALS)
 * without key collision or cross-feed data corruption.
 *
 * Designed with a two-tier data model:
 * 1. Feed fields: Populated immediately during catalog ingestion via [com.example.treasure.data.local.remoteMediators.DealRemoteMediator].
 * 2. Enrichment fields: Default to `null` and are lazily populated when the user navigates into the detail screen.
 *
 * @property id Unique string identifier for the deal.
 * @property listingIndex Sequential display order index within the category feed.
 * @property title Primary title of the game.
 * @property thumbnail Remote URL pointing to the game's box art or cover image.
 * @property storeId Identifier of the storefront offering the deal.
 * @property originalPrice Baseline MSRP price before discount.
 * @property currentPrice Active sale price.
 * @property discountPercent Calculated percentage discount.
 * @property upVotes Community review sentiment score string.
 * @property upVoteColor Semantic color token string for review scores.
 * @property category The [DealCategory] partition this record belongs to.
 * @property description Lazy-loaded narrative plot overview or synopsis.
 * @property screenshots Lazy-loaded list of high-resolution screenshot image URLs.
 * @property trailerUrl Lazy-loaded video trailer streaming URL.
 * @property otherStores Lazy-loaded alternative storefront pricing offers.
 * @property genres Curated category and genre classification tags.
 * @property platforms Operating system compatibility tags ("windows", "mac", "linux").
 * @property systemRequirements Minimum and recommended PC hardware requirements.
 * @property developer Studio responsible for game development.
 * @property publisher Publishing organization.
 * @property franchise Intellectual property franchise series name.
 * @property releaseDate Formatted release date string.
 * @property maturityRating Age rating classification identifier.
 */
@Entity(
    tableName = "deals",
    primaryKeys = ["id", "category"]
)
data class DealEntity(
    val id: String,
    val listingIndex: Int,
    val title: String,
    val thumbnail: String?,
    val storeId: String,
    val originalPrice: Double,
    val currentPrice: Double,
    val discountPercent: Int,
    val upVotes: String?,
    val upVoteColor: String?,
    val category: DealCategory,
    val description: String? = null,
    val screenshots: List<String>? = null,
    val trailerUrl: String? = null,
    val otherStores: List<StoreDeal>? = null,
    val genres: List<String>? = null,
    val platforms: List<String>? = null,
    val systemRequirements: List<SystemRequirementEntity>? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val franchise: String? = null,
    val releaseDate: String? = null,
    val maturityRating: String? = null
)

/**
 * Category partitions for game deal aggregation feeds and pagination isolation.
 */
enum class DealCategory {
    /**
     * Top trending and high-discount featured deals.
     */
    HOT_DEALS,

    /**
     * Historic all-time lowest price deals.
     */
    LOWEST_PRICE,

    /**
     * Deals curated specifically for Apple macOS compatibility.
     */
    MAC_DEALS,

    /**
     * Deals curated specifically for Linux and SteamOS compatibility.
     */
    LINUX_DEALS,

    /**
     * Transient deal category assigned to search query results.
     */
    SEARCH
}
