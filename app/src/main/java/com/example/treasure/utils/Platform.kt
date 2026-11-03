package com.example.treasure.utils

/**
 * Enumeration of supported operating system platforms for video game compatibility filtering.
 *
 * Used by catalog filters, search criteria, and game detail system requirement views to distinguish
 * executable environment requirements.
 */
enum class Platform {
    /**
     * Microsoft Windows platform compatibility.
     */
    WINDOWS,

    /**
     * Linux and SteamOS platform compatibility.
     */
    LINUX,

    /**
     * Apple macOS platform compatibility.
     */
    MAC
}
