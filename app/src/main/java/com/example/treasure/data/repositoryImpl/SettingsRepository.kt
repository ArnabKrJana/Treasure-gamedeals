package com.example.treasure.data.repositoryImpl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Top-level delegate creating a singleton [DataStore] preferences instance dedicated to user settings.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "treasure_settings")

/**
 * Repository managing user configuration and persistent preferences via Jetpack [DataStore] Preferences.
 *
 * Persists and exposes reactive [Flow] streams for:
 * - Application theme selection ([AppTheme])
 * - Material You dynamic wallpaper theming
 * - Key art dynamic palette extraction
 * - Background price fluctuation notifications
 * - Background synchronization polling frequency
 * - Price drop percentage alert thresholds
 * - Google Drive cloud backup authorization status
 *
 * @param context Injected application context used to access the preference DataStore.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    /**
     * Preference keys identifying persisted configuration values in DataStore.
     */
    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val CARD_DYNAMIC_ART = booleanPreferencesKey("card_dynamic_art")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SYNC_FREQUENCY = intPreferencesKey("sync_frequency")
        val NOTIFY_THRESHOLD = intPreferencesKey("notify_threshold")
        val DRIVE_LINKED = booleanPreferencesKey("drive_linked")
    }

    /**
     * Observes the active user interface theme preference. Defaults to [AppTheme.SYSTEM].
     */
    val appTheme: Flow<AppTheme> = dataStore.data.map { prefs ->
        AppTheme.valueOf(
            prefs[Keys.APP_THEME] ?: AppTheme.SYSTEM.name
        )
    }

    /**
     * Observes whether Android Material You wallpaper dynamic theming is enabled. Defaults to `true`.
     */
    val isDynamicColorEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLORS] ?: true
    }

    /**
     * Observes whether dynamic color palette extraction from game cover art is enabled. Defaults to `true`.
     */
    val isCardDynamicArtEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.CARD_DYNAMIC_ART] ?: true
    }

    /**
     * Observes whether background price fluctuation push notifications are enabled. Defaults to `true`.
     */
    val areNotificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * Observes the periodic background price sync worker execution interval in hours. Defaults to `8`.
     */
    val syncFrequency: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.SYNC_FREQUENCY] ?: 8
    }

    /**
     * Observes the minimum price drop percentage threshold required to trigger an alert. Defaults to `3`%.
     */
    val notifyThreshold: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.NOTIFY_THRESHOLD] ?: 3
    }

    /**
     * Observes whether Google Drive cloud storage is linked for wallpaper backups. Defaults to `false`.
     */
    val isDriveLinked: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DRIVE_LINKED] ?: false
    }

    /**
     * Updates the persistent application visual theme preference.
     *
     * @param theme The selected [AppTheme].
     */
    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit {
            it[Keys.APP_THEME] = theme.name
        }
    }

    /**
     * Toggles the Material You dynamic color preference.
     *
     * @param enabled `true` to enable dynamic colors, `false` to disable.
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
    }

    /**
     * Toggles game key art dynamic color palette extraction.
     *
     * @param enabled `true` to enable dynamic card colors, `false` to disable.
     */
    suspend fun setCardDynamicArt(enabled: Boolean) {
        dataStore.edit { it[Keys.CARD_DYNAMIC_ART] = enabled }
    }

    /**
     * Toggles background price alert notifications.
     *
     * @param enabled `true` to permit alerts, `false` to suppress.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    /**
     * Configures the periodic background price sync worker recurrence interval.
     *
     * @param hours Interval in hours between background checks.
     */
    suspend fun setSyncFrequency(hours: Int) {
        dataStore.edit { it[Keys.SYNC_FREQUENCY] = hours }
    }

    /**
     * Configures the percentage price drop required to trigger a notification.
     *
     * @param percent Minimum percentage threshold (e.g., 3, 10, 25).
     */
    suspend fun setNotifyThreshold(percent: Int) {
        dataStore.edit { it[Keys.NOTIFY_THRESHOLD] = percent }
    }

    /**
     * Updates the Google Drive authorization linkage status.
     *
     * @param enabled `true` if Google Drive has been linked, `false` otherwise.
     */
    suspend fun setDriveLinked(enabled: Boolean) {
        dataStore.edit { it[Keys.DRIVE_LINKED] = enabled }
    }
}
