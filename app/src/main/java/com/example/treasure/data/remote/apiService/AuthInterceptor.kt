package com.example.treasure.data.remote.apiService

import android.util.Log
import com.example.treasure.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp [Interceptor] that dynamically attaches the JWT bearer authentication token to outbound HTTP requests.
 *
 * Inspects [TokenManager] for an active access token and appends the `Authorization: Bearer <token>`
 * header before forwarding the request down the chain.
 *
 * @param tokenManager Injected session credential store providing access tokens.
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    companion object {
        private const val TAG = "AUTH_INTERCEPTOR"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    /**
     * Intercepts the outbound HTTP request chain and injects bearer authentication headers.
     *
     * @param chain OkHttp execution interceptor chain.
     * @return Platform HTTP [Response] from upstream servers or subsequent interceptors.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        Log.d(TAG, "Request=${originalRequest.method} ${originalRequest.url}")
        val requestBuilder = originalRequest.newBuilder()

        val accessToken = tokenManager.getAccessToken()
        Log.d(TAG, "Access token exists=${!accessToken.isNullOrBlank()}")

        if (!accessToken.isNullOrBlank()) {
            requestBuilder.addHeader(HEADER_AUTHORIZATION, "$BEARER_PREFIX$accessToken")
        }

        val response = chain.proceed(requestBuilder.build())
        Log.d(TAG, "Response code=${response.code} url=${originalRequest.url}")

        return response
    }
}
