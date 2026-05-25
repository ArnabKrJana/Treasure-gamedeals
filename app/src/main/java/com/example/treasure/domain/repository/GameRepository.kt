package com.example.treasure.domain.repository

import androidx.paging.PagingData
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.domain.uiModels.GameCardItem
import kotlinx.coroutines.flow.Flow

interface GameRepository {

    fun getDealsPaged(category: DealCategory): Flow<PagingData<DealEntity>>

    suspend fun getAnticipatedGames(): List<GameCardItem>

    // Phase 2: Detail View (Lazy Loading)
    fun observeGameDetails(dealId: String): Flow<DealEntity?>
    suspend fun fetchAndEnrichGameDetails(dealId: String)

    suspend fun searchGames(query: String): List<GameCardItem>

    // --- NEW: User Interactions (Cart & Wishlist) ---
    suspend fun syncWishlistFromCloud(): Unit
    fun getCartItems(): Flow<List<UserInteractionEntity>>
    fun getWishlistItems(): Flow<List<UserInteractionEntity>>

    // Returns a Set of IDs for fast "Is Liked?" checks on the Home Screen
    fun observeInteractionIds(): Flow<List<String>>

    suspend fun toggleFavorite(game: GameCardItem)
    suspend fun toggleCart(game: GameCardItem)
}