//package com.example.treasure.workers
//
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.util.Log
//import androidx.core.app.NotificationCompat
//import androidx.core.net.toUri
//import androidx.hilt.work.HiltWorker
//import androidx.room.withTransaction
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import com.example.treasure.R
//import com.example.treasure.data.local.TreasureDatabase
//import com.example.treasure.data.local.entity.NotificationEntity
//import com.example.treasure.data.local.entity.UserInteractionEntity
//import com.example.treasure.data.remote.apiService.TreasureBackendApi
//import com.example.treasure.data.repositoryImpl.SettingsRepository
//import dagger.assisted.Assisted
//import dagger.assisted.AssistedInject
//import kotlinx.coroutines.CoroutineDispatcher
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.withContext
//import kotlin.math.abs
//
//@HiltWorker
//class PriceSyncWorker @AssistedInject constructor(
//    @Assisted appContext: Context,
//    @Assisted workerParams: WorkerParameters,
//    private val database: TreasureDatabase,
//    private val treasureBackendApi: TreasureBackendApi,
//    private val settingsRepository: SettingsRepository,
//    private val ioDispatcher: CoroutineDispatcher
//) : CoroutineWorker(appContext, workerParams) {
//
//    override suspend fun doWork(): Result = workDefinition()
//
//    private suspend fun workDefinition(): Result = withContext(ioDispatcher) {
//        Log.d("PriceSyncWorker", "Work started - checking for price updates via BFF")
//
//        val areNotificationsEnabled = settingsRepository.areNotificationsEnabled.first()
//        if (!areNotificationsEnabled) {
//            Log.d("PriceSyncWorker", "Notifications disabled by user. Skipping work.")
//            return@withContext Result.success()
//        }
//
//        val thresholdPercent = settingsRepository.notifyThreshold.first()
//        val thresholdDecimal = thresholdPercent / 100.0
//
//        try {
//            val wishlistItems = database.userInteractionDao().getFavorites().first()
//            if (wishlistItems.isEmpty()) {
//                Log.d("PriceSyncWorker", "No local favorites found, skipping sync")
//                return@withContext Result.success()
//            }
//
//            // --- 1. Single call to the BFF ---
//            // The TokenAuthenticator handles injecting the JWT automatically!
//            val response = treasureBackendApi.getWishlistPrices()
//
//            if (!response.isSuccessful || response.body() == null) {
//                val code = response.code()
//                Log.e("PriceSyncWorker", "Failed to fetch prices from BFF: $code")
//                // Retry if it's a server error or rate limit
//                return@withContext if (code in 500..599 || code == 429) Result.retry() else Result.failure()
//            }
//
//            // Map of GameID -> Price String (e.g. "29.99")
//            val remotePricesMap = response.body()!!
//
//            val changedGames = mutableListOf<NotificationEntity>()
//            val updatedInteractions = mutableListOf<UserInteractionEntity>()
//
//            // --- 2. Compare local cache with BFF prices ---
//            wishlistItems.forEach { localItem ->
//                val newPriceString = remotePricesMap[localItem.gameId]
//                val newPrice = newPriceString?.toDoubleOrNull()
//
//                if (newPrice != null) {
//                    if (localItem.latestSyncedPrice != newPrice) {
//                        updatedInteractions.add(
//                            localItem.copy(
//                                latestSyncedPrice = newPrice,
//                                lastSyncTimestamp = System.currentTimeMillis()
//                            )
//                        )
//                    }
//
//                    val snapshotPrice = localItem.currentPrice
//                    if (snapshotPrice > 0) {
//                        val diff = newPrice - snapshotPrice
//                        val changePercent = diff / snapshotPrice
//
//                        // Check if the price drop exceeds the user's settings threshold
//                        if (abs(changePercent) > thresholdDecimal) {
//                            changedGames.add(
//                                NotificationEntity(
//                                    gameId = localItem.gameId,
//                                    title = localItem.title,
//                                    thumbnail = localItem.thumbnail,
//                                    oldPrice = snapshotPrice,
//                                    newPrice = newPrice
//                                )
//                            )
//                        }
//                    }
//                }
//            }
//
//            // --- 3. Update Local DB & Trigger Notifications ---
//            if (updatedInteractions.isNotEmpty()) {
//                database.withTransaction {
//                    updatedInteractions.forEach { interaction ->
//                        database.userInteractionDao().insertInteraction(interaction)
//                    }
//                }
//            }
//
//            processNotifications(changedGames)
//            Result.success()
//
//        } catch (e: Exception) {
//            Log.e("PriceSyncWorker", "Work failed due to exception", e)
//            Result.failure()
//        }
//    }
//
//    private suspend fun processNotifications(changedGames: List<NotificationEntity>) {
//        if (changedGames.isNotEmpty()) {
//            database.withTransaction {
//                changedGames.forEach {
//                    database.notificationDao().insertNotification(it)
//                }
//            }
//
//            if (changedGames.size == 1) {
//                showSingleGameNotification(changedGames.first())
//            } else {
//                showBatchedNotification(changedGames)
//            }
//        }
//        Log.d("PriceSyncWorker", "Processed ${changedGames.size} notification(s)")
//    }
//
//    private fun getPendingIntent(): PendingIntent {
//        val intent = Intent(Intent.ACTION_VIEW, "treasure://wishlist".toUri()).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//        return PendingIntent.getActivity(
//            applicationContext, 0, intent,
//            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//        )
//    }
//
//    private fun createChannel(manager: NotificationManager) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                "price_alert_channel", "Price Alerts",
//                NotificationManager.IMPORTANCE_HIGH
//            ).apply {
//                description = "Alerts for price drops and hikes on your wishlist"
//            }
//            manager.createNotificationChannel(channel)
//        }
//    }
//
//    private fun showSingleGameNotification(item: NotificationEntity) {
//        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        createChannel(manager)
//
//        val isDrop = item.newPrice < item.oldPrice
//        val title = if (isDrop) "Price Drop: ${item.title}" else "Price Hike: ${item.title}"
//        val content = "Price updated to ₹${item.newPrice.toInt()}"
//
//        val notification = NotificationCompat.Builder(applicationContext, "price_alert_channel")
//            .setSmallIcon(if (isDrop) R.drawable.wishlist_filled else R.drawable.wishlist_outlined)
//            .setContentTitle(title)
//            .setContentText(content)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(getPendingIntent())
//            .setAutoCancel(true)
//            .build()
//
//        manager.notify(item.gameId.hashCode(), notification)
//    }
//
//    private fun showBatchedNotification(items: List<NotificationEntity>) {
//        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        createChannel(manager)
//
//        val inboxStyle = NotificationCompat.InboxStyle()
//            .setBigContentTitle("${items.size} Price Updates")
//
//        items.take(5).forEach {
//            val prefix = if (it.newPrice < it.oldPrice) "↓" else "↑"
//            inboxStyle.addLine("$prefix ${it.title}: ₹${it.newPrice.toInt()}")
//        }
//
//        val notification = NotificationCompat.Builder(applicationContext, "price_alert_channel")
//            .setSmallIcon(R.drawable.wishlist_filled)
//            .setContentTitle("Wishlist Price Updates")
//            .setContentText("${items.size} games have new prices.")
//            .setStyle(inboxStyle)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(getPendingIntent())
//            .setAutoCancel(true)
//            .build()
//
//        manager.notify(999, notification)
//    }
//}











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
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.treasure.R
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.entity.NotificationEntity
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.repositoryImpl.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.abs

