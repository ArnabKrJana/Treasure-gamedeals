package com.example.treasure.data.repositoryImpl

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.data.local.remoteMediators.DealRemoteMediator
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.remote.dto.GameDto
import com.example.treasure.data.toEntity
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.utils.ColorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val treasureBackendApi: TreasureBackendApi,
    private val db: TreasureDatabase
) : GameRepository {

    private val dealDao = db.dealDao()
    private val userInteractionDao = db.userInteractionDao()

    @OptIn(ExperimentalPagingApi::class)
    override fun getDealsPaged(category: DealCategory): Flow<PagingData<DealEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                enablePlaceholders = true,
                prefetchDistance = 12
            ),
            // Updated to pass the new API instead of ItadApi
            remoteMediator = DealRemoteMediator(db, treasureBackendApi, category),
            pagingSourceFactory = {
                dealDao.getDealsByCategory(category)
            }
        ).flow
    }

    override suspend fun getAnticipatedGames(): List<GameCardItem> = withContext(Dispatchers.IO) {
        try {
            val response = treasureBackendApi.getAnticipatedGames()

            if (response.isSuccessful && response.body() != null) {
                // THE FIX: We MUST access .content here to get the actual List<GameDto> out of the SpringPageResponse wrapper
                return@withContext response.body()!!.content.map { gameDto ->
                    GameCardItem(
                        id = gameDto.id,
                        listingIndex = 0, // Unused for carousel
                        title = gameDto.title,
                        thumbnail = gameDto.thumbnail ?: gameDto.screenshots?.firstOrNull() ?: "",
                        store = gameDto.primaryStore ?: "Multiple",
                        upVotes = if (gameDto.upVotes != null) UpVotes(
                            gameDto.upVotes,
                            safeColorCode(gameDto.upVoteColor)
                        ) else null,
                        price = if (gameDto.originalPrice != null && gameDto.currentPrice != null) {
                            Price(
                                originalPrice = gameDto.originalPrice,
                                currentPrice = gameDto.currentPrice
                            )
                        } else null,
                        genres = gameDto.genres ?: emptyList(),
                        releaseDate = gameDto.expectedReleaseDate

                    )
                }
            }
            return@withContext emptyList()
        } catch (e: Exception) {
            Log.e("GameRepository", "Failed to fetch anticipated games", e)
            return@withContext emptyList()
        }
    }

    override fun observeGameDetails(dealId: String): Flow<DealEntity?> {
        return dealDao.observeDealById(dealId)
    }

    // --- THE MASTER CLEANUP ---
    override suspend fun fetchAndEnrichGameDetails(dealId: String): Unit =
        withContext(Dispatchers.IO) {
            try {
                // 1. Single call to your Spring Boot BFF
                val response = treasureBackendApi.getGameDetails(dealId)

                if (response.isSuccessful && response.body() != null) {
                    // 2. Map the clean DTO to your Room Entity using our Mappers.kt
                    val mappedEntity = response.body()!!.toEntity(DealCategory.SEARCH, 0)

                    // 3. Attempt to update the existing record
                    val rowsUpdated = dealDao.updateDealDetails(
                        dealId = dealId,
                        description = mappedEntity.description,
                        screenshots = mappedEntity.screenshots,
                        otherDeals = mappedEntity.otherStores,
                        developer = mappedEntity.developer,
                        publisher = mappedEntity.publisher,
                        franchise = mappedEntity.franchise,
                        releaseDate = mappedEntity.releaseDate,
                        maturity = mappedEntity.maturityRating,
                        sysReqs = mappedEntity.systemRequirements,
                        trailerUrl = mappedEntity.trailerUrl
                    )

                    // 4. If update returns 0, it means it's a Search Result not in DB. Insert it.
                    if (rowsUpdated == 0) {
                        dealDao.insertDeals(listOf(mappedEntity))
                    }
                }
            } catch (e: Exception) {
                Log.e("GameRepository", "Error fetching game details for ID: $dealId", e)
            }
        }

    override suspend fun searchGames(query: String): List<GameCardItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = treasureBackendApi.searchGames(query)

                if (response.isSuccessful && response.body() != null) {
                    // Map the backend DTO list directly to UI GameCardItems
                    return@withContext response.body()!!.map { it.toGameCardItem() }
                } else {
                    return@withContext emptyList()
                }
            } catch (e: Exception) {
                Log.e("GameRepository", "Error searching games: $query", e)
                return@withContext emptyList()
            }
        }

    // --- USER INTERACTIONS (CART & WISHLIST) ---

    override fun getCartItems(): Flow<List<UserInteractionEntity>> =
        userInteractionDao.getCartItems()

    override fun getWishlistItems(): Flow<List<UserInteractionEntity>> =
        userInteractionDao.getFavorites()

    override fun observeInteractionIds(): Flow<List<String>> =
        userInteractionDao.getAllInteractedIds()

    override suspend fun toggleFavorite(game: GameCardItem) {
        // 1. Optimistic Local Update (Instant UI feedback)
        db.withTransaction {
            val currentInteraction = userInteractionDao.getInteractionForGame(game.id)
            val isCurrentlyFav = currentInteraction?.isFavorite ?: false
            val isInCart = currentInteraction?.isAddedToCart ?: false

            val newInteraction = UserInteractionEntity(
                gameId = game.id,
                isFavorite = !isCurrentlyFav, // Toggle
                isAddedToCart = isInCart,
                timestamp = System.currentTimeMillis(),
                title = game.title,
                thumbnail = game.thumbnail,
                currentPrice = game.price?.currentPrice ?: 0.0,
                originalPrice = game.price?.originalPrice ?: 0.0,
                storeId = game.store,
                latestSyncedPrice = currentInteraction?.latestSyncedPrice,
                lastSyncTimestamp = currentInteraction?.lastSyncTimestamp
            )
            userInteractionDao.insertInteraction(newInteraction)
        }

        // 2. Sync with Spring Boot BFF
        try {
            val response = treasureBackendApi.toggleWishlist(game.id)
            if (!response.isSuccessful) {
                Log.e("GameRepository", "Failed to sync wishlist to cloud: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Network exception syncing wishlist to cloud", e)
        }
    }

    override suspend fun syncWishlistFromCloud(): Unit = withContext(Dispatchers.IO) {
        try {
            // 1. Hit our new fast BFF endpoint
            val response = treasureBackendApi.getDetailedWishlist()

            if (response.isSuccessful && response.body() != null) {
                val fullGames = response.body()!!

                // 2. Save directly into your EXISTING Room Database schema
                db.withTransaction {
                    // Reset all local favorites to false (preserves cart status)
                    userInteractionDao.clearAllFavorites()

                    fullGames.forEach { dto ->
                        // Check if the game is already in DB (e.g., in cart)
                        val existing = userInteractionDao.getInteractionForGame(dto.id)

                        val newInteraction = UserInteractionEntity(
                            gameId = dto.id,
                            isFavorite = true, // Force true because it's from cloud wishlist
                            isAddedToCart = existing?.isAddedToCart ?: false, // Preserve cart state
                            timestamp = existing?.timestamp ?: System.currentTimeMillis(),
                            title = dto.title,
                            thumbnail = dto.thumbnail ?: dto.screenshots?.firstOrNull() ?: "",
                            currentPrice = dto.currentPrice ?: 0.0,
                            originalPrice = dto.originalPrice ?: 0.0,
                            storeId = dto.primaryStore ?: "Multiple",
                            latestSyncedPrice = dto.currentPrice ?: 0.0,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                        userInteractionDao.insertInteraction(newInteraction)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Failed to sync detailed wishlist from cloud", e)
        }
    }

    override suspend fun toggleCart(game: GameCardItem) {
        db.withTransaction {
            val currentInteraction = userInteractionDao.getInteractionForGame(game.id)
            val isCurrentlyFav = currentInteraction?.isFavorite ?: false
            val isInCart = currentInteraction?.isAddedToCart ?: false

            val newInteraction = UserInteractionEntity(
                gameId = game.id,
                isFavorite = isCurrentlyFav,
                isAddedToCart = !isInCart, // Toggle
                timestamp = System.currentTimeMillis(),
                title = game.title,
                thumbnail = game.thumbnail,
                currentPrice = game.price?.currentPrice ?: 0.0,
                originalPrice = game.price?.originalPrice ?: 0.0,
                storeId = game.store,
                latestSyncedPrice = currentInteraction?.latestSyncedPrice,
                lastSyncTimestamp = currentInteraction?.lastSyncTimestamp
            )
            userInteractionDao.insertInteraction(newInteraction)
        }
    }

    // --- HELPER MAPPERS ---

    private fun GameDto.toGameCardItem(): GameCardItem {
        return GameCardItem(
            id = this.id,
            listingIndex = 0,
            title = this.title,
            thumbnail = this.thumbnail ?: this.screenshots?.firstOrNull(),
            store = this.primaryStore ?: "Multiple",
            price = if (this.originalPrice != null && this.currentPrice != null) {
                Price(originalPrice = this.originalPrice, currentPrice = this.currentPrice)
            } else null,
            upVotes = if (this.upVotes != null) UpVotes(
                this.upVotes,
                safeColorCode(this.upVoteColor)
            ) else null,
            genres = this.genres ?: emptyList()
        )
    }

    private fun safeColorCode(colorString: String?): ColorCode {
        return try {
            if (colorString != null) ColorCode.valueOf(colorString.uppercase()) else ColorCode.YELLOW
        } catch (e: IllegalArgumentException) {
            ColorCode.YELLOW
        }
    }
}