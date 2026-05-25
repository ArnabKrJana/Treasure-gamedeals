package com.example.treasure.ui.viewModels


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.treasure.data.remote.auth.googleAuthHelper.GoogleAuthHelper
import com.example.treasure.domain.repository.AuthRepository
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.usecases.LoginWithGoogleUseCase
import com.example.treasure.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val googleAuthHelper: GoogleAuthHelper,
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    val isSessionActive: StateFlow<Boolean> = tokenManager.isSessionActive
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun handleGoogleLogin(context: Context, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val idToken = googleAuthHelper.signIn(context)

            if (idToken != null) {
                val result = loginWithGoogleUseCase(idToken)

                result.onSuccess { user ->
                    // 2. TRIGGER THE SYNC BEFORE NAVIGATING TO HOME!
                    gameRepository.syncWishlistFromCloud()

                    onLoginSuccess()
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Authentication with server failed."
                }
            } else {
                _error.value = "Google Sign-In was cancelled or failed."
            }

            _isLoading.value = false
        }
    }

    // ... Keep deleteMyAccount() and logout() exactly as they are ...

    // Add the Delete Account function for your Settings Screen
    fun deleteMyAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // This calls the repo, hits the backend, and if successful,
            // calls tokenManager.clearSession() automatically!
            val result = authRepository.deleteAccount()

            result.onFailure { exception ->
                _error.value = exception.message ?: "Failed to delete account."
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout() // Also calls clearSession()
        }
    }
}