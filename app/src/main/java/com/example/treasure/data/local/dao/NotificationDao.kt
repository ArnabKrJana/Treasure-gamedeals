package com.example.treasure.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treasure.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<@JvmSuppressWildcards List<NotificationEntity>>

    // FIX 1: Add @JvmSuppressWildcards to Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): @JvmSuppressWildcards Long

    // FIX 2: Add @JvmSuppressWildcards to Int
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int): @JvmSuppressWildcards Int

    // FIX 3: Add @JvmSuppressWildcards to Int
    @Query("DELETE FROM notifications")
    suspend fun clearAll(): @JvmSuppressWildcards Int

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    // FIX 4: Add @JvmSuppressWildcards to Int
    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead(): @JvmSuppressWildcards Int
}