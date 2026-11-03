package com.example.treasure.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Remote Data Transfer Object (DTO) representing a game deal returned from the backend service.
 *
 * Encapsulates both list feed summary attributes and detailed lazy enrichment attributes
 * retrieved from Steam, IsThereAnyDeal, and IGDB aggregations.
 *
 * @property id Unique identifier of the deal.
 * @property title Primary title of the game.
 * @property thumbnail Remote URL pointing to the game's box art image.
 * @property primaryStore Name of the storefront providing the primary deal.
 * @property originalPrice Baseline MSRP price before discount.
 * @property currentPrice Active sale price.
 * @property discountPercent Percentage discount applied.
 * @property upVotes Community review score string.
 * @property upVoteColor Semantic color token string for review scores.
 * @property expectedReleaseDate Unix epoch timestamp of expected release date.
 * @property hypeScore Community anticipation hype score metric.
 * @property description Narrative synopsis or plot summary.
 * @property developer Studio responsible for game development.
 * @property publisher Publishing organization.
 * @property franchise Intellectual property franchise series name.
 * @property releaseDate Human-readable release date string.
 * @property maturityRating Age rating advisory code.
 * @property trailerUrl Streaming video URL for trailers.
 * @property screenshots Gallery of high-resolution screenshot image URLs.
 * @property genres Category and genre classification tags.
 * @property platforms Supported operating system platforms.
 * @property systemRequirements Minimum and recommended PC hardware requirements.
 * @property otherStores Alternative storefront pricing offers.
 * @property categories Set of [DealCategory] partitions this game belongs to.
 * @property lastEnrichedAt ISO-8601 timestamp string recording when metadata was last refreshed on the backend.
 */
@Keep
data class GameDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("primaryStore") val primaryStore: String?,
    @SerializedName("originalPrice") val originalPrice: Double?,
    @SerializedName("currentPrice") val currentPrice: Double?,
    @SerializedName("discountPercent") val discountPercent: Int?,
    @SerializedName("upVotes") val upVotes: String?,
    @SerializedName("upVoteColor") val upVoteColor: String?,
    @SerializedName("expectedReleaseDate") val expectedReleaseDate: Long?,
    @SerializedName("hypeScore") val hypeScore: Int?,
    @SerializedName("description") val description: String?,
    @SerializedName("developer") val developer: String?,
    @SerializedName("publisher") val publisher: String?,
    @SerializedName("franchise") val franchise: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("maturityRating") val maturityRating: String?,
    @SerializedName("trailerUrl") val trailerUrl: String?,
    @SerializedName("screenshots") val screenshots: List<String>?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("platforms") val platforms: List<String>?,
    @SerializedName("systemRequirements") val systemRequirements: List<SystemRequirement>?,
    @SerializedName("otherStores") val otherStores: List<StoreDeal>?,
    @SerializedName("categories") val categories: Set<DealCategory> = emptySet(),
    @SerializedName("lastEnrichedAt") val lastEnrichedAt: String?
)

/**
 * Hardware specification requirements entry received from backend catalog metadata.
 *
 * @property specName Component specification descriptor (e.g., "OS", "Processor", "Memory").
 * @property specValue Hardware capability description string.
 * @property type Tier designation string (e.g., "Minimum", "Maximum").
 */
@Keep
data class SystemRequirement(
    @SerializedName("specName") val specName: String = "",
    @SerializedName("specValue") val specValue: String = "",
    @SerializedName("type") val type: String = ""
)

/**
 * Alternative storefront pricing offer for a game deal.
 *
 * @property storeName Display name of the storefront merchant.
 * @property dealUrl Outbound product or checkout URL.
 * @property originalPrice Baseline MSRP price.
 * @property currentPrice Active sale price.
 */
@Keep
data class StoreDeal(
    @SerializedName("storeName") val storeName: String = "",
    @SerializedName("dealUrl") val dealUrl: String = "",
    @SerializedName("originalPrice") val originalPrice: Double = 0.0,
    @SerializedName("currentPrice") val currentPrice: Double = 0.0
)

/**
 * Deal category classifications matching backend Spring Boot service partitions.
 *
 * @property categoryName Backend parameter name for query string mapping.
 */
@Keep
enum class DealCategory(val categoryName: String) {
    /**
     * Top trending and high-discount featured deals.
     */
    @SerializedName("HOT_DEALS") HOT_DEALS("HotDeals"),

    /**
     * Historic all-time lowest price deals.
     */
    @SerializedName("LOWEST_PRICE") LOWEST_PRICE("LowestPrice"),

    /**
     * Search result deal category.
     */
    @SerializedName("SEARCH") SEARCH("Search"),

    /**
     * Highly anticipated upcoming releases.
     */
    @SerializedName("MOST_ANTICIPATED") MOST_ANTICIPATED("MostAnticated"),

    /**
     * Deals compatible with Apple macOS.
     */
    @SerializedName("MAC_COMPATIBLE") MAC_COMPATIBLE("MacCompatible"),

    /**
     * Deals compatible with Linux / SteamOS.
     */
    @SerializedName("LINUX_COMPATIBLE") LINUX_COMPATIBLE("LinuxCompatible"),

    /**
     * Default unassigned category.
     */
    @SerializedName("NONE") NONE("None")
}
