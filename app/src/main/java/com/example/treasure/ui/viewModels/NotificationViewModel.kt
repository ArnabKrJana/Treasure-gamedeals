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

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val db: TreasureDatabase
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = db.notificationDao().getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun clearAll() {
        viewModelScope.launch { db.notificationDao().clearAll() }
    }

    fun deleteNotification(id: Int) {
        viewModelScope.launch { db.notificationDao().deleteNotification(id) }
    }

    fun markAllAsRead() {
        viewModelScope.launch { db.notificationDao().markAllAsRead() }
    }
}
