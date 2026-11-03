package com.example.treasure.data.remote.apiService

import android.util.Log
import com.example.treasure.data.remote.dto.RefreshTokenRequest
import com.example.treasure.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

/**
 * OkHttp [Authenticator] that handles HTTP 401 Unauthorized responses by refreshing the JWT access token.
 *
 * Employs a thread-synchronized token refresh workflow:
 * 1. Checks outbound request paths to prevent infinite recursion if the `/auth/refresh` endpoint itself rejects credentials.
 * 2. Synchronizes on a lock (`synchronized(this)`) to prevent multiple concurrent HTTP calls from triggering parallel refresh requests.
 * 3. Double-checks whether a previous thread already completed the refresh while the current thread was queued.
 * 4. Executes a synchronous network call to the backend via [TreasureBackendApi.refreshSessionSync] using [retrofit2.Call.execute].
 * 5. Persists the renewed access and refresh token pair into [TokenManager] and updates the failed request header for retry.
 * 6. If the refresh token is expired or rejected, atomically wipes session credentials via [TokenManager.clearSession] to eject the user to login.
 *
 * Note: Injected with [Provider] of [TreasureBackendApi] to resolve circular dependency between OkHttpClient and Retrofit API.
 *
 * @param tokenManager Centralized session token and credential store.
 * @param apiProvider Lazy provider for [TreasureBackendApi] to resolve circular dependency graph during DI binding.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val apiProvider: Provider<TreasureBackendApi>
) : Authenticator {

    companion object {
        private const val TAG = "TOKEN_AUTH"
        private const val REFRESH_ENDPOINT_SEGMENT = "auth/refresh"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    /**
     * Authenticates an expired request following an HTTP 401 challenge.
     *
     * @param route Optional network route over which the connection was established.
     * @param response The HTTP 401 response triggering authentication.
     * @return New [Request] instance containing updated authorization credentials, or `null` if unable to refresh.
     */
    override fun authenticate(route: Route?, response: Response): Request? {
        Log.e(TAG, "Authenticator triggered for ${response.request.url}")

        if (response.request.url.encodedPath.contains(REFRESH_ENDPOINT_SEGMENT)) {
            tokenManager.clearSession()
            return null
        }

        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken == null) {
            Log.e(TAG, "Refresh token missing")
            return null
        }
        Log.d(TAG, "Refresh token exists=${refreshToken != null}")

        return synchronized(this) {
            val currentAccessToken = tokenManager.getAccessToken()
            if (response.request.header(HEADER_AUTHORIZATION) != "$BEARER_PREFIX$currentAccessToken") {
                return@synchronized response.request.newBuilder()
                    .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$currentAccessToken")
                    .build()
            }

            try {
                val refreshRequest = RefreshTokenRequest(refreshToken)
                Log.d(TAG, "Calling refresh endpoint synchronously")

                val refreshResponse = apiProvider.get().refreshSessionSync(refreshRequest).execute()

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    Log.d(TAG, "Refresh success")
                    val newTokens = refreshResponse.body()!!

                    runBlocking {
                        tokenManager.saveTokens(
                            accessToken = newTokens.accessToken,
                            refreshToken = newTokens.refreshToken
                        )
                    }

                    return@synchronized response.request.newBuilder()
                        .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX${newTokens.accessToken}")
                        .build()
                } else {
                    Log.e(TAG, "Refresh failed. Code=${refreshResponse.code()}")
                    runBlocking { tokenManager.clearSession() }
                    return@synchronized null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Refresh exception", e)
                runBlocking { tokenManager.clearSession() }
                return@synchronized null
            }
        }
    }
}
