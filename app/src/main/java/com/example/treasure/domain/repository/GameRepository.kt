package com.example.treasure.domain.repository

import androidx.paging.PagingData
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.domain.uiModels.GameCardItem
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract defining game catalog discovery, paginated deal streams,
 * detail enrichment, catalog search, and user interactions (favorites and cart).
 *
 * Implements an offline-first architecture where the local Room database acts as the single
 * source of truth, synchronized asynchronously with remote backend APIs via Paging 3 and coroutines.
 */
interface GameRepository {

    /**
     * Streams paginated game deals for a specific deal category utilizing AndroidX Paging 3.
     *
     * Backed by a [androidx.paging.RemoteMediator] that fetches pages from the backend and persists
     * them into the local database, emitting reactive updates directly from Room.
     *
     * @param category The [DealCategory] partition to retrieve (e.g., ALL, MAC, LINUX).
     * @return Cold [Flow] emitting reactive [PagingData] batches containing cached [DealEntity] items.
     */
    fun getDealsPaged(category: DealCategory): Flow<PagingData<DealEntity>>

    /**
     * Retrieves the curated list of highly anticipated upcoming games.
     *
     * @return List of [GameCardItem] representations suitable for display in feed carousels.
     */
    suspend fun getAnticipatedGames(): List<GameCardItem>

    /**
     * Observes detailed deal information for a specific game deal from the local database.
     *
     * @param dealId Unique identifier of the game deal.
     * @return Cold [Flow] emitting the cached [DealEntity], or `null` if not found in local persistence.
     */
    fun observeGameDetails(dealId: String): Flow<DealEntity?>

    /**
     * Fetches comprehensive game metadata (screenshots, system requirements, store deals) from remote
     * APIs and enriches the locally cached database entity.
     *
     * @param dealId Unique identifier of the game deal to enrich.
     */
    suspend fun fetchAndEnrichGameDetails(dealId: String)

    /**
     * Searches the game deal catalog by query keywords.
     *
     * @param query Search query text submitted by the user.
     * @return List of matching [GameCardItem] search results.
     */
    suspend fun searchGames(query: String): List<GameCardItem>

    /**
     * Synchronizes the authenticated user's wishlist from the cloud backend into local persistence.
     *
     * Merges remote cloud entries with local database records to support offline access and periodic
     * price drop monitoring via background workers.
     */
    suspend fun syncWishlistFromCloud()

    /**
     * Observes the user's active shopping cart items stored locally.
     *
     * @return Cold [Flow] emitting the current list of cart [UserInteractionEntity] items.
     */
    fun getCartItems(): Flow<List<UserInteractionEntity>>

    /**
     * Observes the user's wishlisted games stored locally.
     *
     * @return Cold [Flow] emitting the current list of wishlisted [UserInteractionEntity] items.
     */
    fun getWishlistItems(): Flow<List<UserInteractionEntity>>

    /**
     * Observes a list of game IDs representing active user interactions (favorited or wishlisted items).
     *
     * Used by catalog list items and cards for O(1) membership checks to reflect favorite states in the UI.
     *
     * @return Cold [Flow] emitting the list of interacted game ID strings.
     */
    fun observeInteractionIds(): Flow<List<String>>

    /**
     * Toggles the favorited/wishlisted state of a game.
     *
     * Persists or removes the item from the local database and enqueues cloud synchronization.
     *
     * @param game The [GameCardItem] target for favorite toggling.
     */
    suspend fun toggleFavorite(game: GameCardItem)

    /**
     * Toggles the presence of a game within the user's shopping cart.
     *
     * Updates the local database and reflects immediately in cart flows.
     *
     * @param game The [GameCardItem] target for cart toggling.
     */
    suspend fun toggleCart(game: GameCardItem)
}
