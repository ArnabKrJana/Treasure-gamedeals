package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity recording price fluctuation alerts for wishlisted game titles.
 *
 * Generated when background synchronization ([com.example.treasure.workers.PriceSyncWorker])
 * detects price drops or price increases relative to previously recorded price snapshots.
 *
 * @property id Auto-generated integer primary key identifying the notification event.
 * @property gameId Unique identifier of the game associated with the price change.
 * @property title Primary title of the game.
 * @property thumbnail Remote URL pointing to the game's box art image.
 * @property oldPrice Historical baseline price before the detected change.
 * @property newPrice Newly detected price.
 * @property timestamp Unix epoch timestamp (in milliseconds) when the alert was generated.
 * @property isRead `true` if the notification has been acknowledged by the user in the UI.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val gameId: String,
    val title: String,
    val thumbnail: String?,
    val oldPrice: Double,
    val newPrice: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
