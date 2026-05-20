package com.example.treasure.domain.usecases

import com.example.treasure.domain.repository.AuthRepository
import com.example.treasure.domain.uiModels.User
import javax.inject.Inject

class LoginWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        if (idToken.isBlank()) return Result.failure(IllegalArgumentException("Token cannot be blank"))
        return repository.loginWithGoogle(idToken)
    }
}