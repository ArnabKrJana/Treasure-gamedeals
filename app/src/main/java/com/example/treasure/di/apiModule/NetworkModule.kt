package com.example.treasure.di.apiModule


import com.example.treasure.BuildConfig
import com.example.treasure.data.remote.apiService.AuthInterceptor
import com.example.treasure.data.remote.apiService.RateLimitInterceptor
import com.example.treasure.data.remote.apiService.TokenAuthenticator
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.utils.Constants
import com.example.treasure.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        rateLimitInterceptor: RateLimitInterceptor
    ): OkHttpClient {
//        val logging = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }
        val logging = HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
        }
        val customDispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 20 // Must be higher than 5 to prevent deadlocks!
        }
        return OkHttpClient.Builder()
            .dispatcher(customDispatcher)
            .readTimeout(25, TimeUnit.SECONDS)
            .connectTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .client(client)
            .baseUrl(Constants.BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTreasureBackendApi(retrofit: Retrofit): TreasureBackendApi {
        return retrofit.create(TreasureBackendApi::class.java)
    }

}