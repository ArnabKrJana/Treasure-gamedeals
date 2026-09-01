package com.example.treasure.data.repositoryImpl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    private lateinit var repository: SettingsRepository
    private lateinit var dataStore: DataStore<Preferences>

    private val context: Context = mockk()

    @Before
    fun setup() {

        // Create a real DataStore in a temporary test file.
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(
                    System.getProperty("java.io.tmpdir"),
                    "settings_test_${System.nanoTime()}.preferences_pb"
                )
            }
        )

        /*
         * Mock ONLY the Context.dataStore extension.
         *
         * We are NOT mocking DataStore itself.
         * This means dataStore.data and dataStore.edit()
         * execute their real implementations.
         */
        mockkStatic(
            "com.example.treasure.data.repositoryImpl.SettingsRepositoryKt"
        )

        every {
            context.dataStore
        } returns dataStore

        repository = SettingsRepository(context)
    }

    @After
    fun tearDown() {
        unmockkStatic(
            "com.example.treasure.data.repositoryImpl.SettingsRepositoryKt"
        )
    }

    @Test
    fun `appTheme returns SYSTEM by default`() = runTest {

        val result = repository.appTheme.first()

        assertThat(result).isEqualTo(AppTheme.SYSTEM)
    }

    @Test
    fun `appTheme returns saved DARK theme`() = runTest {

        dataStore.edit {
            it[stringPreferencesKey("app_theme")] = AppTheme.DARK.name
        }

        val result = repository.appTheme.first()

        assertThat(result).isEqualTo(AppTheme.DARK)
    }

    @Test
    fun `appTheme returns saved LIGHT theme`() = runTest {

        dataStore.edit {
            it[stringPreferencesKey("app_theme")] = AppTheme.LIGHT.name
        }

        val result = repository.appTheme.first()

        assertThat(result).isEqualTo(AppTheme.LIGHT)
    }

    @Test
    fun `isDynamicColorEnabled returns true by default`() = runTest {

        val result = repository.isDynamicColorEnabled.first()

        assertThat(result).isTrue()
    }

    @Test
    fun `isDynamicColorEnabled returns false when disabled`() = runTest {

        dataStore.edit {
            it[androidx.datastore.preferences.core.booleanPreferencesKey(
                "dynamic_colors"
            )] = false
        }

        val result = repository.isDynamicColorEnabled.first()

        assertThat(result).isFalse()
    }

    @Test
    fun `isCardDynamicArtEnabled returns true by default`() = runTest {

        val result = repository.isCardDynamicArtEnabled.first()

        assertThat(result).isTrue()
    }

    @Test
    fun `isCardDynamicArtEnabled returns false when disabled`() = runTest {

        dataStore.edit {
            it[androidx.datastore.preferences.core.booleanPreferencesKey(
                "card_dynamic_art"
            )] = false
        }

        val result = repository.isCardDynamicArtEnabled.first()

        assertThat(result).isFalse()
    }

    @Test
    fun `areNotificationsEnabled returns true by default`() = runTest {

        val result = repository.areNotificationsEnabled.first()

        assertThat(result).isTrue()
    }

    @Test
    fun `areNotificationsEnabled returns false when disabled`() = runTest {

        dataStore.edit {
            it[androidx.datastore.preferences.core.booleanPreferencesKey(
                "notifications_enabled"
            )] = false
        }

        val result = repository.areNotificationsEnabled.first()

        assertThat(result).isFalse()
    }

    @Test
    fun `syncFrequency returns 8 by default`() = runTest {

        val result = repository.syncFrequency.first()

        assertThat(result).isEqualTo(8)
    }

    @Test
    fun `syncFrequency returns saved value`() = runTest {

        dataStore.edit {
            it[androidx.datastore.preferences.core.intPreferencesKey(
                "sync_frequency"
            )] = 12
        }

        val result = repository.syncFrequency.first()

        assertThat(result).isEqualTo(12)
    }

    @Test
    fun `notifyThreshold returns 3 by default`() = runTest {

        val result = repository.notifyThreshold.first()

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun `notifyThreshold returns saved value`() = runTest {

        dataStore.edit {
            it[androidx.datastore.preferences.core.intPreferencesKey(
                "notify_threshold"
            )] = 25
        }

        val result = repository.notifyThreshold.first()

        assertThat(result).isEqualTo(25)
    }

    @Test
    fun `isDriveLinked returns false by default`() = runTest {

        val result = repository.isDriveLinked.first()

        assertThat(result).isFalse()
    }

    @Test
    fun `isDriveLinked returns true when linked`() = runTest {

        dataStore.edit {
            it[androidx.datastore.preferences.core.booleanPreferencesKey(
                "drive_linked"
            )] = true
        }

        val result = repository.isDriveLinked.first()

        assertThat(result).isTrue()
    }

    // ---------------------------------------------------------
    // WRITER TESTS
    // ---------------------------------------------------------

    @Test
    fun `setAppTheme saves theme`() = runTest {

        repository.setAppTheme(AppTheme.DARK)

        val result = repository.appTheme.first()

        assertThat(result).isEqualTo(AppTheme.DARK)
    }

    @Test
    fun `setDynamicColor saves value`() = runTest {

        repository.setDynamicColor(false)

        val result = repository.isDynamicColorEnabled.first()

        assertThat(result).isFalse()
    }

    @Test
    fun `setCardDynamicArt saves value`() = runTest {

        repository.setCardDynamicArt(false)

        val result = repository.isCardDynamicArtEnabled.first()

        assertThat(result).isFalse()
    }

    @Test
    fun `setNotificationsEnabled saves value`() = runTest {

        repository.setNotificationsEnabled(false)

        val result = repository.areNotificationsEnabled.first()

        assertThat(result).isFalse()
    }

    @Test
    fun `setSyncFrequency saves value`() = runTest {

        repository.setSyncFrequency(24)

        val result = repository.syncFrequency.first()

        assertThat(result).isEqualTo(24)
    }

    @Test
    fun `setNotifyThreshold saves value`() = runTest {

        repository.setNotifyThreshold(25)

        val result = repository.notifyThreshold.first()

        assertThat(result).isEqualTo(25)
    }

    @Test
    fun `setDriveLinked saves value`() = runTest {

        repository.setDriveLinked(true)

        val result = repository.isDriveLinked.first()

        assertThat(result).isTrue()
    }
}