package com.example.treasure.data.remote.apiService

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RateLimitInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)

        // If Spring Boot blocks us for going too fast
        if (response.code == 429) {
            // Read the exact seconds Spring Boot told us to wait
            val retryAfterSeconds = response.header("X-RateLimit-Retry-After")?.toLongOrNull() ?: 2L
            Log.e("NETWORK", "429 Too Many Requests! Pausing for $retryAfterSeconds seconds...")

            // Close the rejected response to prevent memory leaks
            response.close()

            // Pause this specific background thread
            Thread.sleep(retryAfterSeconds * 1000)

            // Retry the request silently!
            response = chain.proceed(request)
        }

        return response
    }
}