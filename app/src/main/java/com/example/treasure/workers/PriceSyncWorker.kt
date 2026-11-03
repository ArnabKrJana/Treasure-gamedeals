package com.example.treasure.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.entity.NotificationEntity
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.remote.dto.WishlistPriceDto
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.utils.PriceNotificationManager
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Periodic background [CoroutineWorker] that monitors price drops and hikes for wishlisted games.
 *
 * Managed by Android Jetpack [androidx.work.WorkManager] under system execution constraints:
 * - Network Type: Connected ([androidx.work.NetworkType.CONNECTED])
 * - Battery: Not Low ([androidx.work.Constraints.Builder.setRequiresBatteryNotLow])
 *
 * Synchronization & Baseline Mechanics:
 * 1. Evaluates user configuration in [SettingsRepository] to check whether notifications are enabled.
 * 2. Queries local Room database ([UserInteractionEntity]) for active wishlisted items.
 * 3. Polls the backend BFF endpoint [TreasureBackendApi.getWishlistPrices] for current store prices.
 * 4. Baseline Evaluation:
 *    - First Sync: If [UserInteractionEntity.latestSyncedPrice] is null or zero, establishes the baseline.
 *    - Subsequent Sync: Computes relative price change `(newPrice - previousPrice) / previousPrice`.
 * 5. Notification Dispatch:
 *    - If `abs(changePercent)` meets or exceeds user threshold, persists a [NotificationEntity] in Room
 *      and delegates system notification presentation to [PriceNotificationManager].
 * 6. Updates [UserInteractionEntity.latestSyncedPrice] and timestamp in an atomic Room transaction.
 *
 * @param appContext Android system context provided by WorkManager.
 * @param workerParams Execution runtime parameters and constraints provided by WorkManager.
 * @param database Injected Room [TreasureDatabase] providing transactional access to user interactions and notifications.
 * @param treasureBackendApi Injected Retrofit client for querying remote wishlist prices.
 * @param settingsRepository Injected DataStore repository providing notification flags and threshold preferences.
 * @param priceNotificationManager Injected platform notification manager for displaying single and batched alerts.
 * @param ioDispatcher Injected [CoroutineDispatcher] providing background thread execution.
 */
