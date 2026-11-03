package com.example.treasure.domain.repository

import com.example.treasure.domain.uiModels.User

/**
 * Domain repository contract defining authentication and user identity management operations.
 *
 * Mediates interaction between Google Sign-In / Credential Manager authorization tokens
 * and the backend authentication service. All methods return encapsulated [Result] monads
 * to propagate successes or structured domain exceptions to calling ViewModels and use cases.
 */
interface AuthRepository {
    /**
     * Authenticates a user against the backend server utilizing a Google OAuth ID token.
     *
     * Upon successful backend verification, JWT access/refresh credentials and user details
     * are persisted, and the active session is initialized.
     *
     * @param idToken The Google OAuth 2.0 ID token string obtained from Credential Manager or Google Sign-In.
     * @return [Result.success] containing the authenticated [User] profile, or [Result.failure] on error.
     */
    suspend fun loginWithGoogle(idToken: String): Result<User>

    /**
     * Terminates the active user session.
     *
     * Invalidates local authentication tokens, wipes user profile data from storage,
     * and broadcasts session revocation across observable application state flows.
     *
     * @return [Result.success] if logout completed successfully, or [Result.failure] if clearing failed.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Permanently deletes the authenticated user's account and associated data on the backend server.
     *
     * Clears all local user interactions, cached preferences, and session tokens upon server confirmation.
     *
     * @return [Result.success] on successful account deletion, or [Result.failure] if server rejection occurs.
     */
    suspend fun deleteAccount(): Result<Unit>
}
