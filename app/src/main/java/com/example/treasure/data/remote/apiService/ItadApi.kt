package com.example.treasure.data.remote.apiService

import com.example.treasure.data.remote.networkDto.itad.ItadGameInfoDto
import com.example.treasure.data.remote.networkDto.itad.ItadOverviewPriceDto
import com.example.treasure.data.remote.networkDto.itad.ItadOverviewResponseDto
import com.example.treasure.data.remote.networkDto.itad.ItadResponseDto
import com.example.treasure.data.remote.networkDto.itad.ItadSearchItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ItadApi {


    @GET("deals/v2")
    suspend fun getDeals(

        @Query("country") country:String,
        @Query("sort") sort: String,
        @Query("shops") shops: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<ItadResponseDto>

    @GET("games/info/v2")
    suspend fun getInfo(

        @Query("country") country:String,
       @Query("id") gameId: String
    ): Response<ItadGameInfoDto>

    @POST("games/overview/v2")
    suspend fun getPriceOverview(

        @Query("country") country:String,
        @Query("shops") shops: String,
        @Body gameIds: List<String>
    ): Response<ItadOverviewResponseDto>

    @GET("games/search/v1")
    suspend fun searchGames(

        @Query("title") query: String, // The user's search text
        @Query("limit") limit: Int = 20
    ): Response<List<ItadSearchItemDto>>
}//base url: https://api.isthereanydeal.com/