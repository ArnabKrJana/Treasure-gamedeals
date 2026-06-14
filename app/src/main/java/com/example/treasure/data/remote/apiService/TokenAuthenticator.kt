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

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    // Using Provider prevents the Hilt Circular Dependency crash!
    private val apiProvider: Provider<TreasureBackendApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.e(
            "TOKEN_AUTH",
            "Authenticator triggered for ${response.request.url}"
        )
        // 1. Prevent infinite loops if the refresh endpoint itself returns a 401
        if (response.request.url.encodedPath.contains("auth/refresh")) {
            tokenManager.clearSession()
            return null
        }

        // 2. Grab the refresh token. If we don't have one, we can't refresh.
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken == null) {
            Log.e(
                "TOKEN_AUTH",
                "Refresh token missing"
            )
            return null
        }
        Log.d(
            "TOKEN_AUTH",
            "Refresh token exists=${refreshToken != null}"
        )
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
            // 4. Make the network call to Spring Boot
            try {
                val refreshRequest = RefreshTokenRequest(refreshToken)

                Log.d("TOKEN_AUTH", "Calling refresh endpoint synchronously")

                //  THE FIX: Use .execute() instead of runBlocking
                val refreshResponse = apiProvider.get().refreshSessionSync(refreshRequest).execute()

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    Log.d("TOKEN_AUTH", "Refresh success")
                    val newTokens = refreshResponse.body()!!

                    // Note: Local storage saves can still use runBlocking safely
                    runBlocking {
                        tokenManager.saveTokens(
                            accessToken = newTokens.accessToken,
                            refreshToken = newTokens.refreshToken
                        )
                    }

                    // Retry the original failed request with the new access token!
                    return@synchronized response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                } else {
                    Log.e("TOKEN_AUTH", "Refresh failed. Code=${refreshResponse.code()}")
                    runBlocking { tokenManager.clearSession() }
                    return@synchronized null
                }
            } catch (e: Exception) {
                Log.e("TOKEN_AUTH", "Refresh exception", e)
                runBlocking { tokenManager.clearSession() }
                return@synchronized null
            }
        }
    }
}