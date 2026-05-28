package com.example.treasure.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

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

@Keep
data class SystemRequirement(
    @SerializedName("specName") val specName: String = "",
    @SerializedName("specValue") val specValue: String = "",
    @SerializedName("type") val type: String = ""
)

@Keep
data class StoreDeal(
    @SerializedName("storeName") val storeName: String = "",
    @SerializedName("dealUrl") val dealUrl: String = "",
    @SerializedName("originalPrice") val originalPrice: Double = 0.0,
    @SerializedName("currentPrice") val currentPrice: Double = 0.0
)

@Keep
enum class DealCategory(val categoryName: String) {
    @SerializedName("HOT_DEALS") HOT_DEALS("HotDeals"),
    @SerializedName("LOWEST_PRICE") LOWEST_PRICE("LowestPrice"),
    @SerializedName("SEARCH") SEARCH("Search"),
    @SerializedName("MOST_ANTICIPATED") MOST_ANTICIPATED("MostAnticated"),
    @SerializedName("MAC_COMPATIBLE") MAC_COMPATIBLE("MacCompatible"),
    @SerializedName("LINUX_COMPATIBLE") LINUX_COMPATIBLE("LinuxCompatible"),
    @SerializedName("NONE") NONE("None")
}






//package com.example.treasure.data.remote.dto
//
//import androidx.annotation.Keep
//import com.example.treasure.domain.uiModels.Price
//import com.google.gson.annotations.SerializedName
//
//@Keep
//data class GameDto(
//    @SerializedName("") val  id: String,
//    @SerializedName("") val  title: String,
//    @SerializedName("") val  thumbnail: String?,
//    @SerializedName("") val  primaryStore: String?,
//    @SerializedName("") val  originalPrice: Double?,
//    @SerializedName("") val  currentPrice: Double?,
//    @SerializedName("") val  discountPercent: Int?,
//    @SerializedName("") val  upVotes: String?,
//    @SerializedName("") val  upVoteColor: String?,
//    @SerializedName("") val  expectedReleaseDate: Long?,
//    @SerializedName("") val  hypeScore: Int?,
//    @SerializedName("") val  description: String?,
//    @SerializedName("") val  developer: String?,
//    @SerializedName("") val  publisher: String?,
//    @SerializedName("") val  franchise: String?,
//    @SerializedName("") val  releaseDate: String?,
//    @SerializedName("") val  maturityRating: String?,
//    @SerializedName("") val  trailerUrl: String?,
//    @SerializedName("") val  screenshots: List<String>?,
//    @SerializedName("") val  genres: List<String>?,
//    @SerializedName("") val  platforms: List<String>?,
//    @SerializedName("") val  systemRequirements: List<SystemRequirement>?,
//    @SerializedName("") val  otherStores: List<StoreDeal>?,
//    @SerializedName("") val  categories: Set<DealCategory> = emptySet(),
//    @SerializedName("") val  lastEnrichedAt: String? // Mapped from backend Instant (ISO-8601 String)
//
//)
//
//data class SystemRequirement(
//    @SerializedName("") val  specName: String = "",
//    @SerializedName("") val  specValue: String = "",
//    @SerializedName("") val  type: String = ""
//)
//
//data class StoreDeal(
//    @SerializedName("") val  storeName: String = "",
//    @SerializedName("") val  dealUrl: String = "",
//    @SerializedName("") val  originalPrice: Double = 0.0,
//    @SerializedName("") val  currentPrice: Double = 0.0
//)
//
////data class StoreDeal(
////    val storeName: String,
////    val price: Price,
////    val dealUrl: String?
////)
//
//
//enum class DealCategory(val categoryName: String) {
//    HOT_DEALS("HotDeals"),
//    LOWEST_PRICE("LowestPrice"),
//    SEARCH("Search"),
//    MOST_ANTICIPATED("MostAnticated"),
//    MAC_COMPATIBLE("MacCompatible"),
//    LINUX_COMPATIBLE("LinuxCompatible"),
//    NONE("None")
//}