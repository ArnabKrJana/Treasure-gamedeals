package com.example.treasure.domain.uiModels

import com.example.treasure.utils.ColorCode

data class GameCardItem(
    val id: String,
    val listingIndex: Int,
    val title: String,
    val thumbnail: String?,
    val store: String,
    val upVotes: UpVotes?,
    val price: Price?,
    val genres: List<String> = emptyList(),
    val releaseDate: Long? =null
)

data class UpVotes(
    val votes: String?,
    val colorCode: ColorCode
)

data class Price(
    val originalPrice: Double,
    val currentPrice: Double
) {

    val discountPercent: Int
        get() {
            if (originalPrice <= 0.0) return 0
            val percent = ((originalPrice - currentPrice) / originalPrice) * 100
            return percent.toInt()
        }
}
