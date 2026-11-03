package com.example.treasure.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing the user's saved wishlist games and favorites collection.
 *
 * Exposes a reactive Room stream of wishlist entries and provides actions to toggle favorite
 * and cart states, coordinating local persistence with cloud synchronization.
 *
 * @property repository Game data repository managing wishlist storage and synchronization.
 */
@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    /**
     * Hot observable stream emitting the collection of [UserInteractionEntity] records saved to the wishlist.
     */
    val wishlistItems: StateFlow<List<UserInteractionEntity>> = repository.getWishlistItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Toggles the favorite status of a wishlist game entry in Room and cloud.
     *
     * @param entity The [UserInteractionEntity] to toggle.
     */
    fun toggleFavorite(entity: UserInteractionEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(entity.toGameCardItem())
        }
    }

    /**
     * Toggles the shopping cart status of a wishlist game entry in Room.
     *
     * @param entity The [UserInteractionEntity] to toggle.
     */
    fun toggleCart(entity: UserInteractionEntity) {
        viewModelScope.launch {
            repository.toggleCart(entity.toGameCardItem())
        }
    }

    private fun UserInteractionEntity.toGameCardItem() = GameCardItem(
        id = gameId,
        listingIndex = 0,
        title = title,
        thumbnail = thumbnail,
        store = storeId,
        price = Price(originalPrice = originalPrice, currentPrice = currentPrice),
        upVotes = null
    )
}
