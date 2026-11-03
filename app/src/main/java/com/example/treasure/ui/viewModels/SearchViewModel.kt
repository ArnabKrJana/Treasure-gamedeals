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

/**
 * ViewModel managing the game catalog search experience, live query inputs, and asynchronous search results.
 *
 * Implements explicit search triggering upon submission to prevent unnecessary remote API calls during typing.
 *
 * @property repository Game data repository executing remote and local search queries.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    /**
     * Observable stream representing the current text input entered into the search field.
     */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GameCardItem>>(emptyList())

    /**
     * Observable stream emitting the list of matched [GameCardItem] results from the latest search.
     */
    val searchResults: StateFlow<List<GameCardItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)

    /**
     * Observable stream indicating whether an asynchronous search query is currently executing.
     */
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /**
     * Updates the current search input text without dispatching a network query.
     *
     * @param newQuery The updated search query text.
     */
    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /**
     * Dispatches a remote search query to the repository for the current trimmed search string.
     * Clears results if the query is blank or an error occurs during execution.
     */
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
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}
