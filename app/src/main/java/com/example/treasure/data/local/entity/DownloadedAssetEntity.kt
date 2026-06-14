package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloaded_asset"
)
data class DownloadedAssetEntity(
    @PrimaryKey
    val assetId: String,
    val fileName: String,
    val downloadId: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)
