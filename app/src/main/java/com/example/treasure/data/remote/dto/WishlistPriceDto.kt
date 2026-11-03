package com.example.treasure.data.remote.dto

import androidx.annotation.Keep

/**
 * Data Transfer Object capturing minimal price snapshot values for a wishlisted game title.
 *
 * Used by background synchronization workers during periodic price polling to detect
 * price drops or increases without requesting heavy full-game metadata payloads.
 *
 * @property current Active sale price for the title.
 * @property original Baseline MSRP price before discount.
 */
@Keep
data class WishlistPriceDto(
    val current: Double,
    val original: Double
)
