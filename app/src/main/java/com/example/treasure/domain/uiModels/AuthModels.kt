package com.example.treasure.domain.uiModels

import kotlinx.serialization.Serializable

/**
 * Domain representation of an authenticated user identity within the application.
 *
 * Implements [Serializable] via Kotlinx Serialization to permit persistence within [android.content.SharedPreferences]
 * by [com.example.treasure.utils.TokenManager].
 *
 * @property id Unique numeric user identifier assigned by the backend persistence layer.
 * @property email Primary registered email address associated with the user account.
 * @property fullName Optional display name returned from the OAuth provider profile.
 * @property profilePicture Optional public image URL linking to the user's avatar.
 * @property role Authorization security role assigned to the user (e.g., "ROLE_USER", "ROLE_ADMIN").
 */
@Serializable
data class User(
    val id: Long,
    val email: String,
    val fullName: String?,
    val profilePicture: String?,
    val role: String
)
