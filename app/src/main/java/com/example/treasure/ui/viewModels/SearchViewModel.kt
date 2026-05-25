package com.example.treasure.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.uiModels.GameCardItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GameCardItem>>(emptyList())
    val searchResults: StateFlow<List<GameCardItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // 1. Just updates the text on the screen while typing (NO API CALL)
    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    // 2. Actually fires the API call (Triggered ONLY by a button click)
    fun performSearch() {
        val query = _searchQuery.value.trim()

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = repository.searchGames(query)
                _searchResults.value = results
            } catch (e: Exception) {
                // Handle error or show empty state
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}