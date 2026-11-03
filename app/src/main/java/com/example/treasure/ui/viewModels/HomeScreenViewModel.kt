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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel powering the main Home screen feed with categorized, paged deal streams and user interaction sidecars.
 *
 * Exposes paged reactive streams for Hot Deals, Lowest Price Deals, Mac Deals, and Linux Deals,
 * alongside anticipated carousel games and real-time Room observation sets for wishlist and cart status.
 *
 * @property repository Game data repository providing Room caching and Paging 3 flows.
 */
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    /**
     * Paged reactive stream of hot game deals cached across configuration changes.
     */
    val hotDeals: Flow<PagingData<GameCardItem>> = repository
        .getDealsPaged(DealCategory.HOT_DEALS)
        .map { pagingData -> pagingData.map { entity -> entity.toGameCardItem() } }
        .cachedIn(viewModelScope)

    /**
     * Paged reactive stream of lowest-price game deals cached across configuration changes.
     */
    val lowestPriceDeals: Flow<PagingData<GameCardItem>> = repository
        .getDealsPaged(DealCategory.LOWEST_PRICE)
        .map { pagingData -> pagingData.map { entity -> entity.toGameCardItem() } }
        .cachedIn(viewModelScope)

    /**
     * Paged reactive stream of macOS-compatible game deals.
     */
    val macDeals: Flow<PagingData<GameCardItem>> = repository
        .getDealsPaged(DealCategory.MAC_DEALS)
        .map { pagingData -> pagingData.map { entity -> entity.toGameCardItem() } }
        .cachedIn(viewModelScope)

    /**
     * Paged reactive stream of Linux/SteamOS-compatible game deals.
     */
    val linuxDeals: Flow<PagingData<GameCardItem>> = repository
        .getDealsPaged(DealCategory.LINUX_DEALS)
        .map { pagingData -> pagingData.map { entity -> entity.toGameCardItem() } }
        .cachedIn(viewModelScope)

    private val _anticipatedGames = MutableStateFlow<List<GameCardItem>>(emptyList())

    /**
     * High-visibility anticipated games showcase featured in the top hero carousel.
     */
    val anticipatedGames: StateFlow<List<GameCardItem>> = _anticipatedGames.asStateFlow()

    init {
        fetchAnticipatedGames()
    }

    /**
     * Loads the featured anticipated games from repository cache/network.
     */
    private fun fetchAnticipatedGames() {
        viewModelScope.launch {
            val games = repository.getAnticipatedGames()
            _anticipatedGames.value = games
        }
    }

    /**
     * Real-time set of game IDs currently saved in the user's local/cloud wishlist.
     */
    val favoriteIds: StateFlow<Set<String>> = repository.getWishlistItems()
        .map { list -> list.map { it.gameId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /**
     * Real-time set of game IDs currently marked in the user's shopping cart.
     */
    val cartIds: StateFlow<Set<String>> = repository.getCartItems()
        .map { list -> list.map { it.gameId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /**
     * Toggles the wishlist status of a game deal in Room.
     *
     * @param game The target [GameCardItem].
     */
    fun toggleFavorite(game: GameCardItem) {
        viewModelScope.launch { repository.toggleFavorite(game) }
    }

    /**
     * Toggles the shopping cart status of a game deal in Room.
     *
     * @param game The target [GameCardItem].
     */
    fun toggleCart(game: GameCardItem) {
        viewModelScope.launch { repository.toggleCart(game) }
    }

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
        } catch (_: IllegalArgumentException) {
            ColorCode.YELLOW
        }
    }
}