@HiltWorker
class PriceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: TreasureDatabase,
    private val treasureBackendApi: TreasureBackendApi,
    private val settingsRepository: SettingsRepository,
    private val priceNotificationManager: PriceNotificationManager,
    private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(
    appContext,
    workerParams
) {

    private val gson = Gson()

    companion object {
        private const val TAG = "PriceSyncWorker"
        private const val SERVER_ERROR_MIN = 500
        private const val SERVER_ERROR_MAX = 599
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }

    init {
        Log.d(TAG, "Worker instance created")
    }

    /**
     * Entry point for WorkManager execution. Dispatches workload to [workDefinition] on [ioDispatcher].
     *
     * @return [Result.success] on normal completion, [Result.retry] on transient errors, or [Result.failure].
     */
    override suspend fun doWork(): Result {
        Log.d(TAG, "Price synchronization work started")
        return workDefinition()
    }

    /**
     * Executes the complete price polling, baseline comparison, and notification dispatch lifecycle.
     *
     * @return Execution result indicating success, retry requirement, or failure.
     */
    private suspend fun workDefinition(): Result =
        withContext(ioDispatcher) {
            try {
                Log.d(TAG, "Checking notification settings...")
                val areNotificationsEnabled = settingsRepository.areNotificationsEnabled.first()
                Log.d(TAG, "Notifications enabled = $areNotificationsEnabled")

                if (!areNotificationsEnabled) {
                    Log.d(TAG, "Notifications disabled. Worker stopping.")
                    return@withContext Result.success()
                }

                val thresholdPercent = settingsRepository.notifyThreshold.first()
                val thresholdDecimal = thresholdPercent / 100.0
                Log.d(TAG, "Notification threshold = $thresholdPercent%")

                Log.d(TAG, "Reading local favorites...")
                val wishlistItems = database.userInteractionDao().getFavorites().first()
                Log.d(TAG, "Local favorites count = ${wishlistItems.size}")

                if (wishlistItems.isEmpty()) {
                    Log.d(TAG, "No favorites found. Worker stopping.")
                    return@withContext Result.success()
                }

                Log.d(TAG, "Calling backend getWishlistPrices()...")
                val response = treasureBackendApi.getWishlistPrices()
                Log.d(TAG, "Backend response code = ${response.code()}")

                if (!response.isSuccessful || response.body() == null) {
                    val code = response.code()
                    Log.e(TAG, "Backend price request failed with HTTP $code")

                    return@withContext if (code in SERVER_ERROR_MIN..SERVER_ERROR_MAX || code == HTTP_TOO_MANY_REQUESTS) {
                        Log.d(TAG, "Returning RETRY")
                        Result.retry()
                    } else {
                        Log.d(TAG, "Returning FAILURE")
                        Result.failure()
                    }
                }

                val remotePricesMap = response.body()!!
                Log.d(TAG, "Remote prices received: $remotePricesMap")

                val changedGames = mutableListOf<NotificationEntity>()
                val updatedInteractions = mutableListOf<UserInteractionEntity>()

                wishlistItems.forEach { localItem ->
                    val remotePriceJson = remotePricesMap[localItem.gameId]

                    Log.d(
                        TAG,
                        """
                        GAME CHECK
                        Game ID: ${localItem.gameId}
                        Title: ${localItem.title}
                        Local currentPrice: ${localItem.currentPrice}
                        Local latestSyncedPrice: ${localItem.latestSyncedPrice}
                        Remote JSON: $remotePriceJson
                        """.trimIndent()
                    )

                    if (remotePriceJson == null) {
                        Log.d(TAG, "No remote price found for gameId=${localItem.gameId}")
                        return@forEach
                    }

                    val remotePriceDto: WishlistPriceDto?
                    try {
                        remotePriceDto = gson.fromJson(
                            remotePriceJson,
                            WishlistPriceDto::class.java
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse remote price for gameId=${localItem.gameId}: $remotePriceJson", e)
                        return@forEach
                    }

                    val newPrice = remotePriceDto?.current
                    if (newPrice == null) {
                        Log.d(TAG, "Remote price current value is null for ${localItem.gameId}")
                        return@forEach
                    }

                    Log.d(
                        TAG,
                        """
                        PARSED REMOTE PRICE
                        Game ID: ${localItem.gameId}
                        Current: ${remotePriceDto.current}
                        Original: ${remotePriceDto.original}
                        """.trimIndent()
                    )

                    val previousPrice = localItem.latestSyncedPrice ?: 0.0

                    Log.d(
                        TAG,
                        """
                        PRICE COMPARISON
                        Game: ${localItem.title}
                        Game ID: ${localItem.gameId}
                        Previous known price: $previousPrice
                        New price: $newPrice
                        Threshold: $thresholdPercent%
                        """.trimIndent()
                    )

                    if (previousPrice <= 0.0) {
                        Log.d(TAG, "No previous price available. Establishing initial price = $newPrice")
                        updatedInteractions.add(
                            localItem.copy(
                                latestSyncedPrice = newPrice,
                                lastSyncTimestamp = System.currentTimeMillis()
                            )
                        )
                        return@forEach
                    }

                    val diff = newPrice - previousPrice
                    val changePercent = diff / previousPrice
                    val absoluteChangePercent = abs(changePercent) * 100

                    Log.d(
                        TAG,
                        """
                        PRICE CHANGE
                        Game: ${localItem.title}
                        Previous: $previousPrice
                        New: $newPrice
                        Difference: $diff
                        Change: ${changePercent * 100}%
                        Absolute change: $absoluteChangePercent%
                        Threshold: $thresholdPercent%
                        """.trimIndent()
                    )

                    if (previousPrice != newPrice) {
                        updatedInteractions.add(
                            localItem.copy(
                                latestSyncedPrice = newPrice,
                                lastSyncTimestamp = System.currentTimeMillis()
                            )
                        )
                    }

                    if (abs(changePercent) >= thresholdDecimal) {
                        Log.d(TAG, "Notification threshold reached for ${localItem.title}")
                        changedGames.add(
                            NotificationEntity(
                                gameId = localItem.gameId,
                                title = localItem.title,
                                thumbnail = localItem.thumbnail,
                                oldPrice = previousPrice,
                                newPrice = newPrice
                            )
                        )
                    } else {
                        Log.d(TAG, "Below notification threshold.")
                    }
                }

                Log.d(TAG, "Games requiring price update = ${updatedInteractions.size}")
                Log.d(TAG, "Games requiring notification = ${changedGames.size}")

                if (updatedInteractions.isNotEmpty()) {
                    Log.d(TAG, "Updating local interaction prices...")
                    database.withTransaction {
                        updatedInteractions.forEach { interaction ->
                            database.userInteractionDao().insertInteraction(interaction)
                        }
                    }
                    Log.d(TAG, "Local price updates completed")
                }

                processNotifications(changedGames)

                Log.d(TAG, "Price synchronization worker succeeded")
                Result.success()

            } catch (e: Exception) {
                Log.e(TAG, "Price synchronization worker encountered an exception", e)
                Result.retry()
            }
        }

    /**
     * Persists price fluctuation alert records to the database and dispatches platform notifications.
     *
     * @param changedGames List of games whose price change exceeded the user's notification threshold.
     */
    private suspend fun processNotifications(
        changedGames: List<NotificationEntity>
    ) {
        Log.d(TAG, "processNotifications(): ${changedGames.size} games")

        if (changedGames.isEmpty()) {
            Log.d(TAG, "No notifications to process.")
            return
        }

        database.withTransaction {
            changedGames.forEach {
                Log.d(TAG, "Saving notification DB record: ${it.gameId}")
                database.notificationDao().insertNotification(it)
            }
        }

        Log.d(TAG, "Notification DB records saved.")

        if (changedGames.size == 1) {
            Log.d(TAG, "Showing SINGLE notification")
            priceNotificationManager.showSingleGameNotification(changedGames.first())
        } else {
            Log.d(TAG, "Showing BATCHED notification: ${changedGames.size}")
            priceNotificationManager.showBatchedNotification(changedGames)
        }
    }
}
