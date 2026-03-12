package com.example.treasure.ui.viewModels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.ui.navigationGraphs.LeafDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val repository: GameRepository
) : ViewModel() {

    private val args = savedStateHandle.toRoute<LeafDestination.Detail>()
    val gameId = args.itemId

    val gameDetail: StateFlow<DealEntity?> = repository.observeGameDetails(gameId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        fetchGameDetails()
    }

    private fun fetchGameDetails() {
        viewModelScope.launch {
            repository.fetchAndEnrichGameDetails(gameId)
        }
    }

    fun toggleFavorite(deal: DealEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(deal.toGameCardItem())
        }
    }

    private fun DealEntity.toGameCardItem() = GameCardItem(
        id = id,
        listingIndex = 0,
        title = title,
        thumbnail = thumbnail,
        store = storeId,
        price = Price(originalPrice = originalPrice, currentPrice = currentPrice),
        upVotes = upVotes as UpVotes?
    )
}