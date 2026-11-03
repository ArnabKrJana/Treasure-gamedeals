package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.treasure.domain.uiModels.PriceState

/**
 * Local Room entity representing discovery game titles sourced from IGDB (trending titles and new releases).
 *
 * Employs a discriminator column ([DiscoveryType]) to partition records between trending games and new releases,
 * and maintains asynchronous price enrichment fields resolved via external storefront APIs.
 *
 * @property igdbId Unique numeric game identifier from IGDB serving as the primary key.
 * @property title Primary name of the game.
 * @property coverUrl Remote URL of the game's box art cover image.
 * @property releaseDate Unix epoch timestamp (in milliseconds) used for chronological sorting.
 * @property totalRating Aggregate review rating score out of 100.
 * @property genres Curated category and genre classification tags.
 * @property priceCurrent Enriched current promotional price, or `null` if loading or unpriced.
 * @property priceOriginal Enriched baseline MSRP price.
 * @property discountPercent Enriched calculated discount percentage.
 * @property priceState The lifecycle resolution state ([PriceState]) of the title's pricing.
 * @property type Discriminator distinguishing between trending charts and new releases ([DiscoveryType]).
 * @property description Lazy-loaded narrative plot overview or synopsis.
 * @property screenshots Lazy-loaded gallery of screenshot image URLs.
 * @property trailerUrl Lazy-loaded video trailer streaming URL.
 * @property developer Studio responsible for game development.
 * @property publisher Publishing organization.
 * @property maturityRating Age rating classification identifier.
 * @property systemRequirements Minimum and recommended PC hardware requirements.
 */
@Entity(tableName = "discovery_games")
data class DiscoveryEntity(
    @PrimaryKey
    val igdbId: Int,
    val title: String,
    val coverUrl: String?,
    val releaseDate: Long?,
    val totalRating: Double?,
    val genres: List<String>,
    val priceCurrent: Double?,
    val priceOriginal: Double?,
    val discountPercent: Int?,
    val priceState: PriceState,
    val type: DiscoveryType,
    val description: String? = null,
    val screenshots: List<String>? = null,
    val trailerUrl: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val maturityRating: String? = null,
    val systemRequirements: List<SystemRequirementEntity>? = null
)

/**
 * Discriminator type identifying the catalog discovery category of a [DiscoveryEntity].
 */
enum class DiscoveryType {
    /**
     * High-velocity trending titles.
     */
    TRENDING,

    /**
     * Newly published video game releases.
     */
    NEW_RELEASE
}
