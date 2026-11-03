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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Concrete implementation of [AuthRepository] mediating user authentication with the backend service.
 *
 * Coordinates Google OAuth verification via [TreasureBackendApi], securely persists token pairs in [TokenManager],
 * restores the authenticated user's remote cloud wishlist into local Room persistence upon login, and executes
 * total local data purges (clearing Room tables and cancelling WorkManager jobs) upon logout or account deletion.
 *
 * @param treasureBackendApi Retrofit REST client for backend authentication and wishlist queries.
 * @param tokenManager Centralized session token and user profile preference store.
 * @param db Central [TreasureDatabase] instance providing local table access.
 * @param ioDispatcher Injected [CoroutineDispatcher] providing thread pool isolation for I/O operations.
 * @param context Application context used to cancel scheduled background [WorkManager] tasks.
 */
class AuthRepositoryImpl @Inject constructor(
    private val treasureBackendApi: TreasureBackendApi,
    private val tokenManager: TokenManager,
    private val db: TreasureDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    @param:ApplicationContext private val context: Context
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepo"
        private const val PROVIDER_GOOGLE = "google"
    }

    /**
     * Authenticates the user with the backend using a Google ID token.
     *
     * Saves received access and refresh tokens, maps and stores user profile details, and restores
     * the cloud wishlist into [UserInteractionEntity] records to prime offline access.
     *
     * @param idToken The Google OAuth ID token string.
     * @return [Result.success] containing the authenticated [User], or [Result.failure] on error.
     */
    override suspend fun loginWithGoogle(idToken: String): Result<User> =
        withContext(ioDispatcher) {
            try {
                val request = GoogleLoginRequest(idToken = idToken)
                val response = treasureBackendApi.login(PROVIDER_GOOGLE, request)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!

                    tokenManager.saveTokens(
                        accessToken = authResponse.accessToken,
                        refreshToken = authResponse.refreshToken
                    )

                    val domainUser = authResponse.user.toDomainModel()
                    tokenManager.saveUser(domainUser)

                    try {
                        val wishlistResponse = treasureBackendApi.getDetailedWishlist()

                        if (wishlistResponse.isSuccessful) {
                            val savedGames = wishlistResponse.body() ?: emptyList()

                            savedGames.forEach { gameDto ->
                                val targetGameId = gameDto.id
                                val existingInteraction =
                                    db.userInteractionDao().getInteractionForGame(targetGameId)

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

                                db.userInteractionDao().insertInteraction(updatedEntity)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restore wishlist on login", e)
                    }
                    Result.success(domainUser)
                } else {
                    Log.e(TAG, "Login failed: ${response.code()} ${response.errorBody()?.string()}")
                    Result.failure(Exception("Login failed. Please try again."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during login", e)
                Result.failure(e)
            }
        }

    /**
     * Terminates the active user session.
     *
     * Notifies the backend to revoke refresh tokens, wipes local session credentials from [TokenManager],
     * purges all tables in [TreasureDatabase], and cancels all queued [WorkManager] periodic workers.
     *
     * @return [Result.success] confirming completion.
     */
    override suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val refreshToken = tokenManager.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                val request = RefreshTokenRequest(refreshToken)
                treasureBackendApi.logout(request)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying backend of logout, proceeding to clear local session", e)
        } finally {
            tokenManager.clearSession()

            try {
                db.clearAllTables()
                WorkManager.getInstance(context).cancelAllWork()
            } catch (e: Exception) {
                Log.e(TAG, "Error wiping local database on logout", e)
            }
        }
        Result.success(Unit)
    }

    /**
     * Permanently deletes the authenticated user's account and personal records on the backend server.
     *
     * Wipes local session preferences, clears database tables, and stops background workers upon confirmation.
     *
     * @return [Result.success] upon server confirmation, or [Result.failure] on error.
     */
    override suspend fun deleteAccount(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = treasureBackendApi.deleteMyAccount()
            if (response.isSuccessful) {
                tokenManager.clearSession()

                try {
                    db.clearAllTables()
                    WorkManager.getInstance(context).cancelAllWork()
                } catch (e: Exception) {
                    Log.e(TAG, "Error wiping local database on account deletion", e)
                }

                Result.success(Unit)
            } else {
                Log.e(TAG, "Account deletion failed: ${response.code()}")
                Result.failure(Exception("Failed to delete account. Please contact support."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during account deletion", e)
            Result.failure(e)
        }
    }

    /**
     * Transforms remote [UserDto] received from the backend into a domain [User] entity.
     *
     * @return Domain [User] model representation.
     */
    private fun UserDto.toDomainModel(): User {
        return User(
            id = this.id,
            email = this.email,
            fullName = this.fullName,
            profilePicture = this.profilePicture,
            role = this.role.name
        )
    }
}
