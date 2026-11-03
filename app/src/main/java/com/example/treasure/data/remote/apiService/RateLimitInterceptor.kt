package com.example.treasure.data.remote.apiService

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp [Interceptor] providing automated backoff and transparent retry for HTTP 429 (Too Many Requests) responses.
 *
 * When the backend API rate limiter rejects a request, this interceptor extracts the backoff duration
 * from the `X-RateLimit-Retry-After` response header (defaulting to 2 seconds if absent), closes the
 * rejected response body to prevent connection leaks, suspends the caller thread, and transparently re-executes
 * the request.
 */
class RateLimitInterceptor @Inject constructor() : Interceptor {

    companion object {
        private const val TAG = "NETWORK"
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HEADER_RETRY_AFTER = "X-RateLimit-Retry-After"
        private const val DEFAULT_BACKOFF_SECONDS = 2L
    }

    /**
     * Intercepts the request execution, catching HTTP 429 responses and executing backoff retries.
     *
     * @param chain OkHttp execution interceptor chain.
     * @return Terminal HTTP [Response], either from the initial call or the subsequent retry.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)

        if (response.code == HTTP_TOO_MANY_REQUESTS) {
            val retryAfterSeconds = response.header(HEADER_RETRY_AFTER)?.toLongOrNull() ?: DEFAULT_BACKOFF_SECONDS
            Log.e(TAG, "429 Too Many Requests! Pausing for $retryAfterSeconds seconds...")

            response.close()

            Thread.sleep(retryAfterSeconds * 1000)

            response = chain.proceed(request)
        }

        return response
    }
}
