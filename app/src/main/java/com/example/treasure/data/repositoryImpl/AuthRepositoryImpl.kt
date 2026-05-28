package com.example.treasure.data.repositoryImpl

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.remote.dto.GoogleLoginRequest
import com.example.treasure.data.remote.dto.RefreshTokenRequest
import com.example.treasure.data.remote.dto.UserDto
import com.example.treasure.domain.repository.AuthRepository
import com.example.treasure.domain.uiModels.User
import com.example.treasure.utils.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val treasureBackendApi: TreasureBackendApi,
    private val tokenManager: TokenManager,
    private val db: TreasureDatabase,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val request = GoogleLoginRequest(idToken = idToken)
                val response = treasureBackendApi.login("google", request)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!

                    // 1. Securely save the tokens
                    tokenManager.saveTokens(
                        accessToken = authResponse.accessToken,
                        refreshToken = authResponse.refreshToken
                    )

                    // 2. Map backend DTO to Android UI Model
                    val domainUser = authResponse.user.toDomainModel()

                    // 3. Save the serialized user data locally
                    tokenManager.saveUser(domainUser)

                    // RESTORE THE WISHLIST FROM SPRING BOOT!
                    try {
                        // 1. We MUST call the detailed endpoint to satisfy the non-nullable Room Entity
                        val wishlistResponse = treasureBackendApi.getDetailedWishlist()

                        if (wishlistResponse.isSuccessful) {
                            val savedGames = wishlistResponse.body() ?: emptyList()

                            savedGames.forEach { gameDto ->
                                // Use your DTO's actual ID variable (e.g., gameDto.id or gameDto.gameId)
                                val targetGameId = gameDto.id

                                val existingInteraction =
                                    db.userInteractionDao().getInteractionForGame(targetGameId)

                                // 2. If it exists, flip 'isFavorite' to true. If not, create a new snapshot entity!
                                val updatedEntity = existingInteraction?.copy(isFavorite = true)
                                    ?: UserInteractionEntity(
                                        gameId = targetGameId,
                                        isFavorite = true,
                                        isAddedToCart = false,
                                        timestamp = System.currentTimeMillis(),
                                        title = gameDto.title,
                                        thumbnail = gameDto.thumbnail,
                                        currentPrice = gameDto.currentPrice ?: 0.0,
                                        originalPrice = gameDto.originalPrice ?: 0.0,
                                        storeId = gameDto.primaryStore ?: "Steam",
                                        latestSyncedPrice = gameDto.currentPrice,
                                        lastSyncTimestamp = System.currentTimeMillis()
                                    )

                                // 3. Save it to Room
                                db.userInteractionDao().insertInteraction(updatedEntity)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthRepo", "Failed to restore wishlist on login", e)
                    }
                    Result.success(domainUser)
                } else {
                    Log.e(
                        "AuthRepo",
                        "Login failed: ${response.code()} ${response.errorBody()?.string()}"
                    )
                    Result.failure(Exception("Login failed. Please try again."))
                }
            } catch (e: Exception) {
                Log.e("AuthRepo", "Exception during login", e)
                Result.failure(e)
            }
        }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val refreshToken = tokenManager.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                val request = RefreshTokenRequest(refreshToken)
                // Attempt to notify the backend to invalidate the session/token
                treasureBackendApi.logout(request)
            }
        } catch (e: Exception) {
            Log.e(
                "AuthRepo",
                "Error notifying backend of logout, proceeding to clear local session",
                e
            )
        } finally {
            // ALWAYS clear the local session regardless of network success
            tokenManager.clearSession()

            //  Wipe local database and stop background syncs!
            try {
                db.clearAllTables()
                WorkManager.getInstance(context).cancelAllWork()
            } catch (e: Exception) {
                Log.e("AuthRepo", "Error wiping local database on logout", e)
            }
        }
        Result.success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = treasureBackendApi.deleteMyAccount()
            if (response.isSuccessful) {
                tokenManager.clearSession()

                //  Wipe data here as well so the deleted user's data doesn't linger!
                try {
                    db.clearAllTables()
                    WorkManager.getInstance(context).cancelAllWork()
                } catch (e: Exception) {
                    Log.e("AuthRepo", "Error wiping local database on account deletion", e)
                }

                Result.success(Unit)
            } else {
                Log.e("AuthRepo", "Account deletion failed: ${response.code()}")
                Result.failure(Exception("Failed to delete account. Please contact support."))
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Exception during account deletion", e)
            Result.failure(e)
        }
    }

    // --- MAPPERS ---

    private fun UserDto.toDomainModel(): User {
        return User(
            id = this.id,
            email = this.email,
            fullName = this.fullName,
            profilePicture = this.profilePicture,
            // Maps the Spring Boot enum to a string for the UI
            role = this.role.name
        )
    }
}