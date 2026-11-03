package com.example.treasure.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.treasure.data.local.dao.DealDao
import com.example.treasure.data.local.dao.DownloadedAssetDao
import com.example.treasure.data.local.dao.NotificationDao
import com.example.treasure.data.local.dao.RemoteKeysDao
import com.example.treasure.data.local.dao.UserInteractionDao
import com.example.treasure.data.local.entity.Converters
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.DownloadedAssetEntity
import com.example.treasure.data.local.entity.NotificationEntity
import com.example.treasure.data.local.entity.RemoteKeys
import com.example.treasure.data.local.entity.UserInteractionEntity

/**
 * Primary Room Database definition serving as the local Single Source of Truth (SSOT).
 *
 * Persists cached game deals, Paging 3 pagination cursor keys, user interactions (favorites & cart),
 * price fluctuation alerts, and downloaded wallpaper asset records.
 *
 * Database Configuration:
 * - Version: 5
 * - Export Schema: disabled (`false`)
 * - Converters: [Converters] (JSON serialization for complex collections and enum mappings)
 */
@Database(
    entities = [
        RemoteKeys::class,
        DealEntity::class,
        UserInteractionEntity::class,
        NotificationEntity::class,
        DownloadedAssetEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TreasureDatabase : RoomDatabase() {

    /**
     * Provides access to [RemoteKeysDao] for Paging 3 pagination boundaries.
     *
     * @return Data access object for remote cursor keys.
     */
    abstract fun remoteKeysDao(): RemoteKeysDao

    /**
     * Provides access to [DealDao] for cached game deals and detail enrichment.
     *
     * @return Data access object for deal entities.
     */
    abstract fun dealDao(): DealDao

    /**
     * Provides access to [UserInteractionDao] for cart and wishlist state tracking.
     *
     * @return Data access object for user interactions.
     */
    abstract fun userInteractionDao(): UserInteractionDao

    /**
     * Provides access to [NotificationDao] for price fluctuation alert history.
     *
     * @return Data access object for notifications.
     */
    abstract fun notificationDao(): NotificationDao

    /**
     * Provides access to [DownloadedAssetDao] for tracked media wallpaper downloads.
     *
     * @return Data access object for downloaded assets.
     */
    abstract fun downloadedAssetDao(): DownloadedAssetDao
}
