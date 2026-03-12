package com.example.treasure.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.treasure.domain.uiModels.PriceState

@Entity(tableName = "discovery_games")
data class DiscoveryEntity(
    // --- LIST DATA (Displayed immediately in RecyclerView) ---
    @PrimaryKey
    val igdbId: Int,                // (TrendingGameCard.igdbId)
    val title: String,              //
    val coverUrl: String?,          //
    val releaseDate: Long?,         // Stored as Timestamp for sorting (New Releases)
    val totalRating: Double?,       //
    val genres: List<String>,       // (Needs TypeConverter)

    // --- PRICE ENRICHMENT (From ITAD) ---
    val priceCurrent: Double?,      //
    val priceOriginal: Double?,
    val discountPercent: Int?,
    val priceState: PriceState,     // (LOADING, AVAILABLE, NOT_FOUND)

    // --- DISCRIMINATOR (To separate queries) ---
    val type: DiscoveryType,        // TRENDING or NEW_RELEASE

    // --- DETAIL DATA (Loaded lazily when user clicks) ---
    val description: String? = null,           // (GameDetailUiModel)
    val screenshots: List<String>? = null,     //
    val trailerUrl: String? = null,            //
    val developer: String? = null,             //
    val publisher: String? = null,             //
    val maturityRating: String? = null,        // (Maturity enum stored as String)
    val systemRequirements: List<SystemRequirementEntity>? = null //
)

enum class DiscoveryType { TRENDING, NEW_RELEASE }