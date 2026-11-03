package com.example.treasure.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.entity.NotificationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing price drop and deal alerts, unread counts, and notification lifecycle.
 *
 * Exposes reactive Room flows observing user alerts and provides actions to dismiss or mark notifications as read.
 *
 * @property db Room database instance providing access to the underlying notification DAO.
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val db: TreasureDatabase
) : ViewModel() {

    /**
     * Observable stream of all persisted notifications ordered from newest to oldest.
     */
    val notifications: StateFlow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Observable count of unread notifications, used to populate app bar badges.
     */
    val unreadCount: StateFlow<Int> = db.notificationDao().getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Deletes all notifications from Room persistence.
     */
    fun clearAll() {
        viewModelScope.launch { db.notificationDao().clearAll() }
    }

    /**
     * Deletes a specific notification by its database identifier.
     *
     * @param id The primary key identifier of the notification to remove.
     */
    fun deleteNotification(id: Int) {
        viewModelScope.launch { db.notificationDao().deleteNotification(id) }
    }

    /**
     * Marks all unread notifications as read.
     */
    fun markAllAsRead() {
        viewModelScope.launch { db.notificationDao().markAllAsRead() }
    }
}
