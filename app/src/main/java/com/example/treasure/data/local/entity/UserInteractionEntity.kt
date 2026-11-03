package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity capturing user interaction states (cart membership and wishlisted favorites) for a game.
 *
 * Implements an offline-first state pattern:
 * - State columns track user action flags (`isAddedToCart`, `isFavorite`).
 * - Snapshot columns preserve game metadata at the moment of interaction (`title`, `thumbnail`, `currentPrice`, `originalPrice`, `storeId`)
 *   ensuring instant offline rendering of cart and wishlist screens without external network roundtrips.
 * - Sync columns track periodic background price synchronization performed by [com.example.treasure.workers.PriceSyncWorker].
 *
 * @property gameId Unique identifier of the game serving as the primary key.
 * @property isAddedToCart `true` if the item is present in the user's active cart.
 * @property isFavorite `true` if the item is marked as a favorite/wishlist title.
 * @property timestamp Unix epoch timestamp (in milliseconds) of the most recent interaction update.
 * @property title Cached title of the game.
 * @property thumbnail Cached remote URL pointing to the game's box art image.
 * @property currentPrice Price at the time the user added or updated the interaction.
 * @property originalPrice Baseline MSRP price at the time of interaction.
 * @property storeId Identifier of the storefront offering the deal.
 * @property latestSyncedPrice Most recent price polled by background synchronization workers.
 * @property lastSyncTimestamp Unix epoch timestamp (in milliseconds) of the last successful background price poll.
 */
@Entity(tableName = "user_interactions")
data class UserInteractionEntity(
    @PrimaryKey
    val gameId: String,
    val isAddedToCart: Boolean = false,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val thumbnail: String?,
    val currentPrice: Double,
    val originalPrice: Double,
    val storeId: String,
    val latestSyncedPrice: Double? = null,
    val lastSyncTimestamp: Long? = null
)
