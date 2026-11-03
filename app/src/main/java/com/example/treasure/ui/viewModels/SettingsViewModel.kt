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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * State representation for the user preferences and settings interface.
 *
 * @property theme Currently selected application theme mode (LIGHT, DARK, or SYSTEM).
 * @property dynamicColors Whether dynamic Material You color generation is enabled.
 * @property notificationsEnabled Whether local price drop alerts are active.
 * @property syncFrequency Interval in hours between background price synchronization checks.
 * @property notifyThreshold Price drop percentage threshold required to trigger an alert.
 * @property version Current semantic version of the installed application build.
 * @property isDriveLinked Whether a Google Drive account is actively linked for cloud backups.
 */
data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColors: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val syncFrequency: Int = 8,
    val notifyThreshold: Int = 3,
    val version: String = "1.0.0",
    val isDriveLinked: Boolean = false
)

/**
 * ViewModel managing user preferences, WorkManager background schedule reconfiguration,
 * image cache eviction, and Google Drive account linking.
 *
 * Combines multiple DataStore preference flows into a consolidated [SettingsUiState].
 *
 * @property repository Settings repository managing DataStore preferences.
 * @property api Backend API service executing Google Drive authorization requests.
 * @param context Application context used for WorkManager and Coil ImageLoader access.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val api: TreasureBackendApi,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * Combined observable state representing all active user settings.
     */
    val uiState: StateFlow<SettingsUiState> = combine<Any, SettingsUiState>(
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

    /**
     * Updates the persistent application theme preference.
     *
     * @param theme The target [AppTheme] mode.
     */
    fun updateTheme(theme: AppTheme) = viewModelScope.launch { repository.setAppTheme(theme) }

    /**
     * Toggles whether Material You dynamic colors are extracted from device wallpaper.
     *
     * @param enabled True to enable dynamic wallpaper palettes.
     */
    fun toggleDynamicColors(enabled: Boolean) = viewModelScope.launch {
        repository.setDynamicColor(enabled)
    }

    /**
     * Toggles whether background price drop notifications are delivered.
     *
     * @param enabled True to allow notifications.
     */
    fun toggleNotifications(enabled: Boolean) = viewModelScope.launch {
        repository.setNotificationsEnabled(enabled)
    }

    /**
     * Updates the background price sync cadence and reschedules the periodic [PriceSyncWorker].
     *
     * @param hours Interval in hours between consecutive background sync executions.
     */
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

    /**
     * Updates the minimum price drop percentage required to trigger a user notification.
     *
     * @param percent Percentage drop threshold (e.g., 3%).
     */
    fun updateNotifyThreshold(percent: Int) = viewModelScope.launch {
        repository.setNotifyThreshold(percent)
    }

    /**
     * Evicts all cached images from both memory and disk caches maintained by Coil.
     */
    fun clearImageCache() {
        val imageLoader = ImageLoader(context)
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    /**
     * Clears the Google Drive linked status in local preferences.
     */
    fun unlinkDrive() = viewModelScope.launch {
        repository.setDriveLinked(false)
    }

    /**
     * Exchanges an OAuth server authorization code for backend Google Drive access and updates local status.
     *
     * @param serverAuthCode Google OAuth authorization code.
     */
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

    /**
     * Resets the Google Drive linked flag when backend tokens expire or are revoked.
     */
    fun setDriveNeedsAuth() = viewModelScope.launch {
        repository.setDriveLinked(false)
    }
}
