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

@Database(
    entities = [RemoteKeys::class, DealEntity::class, UserInteractionEntity::class, NotificationEntity::class, DownloadedAssetEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TreasureDatabase: RoomDatabase() {
    abstract fun remoteKeysDao(): RemoteKeysDao
    abstract fun dealDao(): DealDao
    abstract fun userInteractionDao(): UserInteractionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun downloadedAssetDao(): DownloadedAssetDao
}