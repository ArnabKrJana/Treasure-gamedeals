package com.example.treasure.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treasure.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing price fluctuation notifications.
 *
 * Provides real-time reactive streams of price alert history and unread badge counts
 * consumed by [com.example.treasure.ui.viewModels.NotificationViewModel].
 */
@Dao
interface NotificationDao {

    /**
     * Observes all price notification events ordered by creation timestamp in descending order.
     *
     * @return Cold [Flow] emitting the updated list of [NotificationEntity] records.
     */
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<@JvmSuppressWildcards List<NotificationEntity>>

    /**
     * Inserts or updates a price fluctuation notification entry.
     *
     * @param notification The [NotificationEntity] to persist.
     * @return Generated notification row identifier.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): @JvmSuppressWildcards Long

    /**
     * Deletes a specific notification by its unique ID.
     *
     * @param id Identifier of the notification record to remove.
     * @return The number of affected database rows.
     */
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int): @JvmSuppressWildcards Int

    /**
     * Removes all notification records from local persistence.
     *
     * @return The number of affected database rows.
     */
    @Query("DELETE FROM notifications")
    suspend fun clearAll(): @JvmSuppressWildcards Int

    /**
     * Observes the count of unread notifications to drive badge indicators on navigation icons.
     *
     * @return Cold [Flow] emitting the current number of unread alerts.
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    /**
     * Marks all unread notification entries as read.
     *
     * @return The number of updated database rows.
     */
    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead(): @JvmSuppressWildcards Int
}
