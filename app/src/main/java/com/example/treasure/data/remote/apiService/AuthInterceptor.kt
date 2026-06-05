package com.example.treasure.data.remote.apiService

import android.util.Log
import com.example.treasure.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        Log.d(
            "AUTH_INTERCEPTOR",
            "Request=${originalRequest.method} ${originalRequest.url}"
        )
        val requestBuilder = originalRequest.newBuilder()

        // 1. Fetch the JWT Access Token from Secure Storage
        val accessToken = tokenManager.getAccessToken()
        Log.d(
            "AUTH_INTERCEPTOR",
            "Access token exists=${!accessToken.isNullOrBlank()}"
        )
        // 2. If the token exists, attach it to the Authorization header
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        // 3. Proceed with the modified request
        val response = chain.proceed(requestBuilder.build())
        Log.d(
            "AUTH_INTERCEPTOR",
            "Response code=${response.code} url=${originalRequest.url}"
        )

        return response
    }
}