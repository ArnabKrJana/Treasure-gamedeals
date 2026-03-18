package com.example.treasure.ui.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil3.ImageLoader
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.repositoryImpl.AppTheme
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.workers.PriceSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColors: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val syncFrequency: Int = 8,
    val notifyThreshold: Int = 3,
    val version: String = "1.0.0"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val database: TreasureDatabase, // To clear DB cache if needed
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.appTheme,
        repository.isDynamicColorEnabled,
        repository.areNotificationsEnabled,
        repository.syncFrequency,
        repository.notifyThreshold
    ) { theme, dynamic, notifs, freq, threshold ->
        SettingsUiState(theme, dynamic, notifs, freq, threshold)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // --- ACTIONS ---
    fun updateTheme(theme: AppTheme) = viewModelScope.launch {
        repository.setAppTheme(theme)
    }

    fun toggleDynamicColors(enabled: Boolean) = viewModelScope.launch {
        repository.setDynamicColor(enabled)
    }

    fun toggleNotifications(enabled: Boolean) = viewModelScope.launch {
        repository.setNotificationsEnabled(enabled)
    }


    fun updateSyncFrequency(hours: Int) = viewModelScope.launch {
        // 1. Save to Preferences
        repository.setSyncFrequency(hours)

        // 2. Re-schedule WorkManager immediately
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<PriceSyncWorker>(hours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.MINUTES
            )
            .build()

        // "UPDATE" policy keeps the worker unique but replaces the spec
        workManager.enqueueUniquePeriodicWork(
            "PriceSyncWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun updateNotifyThreshold(percent: Int) = viewModelScope.launch {
        repository.setNotifyThreshold(percent)
    }

    fun clearImageCache() {
        // Coil 3 Image Loader clearing
        val imageLoader = ImageLoader(context)
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}