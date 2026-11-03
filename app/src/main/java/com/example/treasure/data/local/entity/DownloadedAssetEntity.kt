package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity tracking high-resolution wallpapers and media assets downloaded to local device storage.
 *
 * Maps internal media asset identifiers to system [android.app.DownloadManager] task references,
 * facilitating download completion handling and local asset existence checks.
 *
 * @property assetId Unique identifier of the media asset serving as the primary key.
 * @property fileName Destination file name stored in local storage.
 * @property downloadId System task reference identifier issued by [android.app.DownloadManager].
 * @property downloadedAt Unix epoch timestamp (in milliseconds) recording when the download was enqueued.
 */
@Entity(tableName = "downloaded_asset")
data class DownloadedAssetEntity(
    @PrimaryKey
    val assetId: String,
    val fileName: String,
    val downloadId: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)
