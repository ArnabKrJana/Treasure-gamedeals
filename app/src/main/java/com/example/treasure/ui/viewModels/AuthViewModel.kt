package com.example.treasure.ui.viewModels


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.treasure.data.remote.auth.googleAuthHelper.GoogleAuthHelper
import com.example.treasure.domain.usecases.LoginWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val googleAuthHelper: GoogleAuthHelper // <-- Helper injected here
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun handleGoogleLogin(context: Context, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // 1. Get the ID Token from the Native Bridge
            val idToken = googleAuthHelper.signIn(context)

            if (idToken != null) {
                // 2. Send the token to the backend
                val result = loginWithGoogleUseCase(idToken)

                result.onSuccess { user ->
                    onLoginSuccess()
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Authentication with server failed."
                }
            } else {
                // The user dismissed the sheet or there was a Google Services error
                _error.value = "Google Sign-In was cancelled or failed."
            }

            _isLoading.value = false
        }
    }
}