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

/**
 * ViewModel orchestrating authentication workflows, user credential exchanges, session lifecycles,
 * and account deletion.
 *
 * Coordinates between Android Credential Manager ([GoogleAuthHelper]), backend JWT validation
 * ([LoginWithGoogleUseCase]), encrypted token management ([TokenManager]), and user wishlist
 * synchronization upon successful sign-in.
 *
 * @property loginWithGoogleUseCase Use case exchanging Google OAuth ID tokens for server JWTs.
 * @property googleAuthHelper Helper abstracting Google Credential Manager interactions.
 * @property tokenManager Encrypted persistence manager tracking JWTs and session activity.
 * @property authRepository Repository executing logout and server-side account deletion.
 * @property gameRepository Repository managing cloud synchronization of user interactions.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val googleAuthHelper: GoogleAuthHelper,
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    /**
     * Observable stream indicating whether an active, valid authentication session currently exists.
     */
    val isSessionActive: StateFlow<Boolean> = tokenManager.isSessionActive

    private val _isLoading = MutableStateFlow(false)

    /**
     * Observable stream emitting true while an asynchronous authentication or account action is in flight.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)

    /**
     * Observable stream emitting human-readable error messages when an authentication operation fails,
     * or null if operation succeeded.
     */
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Initiates the Google Sign-In flow, sends the resulting ID token to the backend,
     * syncs cloud wishlist data upon success, and invokes [onLoginSuccess].
     *
     * @param context The Android context used to launch the Google Credential Manager dialog.
     * @param onLoginSuccess Callback triggered once authentication and cloud synchronization succeed.
     */
    fun handleGoogleLogin(context: Context, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val idToken = googleAuthHelper.signIn(context)

            if (idToken != null) {
                val result = loginWithGoogleUseCase(idToken)

                result.onSuccess {
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

    /**
     * Requests permanent server-side deletion of the authenticated user's account and personal cloud data.
     * Automatically invalidates local encrypted tokens and active session state upon success.
     */
    fun deleteMyAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authRepository.deleteAccount()

            result.onFailure { exception ->
                _error.value = exception.message ?: "Failed to delete account."
            }
            _isLoading.value = false
        }
    }

    /**
     * Clears all encrypted user credentials, tokens, and active session state locally and on the server.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
