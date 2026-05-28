package com.example.treasure.data.remote.dto

import com.example.treasure.domain.uiModels.Price


data class GameDto(
    val id: String,
    val title: String,
    val thumbnail: String?,
    val primaryStore: String?,
    val originalPrice: Double?,
    val currentPrice: Double?,
    val discountPercent: Int?,
    val upVotes: String?,
    val upVoteColor: String?,
    val expectedReleaseDate: Long?,
    val hypeScore: Int?,
    val description: String?,
    val developer: String?,
    val publisher: String?,
    val franchise: String?,
    val releaseDate: String?,
    val maturityRating: String?,
    val trailerUrl: String?,
    val screenshots: List<String>?,
    val genres: List<String>?,
    val platforms: List<String>?,
    val systemRequirements: List<SystemRequirement>?,
    val otherStores: List<StoreDeal>?,
    val categories: Set<DealCategory> = emptySet(),
    val lastEnrichedAt: String? // Mapped from backend Instant (ISO-8601 String)

)

data class SystemRequirement(
    val specName: String = "",
    val specValue: String = "",
    val type: String = ""
)

data class StoreDeal(
    val storeName: String = "",
    val dealUrl: String = "",
    val originalPrice: Double = 0.0,
    val currentPrice: Double = 0.0
)

//data class StoreDeal(
//    val storeName: String,
//    val price: Price,
//    val dealUrl: String?
//)


enum class DealCategory(val categoryName: String) {
    HOT_DEALS("HotDeals"),
    LOWEST_PRICE("LowestPrice"),
    SEARCH("Search"),
    MOST_ANTICIPATED("MostAnticated"),
    MAC_COMPATIBLE("MacCompatible"),
    LINUX_COMPATIBLE("LinuxCompatible"),
    NONE("None")
}