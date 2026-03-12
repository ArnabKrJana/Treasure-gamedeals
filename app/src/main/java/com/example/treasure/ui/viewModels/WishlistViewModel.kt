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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    val wishlistItems: StateFlow<List<UserInteractionEntity>> = repository.getWishlistItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleFavorite(entity: UserInteractionEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(entity.toGameCardItem())
        }
    }

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
