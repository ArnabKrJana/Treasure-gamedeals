package com.example.treasure.data.remote.apiService


import com.example.treasure.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface TreasureBackendApi {

    // ==========================================
    // AUTHENTICATION CONTROLLER (/api/v1/auth)
    // ==========================================

    @POST("api/v1/auth/{provider}")
    suspend fun login(
        @Path("provider") provider: String, // e.g., "google"
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshSession(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest
    ): Response<MessageResponse>


    // ==========================================
    // GAME & DEALS CONTROLLER (/api/v1/games)
    // ==========================================

    @GET("api/v1/games/deals")
    suspend fun getDeals(
        @Query("category") category: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 40
    ): Response<SpringPageResponse<GameDto>>

    @GET("api/v1/games/anticipated")
    suspend fun getAnticipatedGames(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<SpringPageResponse<GameDto>>

    @GET("api/v1/games/platforms/{platformId}")
    suspend fun getPlatformDeals(
        @Path("platformId") platformId: Int, // 2 = Mac, 3 = Linux
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 40
    ): Response<SpringPageResponse<GameDto>>

    @GET("api/v1/games/search")
    suspend fun searchGames(
        @Query("query") query: String
    ): Response<List<GameDto>> // Note: Backend returns a standard List here

    @GET("api/v1/games/{gameId}/details")
    suspend fun getGameDetails(
        @Path("gameId") gameId: String
    ): Response<GameDto>


    // ==========================================
    // USER CONTROLLER (/api/v1/users)
    // ==========================================

    @POST("api/v1/users/me/drive/link")
    suspend fun linkGoogleDrive(
        @Body request: DriveAccessRequest
    ): Response<DriveAccessResponse>

    @DELETE("api/v1/users/me")
    suspend fun deleteMyAccount(): Response<MessageResponse>

    @POST("api/v1/users/me/drive/sync")
    suspend fun queueDriveSync(
        @Body request: DriveSyncRequest
    ): Response<MessageResponse>

    @POST("api/v1/users/me/drive/sync/status")
    suspend fun checkDriveSyncStatus(
        @Body gameIds: List<String>
    ): Response<List<DriveSyncStatusResponse>>


    // ==========================================
    // WISHLIST CONTROLLER (/api/v1/users/wishlist)
    // ==========================================

    @POST("api/v1/users/wishlist/{gameId}")
    suspend fun toggleWishlist(
        @Path("gameId") gameId: String
    ): Response<String>

    @GET("api/v1/users/wishlist/prices")
    suspend fun getWishlistPrices(): Response<Map<String, String>>

    @GET("api/v1/users/wishlist/detailed")
    suspend fun getDetailedWishlist(): Response<List<GameDto>>


}