package com.example.treasure.domain.uiModels

import com.example.treasure.utils.ColorCode

/**
 * Presentation model representing a game card displayed in catalog feeds, carousels, and search listings.
 *
 * Encapsulates essential game metadata, store attribution, review sentiment, pricing, and category tags.
 *
 * @property id Unique string identifier for the deal or catalog item.
 * @property listingIndex Sequential display ordering index within paginated or curated feeds.
 * @property title Primary title of the game.
 * @property thumbnail Optional remote web URL for the game's box art or cover image.
 * @property store Name of the digital storefront offering the deal (e.g., Steam, Epic, GOG).
 * @property upVotes User review sentiment and count, or `null` if unrated.
 * @property price Current and original pricing structure, or `null` if unpriced.
 * @property genres Curated category and genre classification tags.
 * @property releaseDate Unix epoch timestamp (in milliseconds) of the game's release date, or `null` if unannounced.
 */
data class GameCardItem(
    val id: String,
    val listingIndex: Int,
    val title: String,
    val thumbnail: String?,
    val store: String,
    val upVotes: UpVotes?,
    val price: Price?,
    val genres: List<String> = emptyList(),
    val releaseDate: Long? = null
)

/**
 * Encapsulates community review scores and sentiment color indicators.
 *
 * @property votes Formatted vote count or sentiment string (e.g., "95%", "Very Positive").
 * @property colorCode Visual semantic color classification ([ColorCode]) mapped to UI indicator tokens.
 */
data class UpVotes(
    val votes: String?,
    val colorCode: ColorCode
)

/**
 * Monetary representation encapsulating current deal price, baseline MSRP, and discount computation.
 *
 * @property originalPrice The baseline manufacturer suggested retail price (MSRP) prior to discount.
 * @property currentPrice The active discounted promotional sale price.
 */
data class Price(
    val originalPrice: Double,
    val currentPrice: Double
) {
    /**
     * Calculates the integer percentage discount savings relative to [originalPrice].
     *
     * Returns `0` if [originalPrice] is non-positive or if no discount is present.
     */
    val discountPercent: Int
        get() {
            if (originalPrice <= 0.0) return 0
            val percent = ((originalPrice - currentPrice) / originalPrice) * 100
            return percent.toInt()
        }
}
