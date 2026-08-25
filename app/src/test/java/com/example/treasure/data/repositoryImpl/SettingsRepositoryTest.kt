package com.example.treasure.data.repositoryImpl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var repository: SettingsRepository
    private val context: Context = mockk()
    private val dataStore: DataStore<Preferences> = mockk(relaxed = true)

    @Before
    fun setup() {
        // Mocking the extension property context.dataStore
        mockkStatic("com.example.treasure.data.repositoryImpl.SettingsRepositoryKt")
        every { context.dataStore } returns dataStore
        
        repository = SettingsRepository(context)
    }

    @Test
    fun `appTheme returns default when not set`() = runTest {
        // Prepare
        val prefs = mockk<Preferences>()
        every { prefs[any<Preferences.Key<String>>()] } returns null
        every { dataStore.data } returns flowOf(prefs)

        // Act & Assert
        repository.appTheme.collect { theme ->
            assertThat(theme).isEqualTo(AppTheme.SYSTEM)
        }
    }

    @Test
    fun `setAppTheme updates dataStore`() = runTest {
        // Prepare
        coEvery { dataStore.edit(any()) } returns mockk()

        // Act
        repository.setAppTheme(AppTheme.DARK)

        // Assert
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `isDynamicColorEnabled returns true by default`() = runTest {
        // Prepare
        val prefs = mockk<Preferences>()
        every { prefs[any<Preferences.Key<Boolean>>()] } returns null
        every { dataStore.data } returns flowOf(prefs)

        // Act & Assert
        repository.isDynamicColorEnabled.collect { enabled ->
            assertThat(enabled).isTrue()
        }
    }
}
