package com.example.treasure.utils

import androidx.compose.ui.graphics.Color

/**
 * Standardized video game age rating and content advisory classification.
 *
 * Normalizes ratings across multiple international rating boards—predominantly North America's
 * Entertainment Software Rating Board (ESRB) and Europe's Pan-European Game Information (PEGI).
 * Each rating exposes human-readable labels, minimum age guidelines, and visual badge colors.
 *
 * @property id Normalized identifier token matched against remote storefront response payloads.
 * @property displayName User-facing localized title of the content classification.
 * @property ageLimit Minimum recommended age threshold for player eligibility.
 * @property color Compose [Color] token assigned to render maturity warning chips and badges.
 */
enum class Maturity(
    val id: String,
    val displayName: String,
    val ageLimit: Int,
    val color: Color = Color.Gray
) {
    /**
     * ESRB: Content is generally suitable for all ages.
     */
    ESRB_E("e", "Everyone", 0, Color(0xFF4CAF50)),

    /**
     * ESRB: Content is generally suitable for ages 10 and up.
     */
    ESRB_E10("e10", "Everyone 10+", 10, Color(0xFF8BC34A)),

    /**
     * ESRB: Content is generally suitable for ages 13 and up.
     */
    ESRB_T("t", "Teen", 13, Color(0xFFFFC107)),

    /**
     * ESRB: Content is generally suitable for ages 17 and up. May contain intense violence or blood.
     */
    ESRB_M("m", "Mature 17+", 17, Color(0xFFFF5722)),

    /**
     * ESRB: Content suitable only for adults ages 18 and up.
     */
    ESRB_AO("ao", "Adults Only", 18, Color(0xFFD32F2F)),

    /**
     * PEGI: Suitable for all age groups.
     */
    PEGI_3("3", "PEGI 3", 3, Color(0xFF4CAF50)),

    /**
     * PEGI: Suitable for young children ages 7 and older.
     */
    PEGI_7("7", "PEGI 7", 7, Color(0xFF8BC34A)),

    /**
     * PEGI: Suitable for players ages 12 and older.
     */
    PEGI_12("12", "PEGI 12", 12, Color(0xFFFFC107)),

    /**
     * PEGI: Suitable for players ages 16 and older.
     */
    PEGI_16("16", "PEGI 16", 16, Color(0xFFFF9800)),

    /**
     * PEGI: Content restricted to adult players ages 18 and older.
     */
    PEGI_18("18", "PEGI 18", 18, Color(0xFFD32F2F)),

    /**
     * Fallback classification for unrated titles or unrecognizable rating identifiers.
     */
    UNKNOWN("", "Not Rated", 0, Color.LightGray);

    companion object {
        /**
         * Parses and maps a raw rating string retrieved from the Steam Web API into a typed [Maturity] instance.
         *
         * Performs whitespace trimming, case normalization, and cross-board mapping (e.g., Australian
         * classification "r18" is mapped to the equivalent [PEGI_18]).
         *
         * @param ratingCode Raw string identifier received from the remote API payload (e.g., "m", "18", "r18").
         * @return Corresponding [Maturity] enum constant, or [UNKNOWN] if null, empty, or unrecognized.
         */
        fun fromSteamRating(ratingCode: String?): Maturity {
            if (ratingCode.isNullOrEmpty()) return UNKNOWN

            val normalized = ratingCode.lowercase().trim()

            return entries.find { it.id == normalized } ?: when (normalized) {
                "r18" -> PEGI_18
                else -> UNKNOWN
            }
        }
    }
}
