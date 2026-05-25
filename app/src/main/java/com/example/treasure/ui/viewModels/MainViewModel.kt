package com.example.treasure.ui.viewModels



import androidx.lifecycle.ViewModel
import com.example.treasure.domain.uiModels.User
import com.example.treasure.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    // Fetch the user synchronously from EncryptedSharedPreferences
    private val _currentUser = MutableStateFlow<User?>(tokenManager.getUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

}