package com.example.treasure.utils

/**
 * Application-wide compile-time configuration constants.
 *
 * Encapsulates static infrastructure metadata, remote backend endpoints,
 * third-party game store API URLs, and authentication client credentials.
 */
data object Constants {
    /**
     * File name for the local SQLite Room database backing application state and offline caching.
     */
    const val DATABASE_NAME = "treasure-database"

    /**
     * Base URL for the Spring Boot Backend-for-Frontend (BFF) service running on Render.
     * Serves deal aggregations, user authentication, wishlist synchronization, and Drive sync pipelines.
     */
    const val BACKEND_URL = "https://treasure-backend-i2qf.onrender.com/"

    /**
     * Base URL for the IsThereAnyDeal (ITAD) REST API used for game price history and cross-store data.
     */
    const val ITAD_URL = "https://api.isthereanydeal.com/"

    /**
     * Base URL for the official Valve Steam Web API providing game details and storefront metadata.
     */
    const val STEAM_URL = "https://store.steampowered.com/api/"

    /**
     * Google OAuth 2.0 Web Client ID registered in Google Cloud Console, utilized by Credential Manager /
     * Google Sign-In to exchange authorization tokens with the backend service.
     */
    const val WEB_CLIENT_ID = "963722074094-ta0g8028mt9q1negvl7nhhr6r45jfsc4.apps.googleusercontent.com"
}
