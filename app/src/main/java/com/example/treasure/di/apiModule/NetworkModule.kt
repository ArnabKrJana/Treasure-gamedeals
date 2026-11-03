package com.example.treasure.di.apiModule

import com.example.treasure.BuildConfig
import com.example.treasure.data.remote.apiService.AuthInterceptor
import com.example.treasure.data.remote.apiService.RateLimitInterceptor
import com.example.treasure.data.remote.apiService.TokenAuthenticator
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.utils.Constants
import com.example.treasure.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Dagger Hilt dependency injection module provisioning networking clients, interceptors, and Retrofit services.
 *
 * Configures the application-wide [OkHttpClient] with custom request dispatchers, connection timeouts,
 * conditional debug logging, JWT authorization injection, rate-limiting backoff, and synchronized token renewal.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 25L
    private const val MAX_REQUESTS = 64
    private const val MAX_REQUESTS_PER_HOST = 20

    /**
     * Provides a singleton [AuthInterceptor] to append bearer authorization headers.
     *
     * @param tokenManager Injected session credential store.
     * @return Configured [AuthInterceptor].
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    /**
     * Constructs and provisions the central application [OkHttpClient].
     *
     * Configured with:
     * - Custom [Dispatcher] with `maxRequestsPerHost = 20` to prevent connection queue deadlocks during parallel requests.
     * - 25-second connect and read timeouts.
     * - Conditional [HttpLoggingInterceptor] logging HTTP bodies in debug builds and disabled in release.
     * - [AuthInterceptor] for JWT token injection.
     * - [RateLimitInterceptor] for transparent HTTP 429 backoff retry.
     * - [TokenAuthenticator] for synchronized token refresh upon HTTP 401 challenges.
     *
     * @param authInterceptor Outbound authorization header interceptor.
     * @param tokenAuthenticator HTTP 401 token refresh authenticator.
     * @param rateLimitInterceptor HTTP 429 rate limit backoff interceptor.
     * @return Singleton [OkHttpClient] instance.
     */
    @Provides
    @Singleton
    fun provideHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        rateLimitInterceptor: RateLimitInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val customDispatcher = Dispatcher().apply {
            maxRequests = MAX_REQUESTS
            maxRequestsPerHost = MAX_REQUESTS_PER_HOST
        }

        return OkHttpClient.Builder()
            .dispatcher(customDispatcher)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .build()
    }

    /**
     * Builds and provisions the application-wide [Retrofit] HTTP client.
     *
     * Configured with the base backend URL ([Constants.BACKEND_URL]) and [GsonConverterFactory].
     *
     * @param client The configured singleton [OkHttpClient].
     * @return Configured [Retrofit] instance.
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .client(client)
            .baseUrl(Constants.BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Generates and provisions the typed [TreasureBackendApi] Retrofit interface implementation.
     *
     * @param retrofit The configured singleton [Retrofit] instance.
     * @return Concrete [TreasureBackendApi] proxy.
     */
    @Provides
    @Singleton
    fun provideTreasureBackendApi(retrofit: Retrofit): TreasureBackendApi {
        return retrofit.create(TreasureBackendApi::class.java)
    }
}
