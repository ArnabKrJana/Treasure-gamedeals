package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.treasure.domain.uiModels.StoreDeal
import kotlinx.serialization.Serializable


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

    // --- ENRICHMENT DATA (From Steam /appdetails) ---
    // These start as null. You fill them when the user opens the detail screen.
    val description: String? = null,
    val screenshots: List<String>? = null,
    val trailerUrl: String? = null,
    val otherStores: List<StoreDeal>? = null,

    // --- MISSING FIELDS ADDED HERE ---
    val genres: List<String>? = null, // Steam gives specific genres
    val platforms: List<String>? = null, // "windows", "mac", "linux"
    val systemRequirements: List<SystemRequirementEntity>? = null, // Min/Max specs

    // --- IMPORTANT DETAILS ---
    val developer: String? = null,
    val publisher: String? = null,
    val franchise: String? = null,
    val releaseDate: String? = null,
    val maturityRating: String? = null
)



enum class DealCategory {
    HOT_DEALS,
    LOWEST_PRICE,
    SEARCH
}