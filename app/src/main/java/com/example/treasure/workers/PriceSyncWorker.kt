package com.example.treasure.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.treasure.R
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.entity.NotificationEntity
import com.example.treasure.data.remote.apiService.ItadApi
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.utils.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.abs

@HiltWorker
class PriceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: TreasureDatabase,
    private val itadApi: ItadApi,
    private val settingsRepository: SettingsRepository // 1. Injected Repository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Switch to demoWorkDefinition() here for testing if needed
        return workDefinition()
    }

    /**
     * ACTUAL PRODUCTION LOGIC
     * Notifies based on user settings
     */
    private suspend fun workDefinition(): Result = withContext(Dispatchers.IO) {
        Log.d("PriceSyncWorker", "Work started - checking for price updates")

        // 2. CHECK SETTINGS: Are notifications enabled?
        // We use .first() to get the current snapshot of the setting
        val areNotificationsEnabled = settingsRepository.areNotificationsEnabled.first()
        if (!areNotificationsEnabled) {
            Log.d("PriceSyncWorker", "Notifications disabled by user. Skipping work.")
            return@withContext Result.success()
        }

        // 3. GET DYNAMIC THRESHOLD
        // Convert integer (e.g., 5) to decimal (0.05)
        val thresholdPercent = settingsRepository.notifyThreshold.first()
        val thresholdDecimal = thresholdPercent / 100.0

        try {
            val wishlistItems = database.userInteractionDao().getFavorites().first()
            if (wishlistItems.isEmpty()) {
                Log.d("PriceSyncWorker", "No favorites found, skipping sync")
                return@withContext Result.success()
            }

            val gameIds = wishlistItems.map { it.gameId }
            val response = itadApi.getPriceOverview(
                country = "IN",
                shops = "61,35,16",
                gameIds = gameIds
            )

            if (!response.isSuccessful || response.body() == null) {
                Log.e("PriceSyncWorker", "API request failed: ${response.code()}")
                return@withContext Result.retry()
            }

            val priceMap = response.body()!!.prices.associateBy { it.id }
            val changedGames = mutableListOf<NotificationEntity>()

            wishlistItems.forEach { localItem ->
                val remoteItem = priceMap[localItem.gameId]
                val newPrice = remoteItem?.current?.price?.amount

                if (newPrice != null) {
                    if (localItem.latestSyncedPrice != newPrice) {
                        database.userInteractionDao().insertInteraction(
                            localItem.copy(
                                latestSyncedPrice = newPrice,
                                lastSyncTimestamp = System.currentTimeMillis()
                            )
                        )
                    }

                    val snapshotPrice = localItem.currentPrice
                    if (snapshotPrice > 0) {
                        val diff = newPrice - snapshotPrice
                        val changePercent = diff / snapshotPrice

                        // 4. USE DYNAMIC THRESHOLD in logic
                        // We check if the absolute change is greater than user setting
                        if (abs(changePercent) > thresholdDecimal) {
                            changedGames.add(
                                NotificationEntity(
                                    gameId = localItem.gameId,
                                    title = localItem.title,
                                    thumbnail = localItem.thumbnail,
                                    oldPrice = snapshotPrice,
                                    newPrice = newPrice
                                )
                            )
                        }
                    }
                }
            }

            processNotifications(changedGames)
            Result.success()
        } catch (e: Exception) {
            Log.e("PriceSyncWorker", "Work failed", e)
            Result.failure()
        }
    }

    /**
     * TESTING LOGIC
     */
    private suspend fun demoWorkDefinition(): Result = withContext(Dispatchers.IO) {
        Log.d("PriceSyncWorker", "DEMO Work started - testing notifications")
        try {
            val wishlistItems = database.userInteractionDao().getFavorites().first()
            if (wishlistItems.isEmpty()) {
                Log.d("PriceSyncWorker", "Demo: No favorites found, showing dummy notification")
                showSingleGameNotification(
                    NotificationEntity(
                        gameId = "test_id",
                        title = "Test: No Favorites Found",
                        thumbnail = null,
                        oldPrice = 100.0,
                        newPrice = 90.0
                    )
                )
                return@withContext Result.success()
            }

            val gameIds = wishlistItems.map { it.gameId }
            val response = itadApi.getPriceOverview(
                country = "IN",
                shops = "61,35,16",
                gameIds = gameIds
            )

            val changedGames = mutableListOf<NotificationEntity>()

            if (response.isSuccessful && response.body() != null) {
                val priceMap = response.body()!!.prices.associateBy { it.id }
                wishlistItems.forEach { localItem ->
                    val remoteItem = priceMap[localItem.gameId]
                    val newPrice = remoteItem?.current?.price?.amount ?: (localItem.currentPrice - 1.0)

                    changedGames.add(
                        NotificationEntity(
                            gameId = localItem.gameId,
                            title = "[DEMO] ${localItem.title}",
                            thumbnail = localItem.thumbnail,
                            oldPrice = localItem.currentPrice,
                            newPrice = newPrice
                        )
                    )
                }
            } else {
                Log.e("PriceSyncWorker", "Demo: API failed, showing dummy")
                changedGames.add(
                    NotificationEntity(
                        gameId = "api_fail",
                        title = "Demo: API Request Failed",
                        thumbnail = null,
                        oldPrice = 0.0,
                        newPrice = 0.0
                    )
                )
            }

            processNotifications(changedGames)
            Result.success()
        } catch (e: Exception) {
            Log.e("PriceSyncWorker", "Demo Work failed", e)
            Result.failure()
        }
    }

    private suspend fun processNotifications(changedGames: List<NotificationEntity>) {
        if (changedGames.isNotEmpty()) {
            changedGames.forEach {
                database.notificationDao().insertNotification(it)
            }

            if (changedGames.size == 1) {
                showSingleGameNotification(changedGames.first())
            } else {
                showBatchedNotification(changedGames)
            }
        }
        Log.d("PriceSyncWorker", "Processed ${changedGames.size} notification(s)")
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, "treasure://wishlist".toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "price_alert_channel", "Price Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for price drops and hikes on your wishlist"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun showSingleGameNotification(item: NotificationEntity) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(manager)

        val isDrop = item.newPrice < item.oldPrice
        val title = if (isDrop) "Price Drop: ${item.title}" else "Price Hike: ${item.title}"
        val content = "Price updated to ₹${item.newPrice.toInt()}"

        val notification = NotificationCompat.Builder(applicationContext, "price_alert_channel")
            .setSmallIcon(if (isDrop) R.drawable.wishlist_filled else R.drawable.wishlist_outlined)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()

        manager.notify(item.gameId.hashCode(), notification)
    }

    private fun showBatchedNotification(items: List<NotificationEntity>) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(manager)

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${items.size} Price Updates")

        items.take(5).forEach {
            val prefix = if (it.newPrice < it.oldPrice) "↓" else "↑"
            inboxStyle.addLine("$prefix ${it.title}: ₹${it.newPrice.toInt()}")
        }

        val notification = NotificationCompat.Builder(applicationContext, "price_alert_channel")
            .setSmallIcon(R.drawable.wishlist_filled)
            .setContentTitle("Wishlist Price Updates")
            .setContentText("${items.size} games have new prices.")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()

        manager.notify(999, notification)
    }
}