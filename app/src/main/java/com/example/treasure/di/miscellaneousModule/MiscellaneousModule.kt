package com.example.treasure.di.miscellaneousModule

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Dagger Hilt dependency injection module provisioning cross-cutting infrastructural components.
 *
 * Provides the application's I/O [CoroutineDispatcher] and hardware-backed [EncryptedSharedPreferences]
 * for secure token and credential persistence.
 */
@Module
@InstallIn(SingletonComponent::class)
object MiscellaneousModule {

    private const val PREFS_FILE_NAME = "secure_prefs"

    /**
     * Provisions the default [CoroutineDispatcher] assigned to disk I/O and network operations.
     *
     * @return [Dispatchers.IO] coroutine dispatcher.
     */
    @Provides
    @Singleton
    fun providesCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Constructs and provisions a hardware-backed [EncryptedSharedPreferences] instance.
     *
     * Encrypts preferences using an AES256-GCM MasterKey managed by Android Keystore, pairing
     * AES256-SIV key encryption with AES256-GCM value encryption.
     *
     * @param context Host application context.
     * @return Hardware-encrypted [SharedPreferences] instance.
     */
    @Provides
    @Singleton
    fun provideSharedPreference(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
