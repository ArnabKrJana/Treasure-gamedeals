package com.example.treasure.data.remote.apiService

import com.example.treasure.data.remote.dto.AuthResponse
import com.example.treasure.data.remote.dto.DriveAccessRequest
import com.example.treasure.data.remote.dto.DriveAccessResponse
import com.example.treasure.data.remote.dto.DriveSyncRequest
import com.example.treasure.data.remote.dto.DriveSyncStatusResponse
import com.example.treasure.data.remote.dto.GameDto
import com.example.treasure.data.remote.dto.GoogleLoginRequest
import com.example.treasure.data.remote.dto.MessageResponse
import com.example.treasure.data.remote.dto.RefreshTokenRequest
import com.example.treasure.data.remote.dto.SpringPageResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit REST API interface for communicating with the Spring Boot Backend-for-Frontend (BFF).
 *
 * Defines HTTP operations across four primary controller domains:
 * - Authentication (`/auth`): OAuth login exchanges, JWT token refresh, and logout.
 * - Catalog & Deals (`/games`): Paginated deal streams, anticipated games, platform queries, and detail enrichment.
 * - User Profile & Cloud Backup (`/users`): Account deletion, Google Drive authorization linking, and RabbitMQ sync queuing.
 * - Wishlist (`/users/wishlist`): Wishlist synchronization, toggle endpoints, and price update feeds.
 */
interface TreasureBackendApi {

    /**
     * Authenticates a user with a third-party OAuth provider token.
     *
     * @param provider OAuth provider identifier (e.g., "google").
     * @param request Container holding the OAuth ID token.
     * @return [Response] enclosing [AuthResponse] with access/refresh tokens and user details.
     */
    @POST("auth/{provider}")
    suspend fun login(
        @Path("provider") provider: String,
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

    /**
     * Asynchronously renews an expired access token using a valid refresh token.
     *
     * @param request Container holding the active refresh token.
     * @return [Response] enclosing renewed [AuthResponse] credentials.
     */
    @POST("auth/refresh")
    suspend fun refreshSession(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    /**
     * Synchronously renews an expired access token.
     *
     * Invoked exclusively by [TokenAuthenticator] via [Call.execute] to prevent thread deadlocks.
     *
     * @param request Container holding the active refresh token.
     * @return Retrofit [Call] handle executing synchronously for renewed [AuthResponse] credentials.
     */
    @POST("auth/refresh")
    fun refreshSessionSync(
        @Body request: RefreshTokenRequest
    ): Call<AuthResponse>

    /**
     * Logs the user out on the backend server, invalidating active refresh tokens.
     *
     * @param request Container holding the refresh token to invalidate.
     * @return [Response] enclosing a confirmation [MessageResponse].
     */
    @POST("auth/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest
    ): Response<MessageResponse>

    /**
     * Fetches a paginated slice of game deals for a specific catalog category.
     *
     * @param category Target category identifier (e.g., "HOT_DEALS", "LOWEST_PRICE").
     * @param page Zero-indexed page number to retrieve.
     * @param size Page batch size.
     * @return [Response] enclosing a Spring Data [SpringPageResponse] of [GameDto] items.
     */
    @GET("games/deals")
    suspend fun getDeals(
        @Query("category") category: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 40
    ): Response<SpringPageResponse<GameDto>>

    /**
     * Retrieves the curated list of highly anticipated upcoming game titles.
     *
     * @param page Zero-indexed page number to retrieve.
     * @param size Page batch size.
     * @return [Response] enclosing a [SpringPageResponse] of [GameDto] items.
     */
    @GET("games/anticipated")
    suspend fun getAnticipatedGames(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<SpringPageResponse<GameDto>>

    /**
     * Fetches a paginated slice of game deals filtered by operating system platform.
     *
     * @param platformId Numeric platform identifier (2 for macOS, 3 for Linux).
     * @param page Zero-indexed page number to retrieve.
     * @param size Page batch size.
     * @return [Response] enclosing a [SpringPageResponse] of [GameDto] items.
     */
    @GET("games/platforms/{platformId}")
    suspend fun getPlatformDeals(
        @Path("platformId") platformId: Int,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 40
    ): Response<SpringPageResponse<GameDto>>

    /**
     * Searches catalog deals matching query text keywords.
     *
     * @param query Search query text submitted by the user.
     * @return [Response] enclosing a list of matching [GameDto] items.
     */
    @GET("games/search")
    suspend fun searchGames(
        @Query("query") query: String
    ): Response<List<GameDto>>

    /**
     * Fetches comprehensive metadata for a specific game deal for detail view enrichment.
     *
     * @param gameId Unique identifier of the game title.
     * @return [Response] enclosing detailed [GameDto] metadata.
     */
    @GET("games/{gameId}/details")
    suspend fun getGameDetails(
        @Path("gameId") gameId: String
    ): Response<GameDto>

    /**
     * Links the user's Google Drive storage by exchanging an OAuth server authorization code.
     *
     * @param request Container holding the Google server authorization code.
     * @return [Response] enclosing [DriveAccessResponse] detailing link status.
     */
    @POST("users/me/drive/link")
    suspend fun linkGoogleDrive(
        @Body request: DriveAccessRequest
    ): Response<DriveAccessResponse>

    /**
     * Permanently deletes the authenticated user's account and personal data from the server.
     *
     * @return [Response] enclosing a status [MessageResponse].
     */
    @DELETE("users/me")
    suspend fun deleteMyAccount(): Response<MessageResponse>

    /**
     * Enqueues high-resolution game wallpapers for asynchronous backup to Google Drive via RabbitMQ.
     *
     * @param request List of game artwork assets to queue for backup.
     * @return [Response] enclosing a status [MessageResponse].
     */
    @POST("users/me/drive/sync")
    suspend fun queueDriveSync(
        @Body request: DriveSyncRequest
    ): Response<MessageResponse>

    /**
     * Polls the asynchronous Google Drive backup processing status for a list of game IDs.
     *
     * @param gameIds List of game identifier strings to inspect.
     * @return [Response] enclosing a list of [DriveSyncStatusResponse] statuses.
     */
    @POST("users/me/drive/sync/status")
    suspend fun checkDriveSyncStatus(
        @Body gameIds: List<String>
    ): Response<List<DriveSyncStatusResponse>>

    /**
     * Retrieves the set of all game IDs currently wishlisted by the authenticated user.
     *
     * @return [Response] enclosing a list of wishlisted game ID strings.
     */
    @GET("users/wishlist")
    suspend fun getMyWishlistIds(): Response<List<String>>

    /**
     * Toggles the presence of a game in the authenticated user's cloud wishlist.
     *
     * @param gameId Identifier of the target game title.
     * @return Empty [Response] with HTTP 200 on success.
     */
    @POST("users/wishlist/{gameId}")
    suspend fun toggleWishlist(
        @Path("gameId") gameId: String
    ): Response<Unit>

    /**
     * Retrieves current prices for wishlisted titles for background price drop detection.
     *
     * @return [Response] enclosing a map of game IDs to current price strings.
     */
    @GET("users/wishlist/prices")
    suspend fun getWishlistPrices(): Response<Map<String, String>>

    /**
     * Retrieves detailed game deal objects for all titles in the user's wishlist.
     *
     * @return [Response] enclosing a list of [GameDto] items.
     */
    @GET("users/wishlist/detailed")
    suspend fun getDetailedWishlist(): Response<List<GameDto>>
}
