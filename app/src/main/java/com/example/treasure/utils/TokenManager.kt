package com.example.treasure.utils

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.treasure.domain.uiModels.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized session and authentication token manager.
 *
 * Persists OAuth 2.0 JWT access tokens, refresh tokens, and serialized user profile data
 * into private [SharedPreferences]. Exposes an observable [isSessionActive] reactive [StateFlow]
 * to drive application-wide authentication state and automatically trigger session invalidation /
 * navigation ejects upon logout or refresh token expiry.
 *
 * Thread Safety: Backed by Android's [SharedPreferences] and thread-safe [MutableStateFlow].
 *
 * @param sharedPreferences Injected application [SharedPreferences] instance dedicated to auth credentials.
 */
@Singleton
class TokenManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    /**
     * Backing reactive state indicating whether an authenticated user session is active.
     * Initializes to `true` on cold boot if an access token is already persisted.
     */
    private val _isSessionActive = MutableStateFlow(getAccessToken() != null)

    /**
     * Publicly observable stream of the active session state.
     * Observed by navigation composables and root ViewModels to redirect users when sessions expire.
     */
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    companion object {
        private const val TAG = "TOKEN_MANAGER"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_DATA = "user_data"
    }

    /**
     * Atomically stores new access and refresh token credentials and marks the session active.
     *
     * @param accessToken Short-lived JWT bearer token used to authorize API network requests.
     * @param refreshToken Long-lived refresh token used to obtain renewed access tokens via OkHttp authenticator.
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        Log.d(TAG, "Saving tokens")
        sharedPreferences.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        _isSessionActive.value = true
    }

    /**
     * Retrieves the currently persisted JWT access token from preferences.
     *
     * @return The JWT access token string, or `null` if the user is unauthenticated.
     */
    fun getAccessToken(): String? {
        val token = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        Log.d(TAG, "getAccessToken null=${token == null}")
        return token
    }

    /**
     * Retrieves the currently persisted refresh token from preferences.
     *
     * @return The refresh token string, or `null` if unauthenticated.
     */
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Serializes and persists authenticated [User] profile details into JSON format.
     *
     * @param user The authenticated user domain entity to persist.
     */
    fun saveUser(user: User) {
        val userJson = Json.encodeToString(user)
        sharedPreferences.edit { putString(KEY_USER_DATA, userJson) }
    }

    /**
     * Deserializes and retrieves the persisted [User] profile from preferences.
     *
     * @return The cached [User] model, or `null` if missing or deserialization fails.
     */
    fun getUser(): User? {
        val userJson = sharedPreferences.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            try {
                Json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    /**
     * Atomically wipes all persisted tokens and cached user profile data.
     *
     * Emits `false` to [isSessionActive], triggering root navigation to transition back
     * to the unauthenticated authentication flow.
     */
    fun clearSession() {
        Log.e(TAG, "SESSION CLEARED")
        sharedPreferences.edit { clear() }
        _isSessionActive.value = false
    }
}
