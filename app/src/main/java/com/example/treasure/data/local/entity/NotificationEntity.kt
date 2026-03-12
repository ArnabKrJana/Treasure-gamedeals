package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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