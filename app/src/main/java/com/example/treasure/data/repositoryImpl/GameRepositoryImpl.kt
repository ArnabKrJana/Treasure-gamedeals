package com.example.treasure.data.repositoryImpl

import android.os.Build
import android.text.Html
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.SystemRequirementEntity
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.data.local.remoteMediators.DealRemoteMediator
import com.example.treasure.data.remote.apiService.ItadApi
import com.example.treasure.data.remote.apiService.SteamApi
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.RequirementType
import com.example.treasure.domain.uiModels.StoreDeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val itadApi: ItadApi,
    private val steamApi: SteamApi,
    private val db: TreasureDatabase
) : GameRepository {

    private val dealDao = db.dealDao()

    @OptIn(ExperimentalPagingApi::class)
    override fun getDealsPaged(category: DealCategory): Flow<PagingData<DealEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                enablePlaceholders = true,
                prefetchDistance = 12
            ),
            remoteMediator = DealRemoteMediator(db, itadApi, category),
            pagingSourceFactory = {
                dealDao.getDealsByCategory(category)
            }
        ).flow
    }

    override fun observeGameDetails(dealId: String): Flow<DealEntity?> {
        return dealDao.observeDealById(dealId)
    }

    override suspend fun fetchAndEnrichGameDetails(dealId: String) = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch Metadata (ITAD) to get Steam App ID & Basic Info
            val infoResponse = itadApi.getInfo(
                country = "IN",
                gameId = dealId
            )

            if (!infoResponse.isSuccessful) return@withContext

            val steamAppId = infoResponse.body()?.steamAppId

            // 2. Parallel Execution: Fetch Steam Details & Price Overview
            val steamJob = async {
                if (steamAppId != null) {
                    steamApi.getExtraData(appId = steamAppId)
                } else null
            }

            val overviewJob = async {
                itadApi.getPriceOverview(

                    country = "IN",
                    shops = "61,35,16", // Steam, GOG, Epic
                    gameIds = listOf(dealId)
                )
            }

            val steamResponse = steamJob.await()
            val overviewResponse = overviewJob.await()

            // 3. Parse Steam Data
            var description: String? = null
            var screenshots: List<String>? = null
            var sysReqs: List<SystemRequirementEntity>? = null
            var releaseDate: String? = null
            var publisher: String? = null
            var developer: String? = null
            var genres: List<String>? = null
            var maturityRating: String? = null
            var trailerUrl: String? = null

            if (steamResponse?.isSuccessful == true) {
                val appData = steamResponse.body()?.values?.firstOrNull()?.data

                if (appData != null) {
                    description = appData.shortDescription
                    screenshots = appData.screenshots?.map { it.full }
                    releaseDate = appData.releaseDate?.date
                    publisher = appData.publishers?.firstOrNull()
                    developer = appData.developers?.firstOrNull()
                    genres = appData.genres?.map { it.description }
                    maturityRating = appData.requiredAge?.toString()

                    // Extract Trailer URL with Priorities:
                    // 1. HLS (Streaming - .m3u8) -> ExoPlayer handles this well
                    // 2. DASH
                    // 3. MP4 HD (Best compatibility)
                    // 4. MP4 SD
                    val firstMovie = appData.movies?.firstOrNull()

                    trailerUrl = firstMovie?.hlsUrl      // <--- Priority 1
                        ?: firstMovie?.dashUrl           // <--- Priority 2
                                ?: firstMovie?.mp4?.hd
                                ?: firstMovie?.mp4?.sd
                                ?: firstMovie?.webm?.hd
                                ?: firstMovie?.webm?.sd

                    // Parse Requirements
                    sysReqs = parseRequirements(
                        appData.pcRequirements?.minimum,
                        appData.pcRequirements?.recommended
                    )
                }
            }

            // 4. Parse Price Overview
            var otherDeals: List<StoreDeal>? = null

            if (overviewResponse.isSuccessful) {
                val overviewItem = overviewResponse.body()?.prices?.find { it.id == dealId }

                if (overviewItem?.current != null) {
                    val currentDeal = StoreDeal(
                        storeName = overviewItem.current.shop.name,
                        dealUrl = overviewItem.current.url,
                        price = Price(
                            originalPrice = overviewItem.current.regular.amount,
                            currentPrice = overviewItem.current.price.amount
                        )
                    )
                    otherDeals = listOf(currentDeal)
                }
            }

            // 5. Database Operation (The Critical Fix)
            // Attempt to update the existing record
            val rowsUpdated = dealDao.updateDealDetails(
                dealId = dealId,
                description = description,
                screenshots = screenshots,
                otherDeals = otherDeals,
                developer = developer,
                publisher = publisher,
                franchise = null,
                releaseDate = releaseDate,
                maturity = maturityRating,
                sysReqs = sysReqs,
                trailerUrl = trailerUrl
            )

            // IF update returns 0, it means the game is NOT in the DB (Search Result).
            // We must INSERT it now so the UI can display it.
            if (rowsUpdated == 0) {
                val basicInfo = infoResponse.body()
                if (basicInfo != null) {
                    val newEntity = DealEntity(
                        id = dealId,
                        listingIndex = 0, // Not part of the main ranked list
                        title = basicInfo.title,
                        thumbnail = basicInfo.assets?.bannerUrl ?: basicInfo.assets?.boxArt,
                        storeId = "steam", // Fallback store
                        originalPrice = otherDeals?.firstOrNull()?.price?.originalPrice ?: 0.0,
                        currentPrice = otherDeals?.firstOrNull()?.price?.currentPrice ?: 0.0,
                        discountPercent = 0,
                        upVotes = null,
                        upVoteColor = null,

                        // FIX: Use SEARCH category to prevent duplicate key crashes on Home Screen
                        category = DealCategory.SEARCH,

                        // Enriched fields
                        description = description,
                        screenshots = screenshots,
                        trailerUrl = trailerUrl,
                        otherStores = otherDeals,
                        developer = developer,
                        publisher = publisher,
                        franchise = null,
                        releaseDate = releaseDate,
                        maturityRating = maturityRating,
                        systemRequirements = sysReqs,
                        genres = genres
                    )
                    dealDao.insertDeals(listOf(newEntity))
                }
            }

        } catch (e: Exception) {
            Log.e("GameRepository", "Error updating game details for ID: $dealId", e)
        }
    }

    override suspend fun searchGames(query: String): List<GameCardItem> = withContext(Dispatchers.IO) {
        try {
            val response = itadApi.searchGames(
                query = query
            )

            if (response.isSuccessful && response.body() != null) {
                // Map the DTO to your existing UI Model
                return@withContext response.body()!!.map { item ->
                    GameCardItem(
                        id = item.id,
                        listingIndex = 0,
                        title = item.title,
                        thumbnail = item.assets?.bannerUrl ?: item.assets?.boxArt,
                        store = "Multiple",
                        price = null,
                        upVotes = null
                    )
                }
            } else {
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Error searching games: $query", e)
            return@withContext emptyList()
        }
    }

    // --- PARSER FOR STEAM HTML ---
    private fun parseRequirements(minHtml: String?, recHtml: String?): List<SystemRequirementEntity> {
        val list = mutableListOf<SystemRequirementEntity>()

        if (!minHtml.isNullOrBlank()) {
            list.addAll(extractSpecs(minHtml, RequirementType.MINIMUM))
        }

        if (!recHtml.isNullOrBlank()) {
            list.addAll(extractSpecs(recHtml, RequirementType.MAXIMUM))
        }
        return list
    }

    private fun extractSpecs(html: String, type: RequirementType): List<SystemRequirementEntity> {
        val specs = mutableListOf<SystemRequirementEntity>()

        // Steam requirements are usually in <li> tags.
        val pattern = Regex("<strong>(.*?):?</strong>\\s*(.*?)(?:<br>|</li>|$)", RegexOption.IGNORE_CASE)

        val matches = pattern.findAll(html)

        matches.forEach { matchResult ->
            val (key, value) = matchResult.destructured
            val cleanKey = stripHtml(key).replace("*", "").trim().removeSuffix(":")
            val cleanValue = stripHtml(value).trim()

            if (cleanKey.isNotBlank() && cleanValue.isNotBlank()) {
                specs.add(SystemRequirementEntity(cleanKey, cleanValue, type))
            }
        }

        // Fallback for plain text formats
        if (specs.isEmpty() && html.isNotBlank()) {
            val cleanHtml = stripHtml(html).trim()
            if (cleanHtml.isNotEmpty()) {
                specs.add(SystemRequirementEntity("Notes", cleanHtml, type))
            }
        }

        return specs
    }

    private fun stripHtml(html: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            Html.fromHtml(html).toString()
        }
    }

    // --- NEW IMPLEMENTATION (Corrected for KSP Error) ---

    private val userInteractionDao = db.userInteractionDao()

    override fun getCartItems(): Flow<List<UserInteractionEntity>> {
        return userInteractionDao.getCartItems()
    }

    override fun getWishlistItems(): Flow<List<UserInteractionEntity>> {
        return userInteractionDao.getFavorites()
    }

    override fun observeInteractionIds(): Flow<List<String>> {
        return userInteractionDao.getAllInteractedIds()
    }

    override suspend fun toggleFavorite(game: GameCardItem) {
        // We use db.withTransaction to ensure thread safety (Atomic operation)
        // This replaces the @Transaction method in DAO to fix the KSP error
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
                // ADD THIS LINE TO PRESERVE SYNC DATA:
                latestSyncedPrice = currentInteraction?.latestSyncedPrice,
                lastSyncTimestamp = currentInteraction?.lastSyncTimestamp
            )


            userInteractionDao.insertInteraction(newInteraction)
        }
    }

    override suspend fun toggleCart(game: GameCardItem) {
        // We use db.withTransaction to ensure thread safety (Atomic operation)
        // This replaces the @Transaction method in DAO to fix the KSP error
        db.withTransaction {
            val currentInteraction = userInteractionDao.getInteractionForGame(game.id)
            val isCurrentlyFav = currentInteraction?.isFavorite ?: false
            val isInCart = currentInteraction?.isAddedToCart ?: false

            // Inside toggleFavorite
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
                // ADD THIS LINE TO PRESERVE SYNC DATA:
                latestSyncedPrice = currentInteraction?.latestSyncedPrice,
                lastSyncTimestamp = currentInteraction?.lastSyncTimestamp
            )

// Repeat the same logic for toggleCart
            userInteractionDao.insertInteraction(newInteraction)
        }
    }
}