@HiltWorker
class PriceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: TreasureDatabase,
    private val treasureBackendApi: TreasureBackendApi,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(
    appContext,
    workerParams
) {

    init {
        Log.d(
            "PriceSyncWorker",
            "========== WORKER INSTANCE CREATED =========="
        )
    }

    override suspend fun doWork(): Result {

        Log.d(
            "PriceSyncWorker",
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

                    val newPriceString =
                        remotePricesMap[localItem.gameId]

                    val newPrice =
                        newPriceString?.toDoubleOrNull()

                    Log.d(
                        TAG,
                        """
                        GAME CHECK
                        Game ID: ${localItem.gameId}
                        Title: ${localItem.title}
                        Local currentPrice: ${localItem.currentPrice}
                        Local latestSyncedPrice: ${localItem.latestSyncedPrice}
                        Remote price string: $newPriceString
                        Remote price: $newPrice
                        """.trimIndent()
                    )

                    if (newPrice == null) {

                        Log.d(
                            TAG,
                            "No valid remote price for ${localItem.gameId}"
                        )

                        return@forEach
                    }

                    if (localItem.latestSyncedPrice != newPrice) {

                        Log.d(
                            TAG,
                            "Price changed since last sync: " +
                                    "${localItem.latestSyncedPrice} -> $newPrice"
                        )

                        updatedInteractions.add(
                            localItem.copy(
                                latestSyncedPrice = newPrice,
                                lastSyncTimestamp =
                                    System.currentTimeMillis()
                            )
                        )
                    }

                    val snapshotPrice =
                        localItem.currentPrice

                    if (snapshotPrice <= 0) {

                        Log.d(
                            TAG,
                            "Snapshot price <= 0. Cannot calculate percentage."
                        )

                        return@forEach
                    }

                    val diff =
                        newPrice - snapshotPrice

                    val changePercent =
                        diff / snapshotPrice

                    Log.d(
                        TAG,
                        """
                        PRICE COMPARISON
                        Game: ${localItem.title}
                        Snapshot: $snapshotPrice
                        New: $newPrice
                        Difference: $diff
                        Change: ${changePercent * 100}%
                        Threshold: $thresholdPercent%
                        Absolute change: ${abs(changePercent) * 100}%
                        """.trimIndent()
                    )

                    if (
                        abs(changePercent) >
                        thresholdDecimal
                    ) {

                        Log.d(
                            TAG,
                            "!!! NOTIFICATION THRESHOLD REACHED !!!"
                        )

                        changedGames.add(
                            NotificationEntity(
                                gameId = localItem.gameId,
                                title = localItem.title,
                                thumbnail = localItem.thumbnail,
                                oldPrice = snapshotPrice,
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
                    "Games requiring price update = " +
                            updatedInteractions.size
                )

                Log.d(
                    TAG,
                    "Games requiring notification = " +
                            changedGames.size
                )

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

            showSingleGameNotification(
                changedGames.first()
            )

        } else {

            Log.d(
                TAG,
                "Showing BATCHED notification: ${changedGames.size}"
            )

            showBatchedNotification(
                changedGames
            )
        }
    }

    private fun getPendingIntent(): PendingIntent {

        val intent = Intent(
            Intent.ACTION_VIEW,
            "treasure://wishlist".toUri()
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createChannel(
        manager: NotificationManager
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "price_alert_channel",
                "Price Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    "Alerts for price drops and hikes on your wishlist"
            }

            manager.createNotificationChannel(channel)

            Log.d(
                TAG,
                "Notification channel created/updated"
            )
        }
    }

    private fun showSingleGameNotification(
        item: NotificationEntity
    ) {

        val manager =
            applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        Log.d(
            TAG,
            "NotificationManager obtained"
        )

        createChannel(manager)

        val isDrop =
            item.newPrice < item.oldPrice

        val title =
            if (isDrop) {
                "Price Drop: ${item.title}"
            } else {
                "Price Hike: ${item.title}"
            }

        val content =
            "Price updated to ₹${item.newPrice.toInt()}"

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                "price_alert_channel"
            )
                .setSmallIcon(
                    if (isDrop) {
                        R.drawable.wishlist_filled
                    } else {
                        R.drawable.wishlist_outlined
                    }
                )
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setContentIntent(
                    getPendingIntent()
                )
                .setAutoCancel(true)
                .build()

        Log.d(
            TAG,
            "Calling NotificationManager.notify()"
        )

        manager.notify(
            item.gameId.hashCode(),
            notification
        )

        Log.d(
            TAG,
            "NotificationManager.notify() completed"
        )
    }

    private fun showBatchedNotification(
        items: List<NotificationEntity>
    ) {

        val manager =
            applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        createChannel(manager)

        val inboxStyle =
            NotificationCompat.InboxStyle()
                .setBigContentTitle(
                    "${items.size} Price Updates"
                )

        items
            .take(5)
            .forEach {

                val prefix =
                    if (it.newPrice < it.oldPrice) {
                        "↓"
                    } else {
                        "↑"
                    }

                inboxStyle.addLine(
                    "$prefix ${it.title}: ₹${it.newPrice.toInt()}"
                )
            }

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                "price_alert_channel"
            )
                .setSmallIcon(
                    R.drawable.wishlist_filled
                )
                .setContentTitle(
                    "Wishlist Price Updates"
                )
                .setContentText(
                    "${items.size} games have new prices."
                )
                .setStyle(inboxStyle)
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setContentIntent(
                    getPendingIntent()
                )
                .setAutoCancel(true)
                .build()

        Log.d(
            TAG,
            "Calling NotificationManager.notify() for batch"
        )

        manager.notify(
            999,
            notification
        )
    }

    companion object {
        private const val TAG = "PriceSyncWorker"
    }
}