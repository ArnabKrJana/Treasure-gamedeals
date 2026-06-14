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
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.remote.dto.DriveAccessRequest
import com.example.treasure.data.repositoryImpl.AppTheme
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.workers.PriceSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    val version: String = "1.0.0",
    val isDriveLinked: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val api: TreasureBackendApi,
    @param:ApplicationContext private val context: Context // Fixes the Kotlin 2.0 annotation warning
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.appTheme,
        repository.isDynamicColorEnabled,
        repository.areNotificationsEnabled,
        repository.syncFrequency,
        repository.notifyThreshold,
        repository.isDriveLinked
    ) { values ->
        SettingsUiState(
            theme = values[0] as AppTheme,
            dynamicColors = values[1] as Boolean,
            notificationsEnabled = values[2] as Boolean,
            syncFrequency = values[3] as Int,
            notifyThreshold = values[4] as Int,
            isDriveLinked = values[5] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    private fun safeAppTheme(themeName: String): AppTheme {
        return try {
            AppTheme.valueOf(themeName)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    }

    // --- ACTIONS ---

    fun updateTheme(theme: AppTheme) = viewModelScope.launch { repository.setAppTheme(theme) }

    fun toggleDynamicColors(enabled: Boolean) = viewModelScope.launch {
        repository.setDynamicColor(enabled)
    }

    fun toggleNotifications(enabled: Boolean) = viewModelScope.launch {
        repository.setNotificationsEnabled(enabled)
    }

    fun updateSyncFrequency(hours: Int) = viewModelScope.launch {
        repository.setSyncFrequency(hours)

        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<PriceSyncWorker>(hours.toLong(), TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

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
        val imageLoader = ImageLoader(context)
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    // --- GOOGLE DRIVE ACTIONS ---

    fun unlinkDrive() = viewModelScope.launch {
        repository.setDriveLinked(false)
    }

    fun linkDriveAccount(serverAuthCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = api.linkGoogleDrive(DriveAccessRequest(serverAuthCode))
                if (response.isSuccessful && response.body()?.driveLinked == true) {
                    repository.setDriveLinked(true)
                }
            } catch (_: Exception) {
                // Handled gracefully by UI remaining unlinked
            }
        }
    }

    fun setDriveNeedsAuth() = viewModelScope.launch {
        repository.setDriveLinked(false)
    }
}