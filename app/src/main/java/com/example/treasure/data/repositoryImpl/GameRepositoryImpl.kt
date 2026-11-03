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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Concrete implementation of [GameRepository] orchestrating offline-first catalog queries,
 * Paging 3 deal streams, lazy detail enrichment, search, and optimistic user interaction mutations.
 *
 * Employs a multi-tiered persistence and caching strategy:
 * - Room database acts as the single source of truth for deal feeds and user interactions.
 * - [DealRemoteMediator] coordinates background network pagination from [TreasureBackendApi] into Room.
 * - In-memory time-to-live (TTL) caching (30 minutes) cushions anticipated game queries to conserve network bandwidth.
 * - Optimistic local database mutations ensure instant zero-latency UI responses for wishlist and cart actions,
 *   synchronized asynchronously with backend cloud endpoints.
 *
 * @param treasureBackendApi Retrofit client communicating with the backend BFF.
 * @param db Central Room database managing persistent tables and transactions.
 */
class GameRepositoryImpl @Inject constructor(
    private val treasureBackendApi: TreasureBackendApi,
    private val db: TreasureDatabase
) : GameRepository {

    private val dealDao = db.dealDao()
    private val userInteractionDao = db.userInteractionDao()

    companion object {
        private const val TAG = "GameRepository"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L
        private const val PAGING_PAGE_SIZE = 20
        private const val PAGING_PREFETCH_DISTANCE = 5
    }

    private var cachedAnticipatedGames: List<GameCardItem>? = null
    private var cacheTimestamp: Long = 0L

    /**
     * Produces a continuous stream of paginated [DealEntity] records for the specified [DealCategory].
     *
     * Integrates [DealRemoteMediator] with Room's [androidx.paging.PagingSource] via [Pager].
     *
     * @param category The catalog category partition to stream.
     * @return Cold [Flow] emitting reactive [PagingData] batches.
     */
    @OptIn(ExperimentalPagingApi::class)
    override fun getDealsPaged(category: DealCategory): Flow<PagingData<DealEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGING_PAGE_SIZE,
                enablePlaceholders = true,
                prefetchDistance = PAGING_PREFETCH_DISTANCE
            ),
            remoteMediator = DealRemoteMediator(db, treasureBackendApi, category),
            pagingSourceFactory = {
                dealDao.getDealsByCategory(category)
            }
        ).flow
    }

    /**
     * Retrieves the curated list of highly anticipated upcoming game titles.
     *
     * Evaluates the in-memory cache against the 30-minute expiration window before querying
     * the backend server. Falls back to stale cached data if network connectivity fails.
     *
     * @return List of [GameCardItem] representations.
     */
    override suspend fun getAnticipatedGames(): List<GameCardItem> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()

        if (cachedAnticipatedGames != null && (currentTime - cacheTimestamp) < CACHE_DURATION_MS) {
            Log.d(TAG, "Returning cached anticipated games")
            return@withContext cachedAnticipatedGames!!
        }

        try {
            Log.d(TAG, "Fetching fresh anticipated games from server")
            val response = treasureBackendApi.getAnticipatedGames()

            if (response.isSuccessful && response.body() != null) {
                val freshGames = response.body()!!.content.map { gameDto ->
                    GameCardItem(
                        id = gameDto.id,
                        listingIndex = 0,
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

                cachedAnticipatedGames = freshGames
                cacheTimestamp = currentTime

                return@withContext freshGames
            }
            return@withContext emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch anticipated games", e)
            return@withContext cachedAnticipatedGames ?: emptyList()
        }
    }

    /**
     * Observes real-time changes to a cached deal entity from the local Room database.
     *
     * @param dealId Unique identifier of the deal.
     * @return Cold [Flow] emitting the cached [DealEntity], or `null` if not in local storage.
     */
    override fun observeGameDetails(dealId: String): Flow<DealEntity?> {
        return dealDao.observeDealById(dealId)
    }

    /**
     * Lazily fetches comprehensive game details from the backend and enriches the local Room record.
     *
     * Executes a single request to the backend BFF, maps the received [GameDto] to [DealEntity],
     * and updates existing database rows via [dealDao.updateDealDetails]. If the deal does not
     * yet exist locally (e.g., opened from Search), inserts the complete entity.
     *
     * @param dealId Unique identifier of the game deal to enrich.
     */
    override suspend fun fetchAndEnrichGameDetails(dealId: String): Unit =
        withContext(Dispatchers.IO) {
            try {
                val response = treasureBackendApi.getGameDetails(dealId)

                if (response.isSuccessful && response.body() != null) {
                    val mappedEntity = response.body()!!.toEntity(DealCategory.SEARCH, 0)

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

                    if (rowsUpdated == 0) {
                        dealDao.insertDeals(listOf(mappedEntity))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching game details for ID: $dealId", e)
            }
        }

    /**
     * Searches catalog deals matching query text keywords.
     *
     * @param query Search query text submitted by the user.
     * @return List of matching [GameCardItem] representations.
     */
    override suspend fun searchGames(query: String): List<GameCardItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = treasureBackendApi.searchGames(query)

                if (response.isSuccessful && response.body() != null) {
                    return@withContext response.body()!!.map { it.toGameCardItem() }
                } else {
                    return@withContext emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching games: $query", e)
                return@withContext emptyList()
            }
        }

    /**
     * Observes all items currently placed in the user's shopping cart.
     *
     * @return Cold [Flow] emitting the list of cart [UserInteractionEntity] records.
     */
    override fun getCartItems(): Flow<List<UserInteractionEntity>> =
        userInteractionDao.getCartItems()

    /**
     * Observes all titles marked as favorites or wishlisted.
     *
     * @return Cold [Flow] emitting the list of wishlisted [UserInteractionEntity] records.
     */
    override fun getWishlistItems(): Flow<List<UserInteractionEntity>> =
        userInteractionDao.getFavorites()

    /**
     * Observes all favorited game IDs for fast O(1) membership checks in feed composables.
     *
     * @return Cold [Flow] emitting the list of active favorite game ID strings.
     */
    override fun observeInteractionIds(): Flow<List<String>> =
        userInteractionDao.getAllInteractedIds()

    /**
     * Optimistically toggles the favorited state of a game locally, followed by background cloud synchronization.
     *
     * Mutates the local Room record inside an atomic transaction, then dispatches a network sync call to the backend.
     *
     * @param game The target [GameCardItem] being toggled.
     */
    override suspend fun toggleFavorite(game: GameCardItem) {
        db.withTransaction {
            val currentInteraction = userInteractionDao.getInteractionForGame(game.id)
            val isCurrentlyFav = currentInteraction?.isFavorite ?: false
            val isInCart = currentInteraction?.isAddedToCart ?: false

            val newInteraction = UserInteractionEntity(
                gameId = game.id,
                isFavorite = !isCurrentlyFav,
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

        try {
            val response = treasureBackendApi.toggleWishlist(game.id)
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to sync wishlist to cloud: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception syncing wishlist to cloud", e)
        }
    }

    /**
     * Asynchronously synchronizes the authenticated user's cloud wishlist into local Room persistence.
     *
     * Executes in an independent scope to ensure in-flight sync survives calling ViewModel teardowns.
     */
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun syncWishlistFromCloud() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = treasureBackendApi.getDetailedWishlist()

                if (response.isSuccessful && response.body() != null) {
                    val fullGames = response.body()!!

                    db.withTransaction {
                        userInteractionDao.clearAllFavorites()

                        fullGames.forEach { dto ->
                            val existing = userInteractionDao.getInteractionForGame(dto.id)

                            val newInteraction = UserInteractionEntity(
                                gameId = dto.id,
                                isFavorite = true,
                                isAddedToCart = existing?.isAddedToCart ?: false,
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
                Log.e(TAG, "Failed to sync detailed wishlist from cloud", e)
            }
        }
    }

    /**
     * Toggles the presence of a game within the user's shopping cart.
     *
     * Executes an atomic database transaction updating the cart flag on [UserInteractionEntity].
     *
     * @param game The target [GameCardItem] being added or removed from the cart.
     */
    override suspend fun toggleCart(game: GameCardItem) {
        db.withTransaction {
            val currentInteraction = userInteractionDao.getInteractionForGame(game.id)
            val isCurrentlyFav = currentInteraction?.isFavorite ?: false
            val isInCart = currentInteraction?.isAddedToCart ?: false

            val newInteraction = UserInteractionEntity(
                gameId = game.id,
                isFavorite = isCurrentlyFav,
                isAddedToCart = !isInCart,
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

    /**
     * Maps a remote [GameDto] directly into a UI [GameCardItem] model.
     *
     * @return Initialized [GameCardItem] representation.
     */
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
            genres = this.genres ?: emptyList(),
            releaseDate = this.expectedReleaseDate
        )
    }

    /**
     * Safely parses review score color strings into strongly-typed [ColorCode] enums with a fallback to YELLOW.
     *
     * @param colorString Color name received from remote API.
     * @return Corresponding [ColorCode] constant.
     */
    private fun safeColorCode(colorString: String?): ColorCode {
        return try {
            if (colorString != null) ColorCode.valueOf(colorString.uppercase()) else ColorCode.YELLOW
        } catch (e: IllegalArgumentException) {
            ColorCode.YELLOW
        }
    }
}
