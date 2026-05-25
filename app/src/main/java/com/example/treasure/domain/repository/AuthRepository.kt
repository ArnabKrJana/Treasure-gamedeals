package com.example.treasure.domain.repository

import com.example.treasure.domain.uiModels.User

interface AuthRepository {
    suspend fun loginWithGoogle(idToken: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}