package com.example.treasure.utils

/**
 * Visual semantic color classification used across the user interface to visually communicate
 * ratings, discount tiers, price trends, or status severities.
 *
 * Mapped to concrete Compose [androidx.compose.ui.graphics.Color] tokens in UI rendering components.
 */
enum class ColorCode {
    /**
     * Represents optimal or positive metrics (e.g., steep discounts, exceptional review scores, price drops).
     */
    GREEN,

    /**
     * Represents moderate or neutral metrics (e.g., average discounts, mixed reviews, stable prices).
     */
    YELLOW,

    /**
     * Represents unfavorable or cautionary metrics (e.g., minimal discounts, negative reviews, price hikes).
     */
    RED
}
