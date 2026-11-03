package com.example.treasure.ui.viewModels

import androidx.lifecycle.ViewModel
import com.example.treasure.domain.uiModels.User
import com.example.treasure.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Top-level application ViewModel managing global user profile and session identity state.
 *
 * Provides root-level access to the authenticated [User] entity retrieved from encrypted storage.
 *
 * @property tokenManager Encrypted credentials manager providing the cached authenticated [User].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(tokenManager.getUser())

    /**
     * Observable stream emitting the currently authenticated user profile, or null if unauthenticated.
     */
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
}
