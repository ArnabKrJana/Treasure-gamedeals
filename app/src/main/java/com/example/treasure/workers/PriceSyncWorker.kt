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
 * Synchronizes prices for the user's locally stored wishlist.
 *
 * Price changes are calculated against the previously synchronized price
 * stored in [UserInteractionEntity.latestSyncedPrice].
 *
 * First successful sync:
 *      Establishes the initial price baseline.
 *
 * Subsequent sync:
 *      Compares the new price against the previous synchronized price.
 *
 * If the absolute percentage change reaches or exceeds the user's configured threshold,
 * a notification is created and displayed.
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

    init {
        Log.d(
            TAG,
            "========== WORKER INSTANCE CREATED =========="
        )
    }

    override suspend fun doWork(): Result {

        Log.d(
            TAG,
            "========== DO WORK STARTED =========="
        )

        return workDefinition()
    }

    private suspend fun workDefinition(): Result =
        withContext(ioDispatcher) {

            try {

                Log.d(
                    TAG,
                    "Checking notification settings..."
                )

                val areNotificationsEnabled =
                    settingsRepository
                        .areNotificationsEnabled
                        .first()

                Log.d(
                    TAG,
                    "Notifications enabled = $areNotificationsEnabled"
                )

                if (!areNotificationsEnabled) {

                    Log.d(
                        TAG,
                        "Notifications disabled. Worker stopping."
                    )

                    return@withContext Result.success()
                }

                val thresholdPercent =
                    settingsRepository
                        .notifyThreshold
                        .first()

                val thresholdDecimal =
                    thresholdPercent / 100.0

                Log.d(
                    TAG,
                    "Notification threshold = $thresholdPercent%"
                )

                Log.d(
                    TAG,
                    "Reading local favorites..."
                )

                val wishlistItems =
                    database
                        .userInteractionDao()
                        .getFavorites()
                        .first()

                Log.d(
                    TAG,
                    "Local favorites count = ${wishlistItems.size}"
                )

                if (wishlistItems.isEmpty()) {

                    Log.d(
                        TAG,
                        "No favorites found. Worker stopping."
                    )

                    return@withContext Result.success()
                }

                Log.d(
                    TAG,
                    "Calling backend getWishlistPrices()..."
                )

                val response =
                    treasureBackendApi.getWishlistPrices()

                Log.d(
                    TAG,
                    "Backend response code = ${response.code()}"
                )

                if (!response.isSuccessful || response.body() == null) {

                    val code = response.code()

                    Log.e(
                        TAG,
                        "Backend price request FAILED. HTTP $code"
                    )

                    return@withContext if (
                        code in 500..599 || code == 429
                    ) {

                        Log.d(
                            TAG,
                            "Returning RETRY"
                        )

                        Result.retry()

                    } else {

                        Log.d(
                            TAG,
                            "Returning FAILURE"
                        )

                        Result.failure()
                    }
                }

                /*
                 * Backend currently returns:
                 * {
                 *   "gameId": "{\"current\":1000.66,\"original\":3338.71}"
                 * }
                 * Therefore, the Retrofit type remains Map<String, String>
                 * and each String must be parsed as JSON.
                 */
                val remotePricesMap =
                    response.body()!!

                Log.d(
                    TAG,
                    "Remote prices received: $remotePricesMap"
                )

                val changedGames =
                    mutableListOf<NotificationEntity>()

                val updatedInteractions =
                    mutableListOf<UserInteractionEntity>()

                wishlistItems.forEach { localItem ->

                    val remotePriceJson =
                        remotePricesMap[localItem.gameId]

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

                        Log.d(
                            TAG,
                            "No remote price found for gameId=${localItem.gameId}"
                        )

                        return@forEach
                    }

                    val remotePriceDto: WishlistPriceDto?

                    try {

                        remotePriceDto =
                            gson.fromJson(
                                remotePriceJson,
                                WishlistPriceDto::class.java
                            )

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "Failed to parse remote price for gameId=${localItem.gameId}: $remotePriceJson",
                            e
                        )

                        return@forEach
                    }

                    val newPrice =
                        remotePriceDto?.current

                    if (newPrice == null) {

                        Log.d(
                            TAG,
                            "Remote price current value is null for ${localItem.gameId}"
                        )

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

                    /*
                     * First successful sync for this game.
                     *
                     * There is no previous price to compare against,
                     * so simply establish the initial baseline.
                     */
                    if (previousPrice <= 0.0) {

                        Log.d(
                            TAG,
                            "No previous price available. Establishing initial price = $newPrice"
                        )

                        updatedInteractions.add(
                            localItem.copy(
                                latestSyncedPrice = newPrice,
                                lastSyncTimestamp = System.currentTimeMillis()
                            )
                        )

                        return@forEach
                    }

                    /*
                     * Compare the NEW price against the PREVIOUSLY SYNCED price.
                     */
                    val diff = newPrice - previousPrice

                    val changePercent =
                        diff / previousPrice

                    val absoluteChangePercent =
                        abs(changePercent) * 100

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

                    /*
                     * Always update the baseline after successfully
                     * receiving a valid price.
                     */
                    if (previousPrice != newPrice) {

                        updatedInteractions.add(
                            localItem.copy(
                                latestSyncedPrice = newPrice,
                                lastSyncTimestamp = System.currentTimeMillis()
                            )
                        )
                    }

                    /*
                     * Notify only when the change from the PREVIOUS
                     * known price exceeds or equals the configured threshold.
                     */
                    if (abs(changePercent) >= thresholdDecimal) {

                        Log.d(
                            TAG,
                            "!!! NOTIFICATION THRESHOLD REACHED !!!"
                        )

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

                        Log.d(
                            TAG,
                            "Below notification threshold."
                        )
                    }
                }

                Log.d(
                    TAG,
                    "Games requiring price update = ${updatedInteractions.size}"
                )

                Log.d(
                    TAG,
                    "Games requiring notification = ${changedGames.size}"
                )

                /*
                 * Save latest synced prices.
                 */
                if (updatedInteractions.isNotEmpty()) {

                    Log.d(
                        TAG,
                        "Updating local interaction prices..."
                    )

                    database.withTransaction {

                        updatedInteractions.forEach { interaction ->

                            database
                                .userInteractionDao()
                                .insertInteraction(interaction)
                        }
                    }

                    Log.d(
                        TAG,
                        "Local price updates completed"
                    )
                }

                /*
                 * Save notification records and show
                 * Android notifications.
                 */
                processNotifications(changedGames)

                Log.d(
                    TAG,
                    "========== WORKER SUCCEEDED =========="
                )

                Result.success()

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "========== WORKER EXCEPTION ==========",
                    e
                )

                /*
                 * Network / unexpected errors should be retried
                 * by WorkManager.
                 */
                Result.retry()
            }
        }

    private suspend fun processNotifications(
        changedGames: List<NotificationEntity>
    ) {

        Log.d(
            TAG,
            "processNotifications(): ${changedGames.size} games"
        )

        if (changedGames.isEmpty()) {

            Log.d(
                TAG,
                "No notifications to process."
            )

            return
        }

        database.withTransaction {

            changedGames.forEach {

                Log.d(
                    TAG,
                    "Saving notification DB record: ${it.gameId}"
                )

                database
                    .notificationDao()
                    .insertNotification(it)
            }
        }

        Log.d(
            TAG,
            "Notification DB records saved."
        )

        if (changedGames.size == 1) {

            Log.d(
                TAG,
                "Showing SINGLE notification"
            )

            priceNotificationManager.showSingleGameNotification(
                changedGames.first()
            )

        } else {

            Log.d(
                TAG,
                "Showing BATCHED notification: ${changedGames.size}"
            )

            priceNotificationManager.showBatchedNotification(
                changedGames
            )
        }
    }

    companion object {
        private const val TAG = "PriceSyncWorker"
    }
}
