package com.example.treasure.di.apiModule

import com.example.treasure.data.remote.apiService.AuthInterceptor
import com.example.treasure.data.remote.apiService.ItadApi
import com.example.treasure.data.remote.apiService.SteamApi
import com.example.treasure.data.remote.networkDto.steam.SteamRequirementsDeserializer
import com.example.treasure.data.remote.networkDto.steam.SteamRequirementsDto
import com.example.treasure.utils.Constants
import com.example.treasure.utils.TokenManager
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ItadService {

    // Provide the Interceptor
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder().readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder().client(client).baseUrl(
        Constants.ITAD_URL
    ).addConverterFactory(GsonConverterFactory.create()).build()

    @Provides
    @Singleton
    fun provideService(retrofit: Retrofit): ItadApi = retrofit.create(ItadApi::class.java)
}


@Module
@InstallIn(SingletonComponent::class)
object SteamService {

    @Provides
    @Singleton
    @Named("SteamClient")
    fun provideHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named("SteamRetrofit")
    fun provideRetrofit(@Named("SteamClient") client: OkHttpClient): Retrofit {

        // FIX: Register the Custom Adapter here
        val customGson = GsonBuilder()
            .registerTypeAdapter(SteamRequirementsDto::class.java, SteamRequirementsDeserializer())
            .create()

        return Retrofit.Builder()
            .client(client)
            .baseUrl(Constants.STEAM_URL)
            // FIX: Pass the customGson to the factory
            .addConverterFactory(GsonConverterFactory.create(customGson))
            .build()
    }

    @Provides
    @Singleton
    fun provideService(@Named("SteamRetrofit") retrofit: Retrofit): SteamApi =
        retrofit.create(SteamApi::class.java)
}