package com.example.treasure.domain.usecases

import com.example.treasure.domain.repository.AuthRepository
import com.example.treasure.domain.uiModels.User
import javax.inject.Inject

/**
 * Domain use case encapsulating Google OAuth 2.0 authentication business validation and execution.
 *
 * Validates the caller-supplied ID token before delegating the authentication exchange
 * to [AuthRepository.loginWithGoogle].
 *
 * @param repository Injected [AuthRepository] managing remote authentication handshakes.
 */
class LoginWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    /**
     * Executes Google authentication validation and backend verification.
     *
     * @param idToken Google OAuth 2.0 ID token. Must not be blank.
     * @return [Result.success] containing the authenticated [User] profile, or [Result.failure]
     * with an [IllegalArgumentException] if the token is blank, or the underlying repository error.
     */
    suspend operator fun invoke(idToken: String): Result<User> {
        if (idToken.isBlank()) return Result.failure(IllegalArgumentException("Token cannot be blank"))
        return repository.loginWithGoogle(idToken)
    }
}
