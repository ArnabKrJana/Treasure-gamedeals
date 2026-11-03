package com.example.treasure.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.treasure.R
import com.example.treasure.data.local.entity.NotificationEntity
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Architectural contract governing the presentation of system notification alerts for game price fluctuations.
 *
 * Dispatches notifications when background synchronization ([com.example.treasure.workers.PriceSyncWorker])
 * detects price drops or price increases for wishlisted titles.
 */
interface PriceNotificationManager {
    /**
     * Posts a focused heads-up system notification for a single game price change event.
     *
     * @param item Entity encapsulating the updated price, prior price, game title, and store metadata.
     */
    fun showSingleGameNotification(item: NotificationEntity)

    /**
     * Posts a consolidated [androidx.core.app.NotificationCompat.InboxStyle] notification summarizing multiple
     * game price updates in a single notification tray item to prevent notification fatigue.
     *
     * @param items List of price change events to be aggregated.
     */
    fun showBatchedNotification(items: List<NotificationEntity>)
}

/**
 * Android system notification dispatcher implementing [PriceNotificationManager].
 *
 * Configures notification channels for Android 8.0+ (Oreo), constructs deep-link pending intents
 * targeting the application's wishlist destination (`treasure://wishlist`), and formats localized price
 * drop/hike messages.
 *
 * @param context Application context used to resolve system services, resources, and pending intents.
 */
@Singleton
class PriceNotificationManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PriceNotificationManager {

    /**
     * Builds an immutable deep-link [PendingIntent] navigating the user directly to the Wishlist screen.
     *
     * @return A configured [PendingIntent] with `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_CLEAR_TASK`.
     */
    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, "treasure://wishlist".toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Creates and registers the high-importance notification channel on Android API 26 (Oreo) and above.
     *
     * @param manager The platform [NotificationManager] system service.
     */
    private fun createChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Displays a notification for an individual game price update.
     *
     * Differentiates visually between a price drop (green/filled icon) and a price hike (outline icon),
     * and uses the game's unique ID hash code as the notification identifier to allow distinct updates per title.
     *
     * @param item The [NotificationEntity] containing updated pricing data.
     */
    override fun showSingleGameNotification(item: NotificationEntity) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(manager)

        val isDrop = item.newPrice < item.oldPrice
        val title = if (isDrop) "Price Drop: ${item.title}" else "Price Hike: ${item.title}"
        val content = "Price updated to ₹${item.newPrice.toInt()}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (isDrop) R.drawable.wishlist_filled else R.drawable.wishlist_outlined)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()

        manager.notify(item.gameId.hashCode(), notification)
    }

    /**
     * Displays an aggregated [NotificationCompat.InboxStyle] notification summarizing up to 5 price updates.
     *
     * Prefixes lines with directional indicators (↓ for drop, ↑ for hike) and publishes under a static notification ID
     * (999) to collapse multiple updates into a single concise notification drawer entry.
     *
     * @param items List of [NotificationEntity] instances to summarize.
     */
    override fun showBatchedNotification(items: List<NotificationEntity>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(manager)

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${items.size} Price Updates")

        items.take(5).forEach {
            val prefix = if (it.newPrice < it.oldPrice) "↓" else "↑"
            inboxStyle.addLine("$prefix ${it.title}: ₹${it.newPrice.toInt()}")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.wishlist_filled)
            .setContentTitle("Wishlist Price Updates")
            .setContentText("${items.size} games have new prices.")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()

        manager.notify(BATCH_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "price_alert_channel"
        private const val CHANNEL_NAME = "Price Alerts"
        private const val CHANNEL_DESCRIPTION = "Alerts for price drops and hikes on your wishlist"
        private const val BATCH_NOTIFICATION_ID = 999
    }
}

/**
 * Hilt dependency injection module providing the [PriceNotificationManager] singleton binding.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    /**
     * Binds [PriceNotificationManagerImpl] as the application-wide [PriceNotificationManager] implementation.
     *
     * @param impl Concrete notification manager implementation.
     * @return Bound [PriceNotificationManager] contract.
     */
    @Binds
    @Singleton
    abstract fun bindPriceNotificationManager(
        impl: PriceNotificationManagerImpl
    ): PriceNotificationManager
}
