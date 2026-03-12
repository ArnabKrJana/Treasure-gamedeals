package com.example.treasure.domain.uiModels

import com.example.treasure.utils.Stores

data class TrendingGameCard(
    val igdbId: Int,                 // IGDB game id
    val title: String,               // IGDB name
    val coverUrl: String?,            // IGDB cover
    val rating: Double?,              // IGDB total_rating
    val genres: List<String>,         // IGDB genres

    // ---- Price enrichment (optional, delayed) ----
    val price: Price?,                // ITAD current price
    val store: Stores?,                // Steam / Epic / GOG
    val priceState: PriceState        // UI truth
)






