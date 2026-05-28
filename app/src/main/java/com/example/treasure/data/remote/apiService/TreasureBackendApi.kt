package com.example.treasure.data.remote.apiService


import com.example.treasure.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface TreasureBackendApi {

    // ==========================================
    // AUTHENTICATION CONTROLLER (/api/v1/auth)
    // ==========================================

    @POST("auth/{provider}")
    suspend fun login(
        @Path("provider") provider: String, // e.g., "google"
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshSession(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest
    ): Response<MessageResponse>


    // ==========================================
    // GAME & DEALS CONTROLLER (/games)
    // ==========================================

    @GET("games/deals")
    suspend fun getDeals(
        @Query("category") category: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 40
    ): Response<SpringPageResponse<GameDto>>

    @GET("games/anticipated")
    suspend fun getAnticipatedGames(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<SpringPageResponse<GameDto>>

    @GET("games/platforms/{platformId}")
    suspend fun getPlatformDeals(
        @Path("platformId") platformId: Int, // 2 = Mac, 3 = Linux
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 40
    ): Response<SpringPageResponse<GameDto>>

    @GET("games/search")
    suspend fun searchGames(
        @Query("query") query: String
    ): Response<List<GameDto>> // Note: Backend returns a standard List here

    @GET("games/{gameId}/details")
    suspend fun getGameDetails(
        @Path("gameId") gameId: String
    ): Response<GameDto>


    // ==========================================
    // USER CONTROLLER (/api/v1/users)
    // ==========================================

    @POST("users/me/drive/link")
    suspend fun linkGoogleDrive(
        @Body request: DriveAccessRequest
    ): Response<DriveAccessResponse>

    @DELETE("users/me")
    suspend fun deleteMyAccount(): Response<MessageResponse>

    @POST("users/me/drive/sync")
    suspend fun queueDriveSync(
        @Body request: DriveSyncRequest
    ): Response<MessageResponse>

    @POST("users/me/drive/sync/status")
    suspend fun checkDriveSyncStatus(
        @Body gameIds: List<String>
    ): Response<List<DriveSyncStatusResponse>>


    // ==========================================
    // WISHLIST CONTROLLER (/api/v1/users/wishlist)
    // ==========================================
    @GET("users/wishlist")
    suspend fun getMyWishlistIds(): Response<List<String>>

    @POST("users/wishlist/{gameId}")
    suspend fun toggleWishlist(
        @Path("gameId") gameId: String
    ): Response<String>

    @GET("users/wishlist/prices")
    suspend fun getWishlistPrices(): Response<Map<String, String>>

    @GET("users/wishlist/detailed")
    suspend fun getDetailedWishlist(): Response<List<GameDto>>


}