//package com.example.treasure.data.repositoryImpl
//
//import android.content.Context
//import androidx.datastore.core.DataStore
//import androidx.datastore.preferences.core.*
//import androidx.datastore.preferences.preferencesDataStore
//import dagger.hilt.android.qualifiers.ApplicationContext
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//import javax.inject.Inject
//import javax.inject.Singleton
//
//// Extension for singleton DataStore
//val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "treasure_settings")
//
//@Singleton
//class SettingsRepository @Inject constructor(
//    @ApplicationContext private val context: Context
//) {
//    private val dataStore = context.dataStore
//
//    // --- KEYS ---
//    private object Keys {
//        val APP_THEME = stringPreferencesKey("app_theme") // "SYSTEM", "LIGHT", "DARK"
//        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors") // Material You (Wallpaper)
//        val CARD_DYNAMIC_ART = booleanPreferencesKey("card_dynamic_art") // Palette API (Game Art)
//        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
//        val SYNC_FREQUENCY = intPreferencesKey("sync_frequency") // Hours (e.g., 8)
//        val NOTIFY_THRESHOLD = intPreferencesKey("notify_threshold") // Percent (e.g., 3, 10, 25)
//    }
//
//    // --- READERS ---
//    val appTheme: Flow<AppTheme> = dataStore.data.map { prefs ->
//        AppTheme.valueOf(prefs[Keys.APP_THEME] ?: AppTheme.SYSTEM.name)
//    }
//
//    val isDynamicColorEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
//        prefs[Keys.DYNAMIC_COLORS] ?: true
//    }
//
//    val isCardDynamicArtEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
//        prefs[Keys.CARD_DYNAMIC_ART] ?: true
//    }
//
//    val areNotificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
//        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
//    }
//
//    val syncFrequency: Flow<Int> = dataStore.data.map { prefs ->
//        prefs[Keys.SYNC_FREQUENCY] ?: 8
//    }
//
//    val notifyThreshold: Flow<Int> = dataStore.data.map { prefs ->
//        prefs[Keys.NOTIFY_THRESHOLD] ?: 3 // Default 3%
//    }
//
//    // --- WRITERS ---
//    suspend fun setAppTheme(theme: AppTheme) {
//        dataStore.edit { it[Keys.APP_THEME] = theme.name }
//    }
//
//    suspend fun setDynamicColor(enabled: Boolean) {
//        dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
//    }
//
//    suspend fun setCardDynamicArt(enabled: Boolean) {
//        dataStore.edit { it[Keys.CARD_DYNAMIC_ART] = enabled }
//    }
//
//    suspend fun setNotificationsEnabled(enabled: Boolean) {
//        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
//    }
//
//    suspend fun setSyncFrequency(hours: Int) {
//        dataStore.edit { it[Keys.SYNC_FREQUENCY] = hours }
//    }
//
//    suspend fun setNotifyThreshold(percent: Int) {
//        dataStore.edit { it[Keys.NOTIFY_THRESHOLD] = percent }
//    }
//}
//
//enum class AppTheme(val label: String) {
//    SYSTEM("System Default"),
//    LIGHT("Light Mode"),
//    DARK("Dark Mode")
//}

package com.example.treasure.data.repositoryImpl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension for singleton DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "treasure_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // --- KEYS ---
    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val CARD_DYNAMIC_ART = booleanPreferencesKey("card_dynamic_art")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SYNC_FREQUENCY = intPreferencesKey("sync_frequency")
        val NOTIFY_THRESHOLD = intPreferencesKey("notify_threshold")

        // 🚀 ADDED: Drive Sync Key
        val DRIVE_LINKED = booleanPreferencesKey("drive_linked")
    }

    // --- READERS ---
    val appTheme: Flow<AppTheme> = dataStore.data.map { prefs ->
        AppTheme.valueOf(
            prefs[Keys.APP_THEME] ?: AppTheme.SYSTEM.name
        )
    }

    val isDynamicColorEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLORS] ?: true
    }

    val isCardDynamicArtEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.CARD_DYNAMIC_ART] ?: true
    }

    val areNotificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val syncFrequency: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.SYNC_FREQUENCY] ?: 8
    }

    val notifyThreshold: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.NOTIFY_THRESHOLD] ?: 3
    }

    // 🚀 ADDED: Drive Linked Observer
    val isDriveLinked: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DRIVE_LINKED] ?: false
    }

    // --- WRITERS ---
    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit {
            it[Keys.APP_THEME] = theme.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
    }

    suspend fun setCardDynamicArt(enabled: Boolean) {
        dataStore.edit { it[Keys.CARD_DYNAMIC_ART] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setSyncFrequency(hours: Int) {
        dataStore.edit { it[Keys.SYNC_FREQUENCY] = hours }
    }

    suspend fun setNotifyThreshold(percent: Int) {
        dataStore.edit { it[Keys.NOTIFY_THRESHOLD] = percent }
    }

    // 🚀 ADDED: Drive Linked Setter
    suspend fun setDriveLinked(enabled: Boolean) {
        dataStore.edit { it[Keys.DRIVE_LINKED] = enabled }
    }
}