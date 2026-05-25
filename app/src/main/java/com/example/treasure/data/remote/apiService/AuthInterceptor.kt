package com.example.treasure.data.remote.apiService

import com.example.treasure.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // 1. Fetch the JWT Access Token from Secure Storage
        val accessToken = tokenManager.getAccessToken()

        // 2. If the token exists, attach it to the Authorization header
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        // 3. Proceed with the modified request
        return chain.proceed(requestBuilder.build())
    }
}