package com.example.treasure.domain.uiModels

import com.example.treasure.utils.Stores

/**
 * Presentation model representing high-velocity trending video game titles.
 *
 * Sourced initially from IGDB trending charts and enriched asynchronously with live
 * storefront pricing from IsThereAnyDeal (ITAD).
 *
 * @property igdbId Unique numeric game identifier from the Internet Game Database (IGDB).
 * @property title Primary name of the game title.
 * @property coverUrl Remote URL pointing to the game's box art cover image.
 * @property rating Composite rating score out of 100, or `null` if unrated.
 * @property genres Curated category and genre classification tags.
 * @property price Enriched [Price] structure, or `null` if currently loading or unavailable.
 * @property store Storefront offering the best current deal ([Stores]), or `null` if unpriced.
 * @property priceState The asynchronous resolution lifecycle state ([PriceState]) driving UI loading placeholders.
 */
data class TrendingGameCard(
    val igdbId: Int,
    val title: String,
    val coverUrl: String?,
    val rating: Double?,
    val genres: List<String>,
    val price: Price?,
    val store: Stores?,
    val priceState: PriceState
)
