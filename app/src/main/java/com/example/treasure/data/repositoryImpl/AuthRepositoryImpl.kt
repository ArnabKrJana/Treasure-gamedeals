package com.example.treasure.data.repositoryImpl


import com.example.treasure.domain.repository.AuthRepository
import com.example.treasure.domain.uiModels.User
import kotlinx.coroutines.delay
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    // private val authApi: AuthApi // <-- Inject your Retrofit API here later
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        return try {
            // TODO: Retrofit implementation later
            // val request = GoogleLoginRequest(idToken = idToken)
            // val response = authApi.login("google", request)
            // val userDto = response.user
            // return Result.success(userDto.toDomainModel())

            // MOCK DELAY FOR NOW to test UI
            delay(1500)
            Result.success(
                User(
                    id = 1L,
                    email = "gamer@treasure.com",
                    fullName = "Senua",
                    profilePicture = null,
                    role = "USER"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        // Implementation for calling /auth/logout
        return Result.success(Unit)
    }

//    override suspend fun deleteAccount(): Result<Unit> {
//        TODO("Not yet implemented")
//    }
}