package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_interactions")
data class UserInteractionEntity(
    @PrimaryKey
    val gameId: String,
    
    // State Columns
    val isAddedToCart: Boolean = false,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Snapshot Columns
    val title: String,
    val thumbnail: String?,
    val currentPrice: Double,
    val originalPrice: Double,
    val storeId: String,

    // Sync Columns (Updated by Worker)
    val latestSyncedPrice: Double? = null,
    val lastSyncTimestamp: Long? = null
)
