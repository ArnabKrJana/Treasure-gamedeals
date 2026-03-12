package com.example.treasure.utils

import androidx.compose.ui.graphics.Color

enum class Maturity(
    val id: String,
    val displayName: String,
    val ageLimit: Int,
    val color: Color = Color.Gray
) {
    // --- ESRB (North America) ---
    ESRB_E("e", "Everyone", 0, Color(0xFF4CAF50)),
    ESRB_E10("e10", "Everyone 10+", 10, Color(0xFF8BC34A)),
    ESRB_T("t", "Teen", 13, Color(0xFFFFC107)),
    ESRB_M("m", "Mature 17+", 17, Color(0xFFFF5722)),
    ESRB_AO("ao", "Adults Only", 18, Color(0xFFD32F2F)),

    // --- PEGI (Europe) ---
    PEGI_3("3", "PEGI 3", 3, Color(0xFF4CAF50)),
    PEGI_7("7", "PEGI 7", 7, Color(0xFF8BC34A)),
    PEGI_12("12", "PEGI 12", 12, Color(0xFFFFC107)),
    PEGI_16("16", "PEGI 16", 16, Color(0xFFFF9800)),
    PEGI_18("18", "PEGI 18", 18, Color(0xFFD32F2F)),

    // --- Generic / Fallback ---
    UNKNOWN("", "Not Rated", 0, Color.LightGray);

    companion object {
        fun fromSteamRating(ratingCode: String?): Maturity {
            if (ratingCode.isNullOrEmpty()) return UNKNOWN

            // Normalize string (Steam sometimes sends "m" or "M")
            val normalized = ratingCode.lowercase().trim()

            return entries.find { it.id == normalized } ?: when (normalized) {
                "r18" -> PEGI_18 // Map Australian R18 to PEGI 18 equivalent
                else -> UNKNOWN
            }
        }

    }


}