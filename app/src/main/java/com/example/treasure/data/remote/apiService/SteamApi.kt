package com.example.treasure.data.remote.apiService

import com.example.treasure.data.remote.networkDto.steam.SteamAppResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SteamApi {

    // https://store.steampowered.com/api/appdetails?appids=632470&cc=in

    @GET("appdetails")
    suspend fun getExtraData(
        @Query("appids") appId: Int,
        @Query("cc") country: String = "in"
    ): Response<Map<String, SteamAppResponse>>
}