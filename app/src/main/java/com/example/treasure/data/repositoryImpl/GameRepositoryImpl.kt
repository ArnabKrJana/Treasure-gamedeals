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
    private val treasureBackendApi: TreasureBackendApi, // <-- Replaced ItadApi & SteamApi
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

        // NOTE: In Step 5, we will trigger treasureBackendApi.toggleWishlist(game.id) here!
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