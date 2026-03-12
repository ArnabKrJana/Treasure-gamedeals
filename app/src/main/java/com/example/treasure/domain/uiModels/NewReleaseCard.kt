package com.example.treasure.domain.uiModels

data class NewReleaseCard(
    val igdbId: Int,
    val title: String,
    val coverUrl: String?,
    val releaseDate: String,
    val rating: Double?,
    val genres: List<String>,
    val price: Price?,          // nullable
    val priceState: PriceState
)

enum class PriceState {
    LOADING,
    AVAILABLE,
    NOT_FOUND
}

