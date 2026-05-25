package com.example.treasure.data.remote.apiService

import com.example.treasure.data.remote.dto.RefreshTokenRequest
import com.example.treasure.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    // Using Provider prevents the Hilt Circular Dependency crash!
    private val apiProvider: Provider<TreasureBackendApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Prevent infinite loops if the refresh endpoint itself returns a 401
        if (response.request.url.encodedPath.contains("auth/refresh")) {
            tokenManager.clearSession()
            return null
        }

        // 2. Grab the refresh token. If we don't have one, we can't refresh.
        val refreshToken = tokenManager.getRefreshToken() ?: return null

        // 3. Synchronize to prevent multiple parallel network calls from triggering multiple refreshes
        return synchronized(this) {
            // Double-check if another thread already refreshed the token while this thread was waiting
            val currentAccessToken = tokenManager.getAccessToken()
            if (response.request.header("Authorization") != "Bearer $currentAccessToken") {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // 4. Make the network call to Spring Boot
            try {
                val refreshRequest = RefreshTokenRequest(refreshToken)

                // RunBlocking is required here because OkHttp Interceptors are synchronous,
                // but our Retrofit interface uses suspend functions.
                val refreshResponse = runBlocking {
                    apiProvider.get().refreshSession(refreshRequest)
                }

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val newTokens = refreshResponse.body()!!

                    // Save the brand new tokens
                    tokenManager.saveTokens(
                        accessToken = newTokens.accessToken,
                        refreshToken = newTokens.refreshToken
                    )

                    // Retry the original failed request with the new access token!
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                } else {
                    // The refresh token itself is expired or revoked. Forced Logout.
                    tokenManager.clearSession()
                    null
                }
            } catch (e: Exception) {
                // Network failure during refresh. Clear session to be safe.
                tokenManager.clearSession()
                null
            }
        }
    }
}