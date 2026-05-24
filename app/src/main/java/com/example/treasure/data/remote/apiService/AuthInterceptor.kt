package com.example.treasure.data.remote.apiService

import com.example.treasure.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

//class AuthInterceptor @Inject constructor(
//    private val tokenManager: TokenManager
//) : Interceptor {
//
//    override fun intercept(chain: Interceptor.Chain): Response {
//        val originalRequest = chain.request()
//        val originalUrl = originalRequest.url
//
//        // 1. Fetch Key from Secure Storage
//        // Fallback to empty string if null (request will fail, but app won't crash)
//        val apiKey = tokenManager.getApiKey() ?: ""
//
//        // 2. Append key to the URL as a query parameter "?key=..."
//        val newUrl = originalUrl.newBuilder()
//            .addQueryParameter("key", apiKey)
//            .build()
//
//        // 3. Rebuild the request with the new URL
//        val newRequest = originalRequest.newBuilder()
//            .url(newUrl)
//            .build()
//
//        return chain.proceed(newRequest)
//    }
//}

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // 1. Fetch Key from Secure Storage
        // Fallback to empty string if null (request will fail, but app won't crash)
        val apiKey = tokenManager.getApiKey() ?: ""

        // 2. Append key to the URL as a query parameter "?key=..."
        val newUrl = originalUrl.newBuilder()
            .addQueryParameter("key", apiKey)
            .build()

        // 3. Rebuild the request with the new URL
        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}