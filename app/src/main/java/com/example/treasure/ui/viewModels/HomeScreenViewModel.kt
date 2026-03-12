package com.example.treasure.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.utils.ColorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    // --- 1. HOT DEALS STREAM ---
    val hotDeals: Flow<PagingData<GameCardItem>> = repository
        .getDealsPaged(DealCategory.HOT_DEALS)
        .map { pagingData -> pagingData.map { entity -> entity.toGameCardItem() } }
        .cachedIn(viewModelScope)

    // --- 2. LOWEST PRICE STREAM ---
    val lowestPriceDeals: Flow<PagingData<GameCardItem>> = repository
        .getDealsPaged(DealCategory.LOWEST_PRICE)
        .map { pagingData -> pagingData.map { entity -> entity.toGameCardItem() } }
        .cachedIn(viewModelScope)

    // --- 3. USER INTERACTION STATE (The "Sidecar" Data) ---
    // We observe the database and convert the List<Entity> into a Set<String> of IDs.
    // This allows the UI to quickly check: "if (id in favoritesIds) { showRedHeart }"

    val favoriteIds: StateFlow<Set<String>> = repository.getWishlistItems()
        .map { list -> list.map { it.gameId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val cartIds: StateFlow<Set<String>> = repository.getCartItems()
        .map { list -> list.map { it.gameId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // --- ACTIONS ---
    fun toggleFavorite(game: GameCardItem) {
        viewModelScope.launch { repository.toggleFavorite(game) }
    }

    fun toggleCart(game: GameCardItem) {
        viewModelScope.launch { repository.toggleCart(game) }
    }

    // --- HELPER: MAPPER ---
    private fun DealEntity.toGameCardItem(): GameCardItem {
        return GameCardItem(
            id = this.id,
            listingIndex = this.listingIndex,
            title = this.title,
            thumbnail = this.thumbnail,
            store = this.storeId,
            price = Price(this.originalPrice, this.currentPrice),
            upVotes = if (this.upVotes != null) {
                UpVotes(this.upVotes, safeColorCode(this.upVoteColor))
            } else null,
            genres = emptyList()
        )
    }

    private fun safeColorCode(colorString: String?): ColorCode {
        return try {
            if (colorString != null) ColorCode.valueOf(colorString) else ColorCode.YELLOW
        } catch (e: IllegalArgumentException) {
            ColorCode.YELLOW
        }
    }
}