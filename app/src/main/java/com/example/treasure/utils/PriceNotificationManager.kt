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

interface PriceNotificationManager {
    fun showSingleGameNotification(item: NotificationEntity)
    fun showBatchedNotification(items: List<NotificationEntity>)
}

@Singleton
class PriceNotificationManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PriceNotificationManager {

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, "treasure://wishlist".toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
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

    override fun showSingleGameNotification(item: NotificationEntity) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(manager)

        val isDrop = item.newPrice < item.oldPrice
        val title = if (isDrop) "Price Drop: ${item.title}" else "Price Hike: ${item.title}"
        val content = "Price updated to ₹${item.newPrice.toInt()}"

        val notification = NotificationCompat.Builder(context, "price_alert_channel")
            .setSmallIcon(if (isDrop) R.drawable.wishlist_filled else R.drawable.wishlist_outlined)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()

        manager.notify(item.gameId.hashCode(), notification)
    }

    override fun showBatchedNotification(items: List<NotificationEntity>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(manager)

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${items.size} Price Updates")

        items.take(5).forEach {
            val prefix = if (it.newPrice < it.oldPrice) "↓" else "↑"
            inboxStyle.addLine("$prefix ${it.title}: ₹${it.newPrice.toInt()}")
        }

        val notification = NotificationCompat.Builder(context, "price_alert_channel")
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

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    @Singleton
    abstract fun bindPriceNotificationManager(
        impl: PriceNotificationManagerImpl
    ): PriceNotificationManager
}
