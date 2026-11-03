package com.example.treasure.utils

/**
 * Enumeration of digital PC game distribution storefronts aggregated by the application.
 *
 * Distinguishes store-specific deals, deep-linking URL schemes, and licensing origins across the platform.
 */
enum class Stores {
    /**
     * Valve Steam digital storefront.
     */
    STEAM,

    /**
     * Epic Games Store digital storefront.
     */
    EPIC,

    /**
     * CD Projekt GOG (Good Old Games) DRM-free digital storefront.
     */
    GOG
}
