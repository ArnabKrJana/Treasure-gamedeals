package com.example.treasure

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.utils.TokenManager
import com.example.treasure.workers.PriceSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Custom [Application] class initializing Hilt dependency injection, custom WorkManager
 * worker factories, and bootstrapping background price sync periodic work.
 *
 * Implements [Configuration.Provider] to wire [HiltWorkerFactory] into AndroidX WorkManager.
 */
@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.d("PeriodicWork", "Application initialized; setting up background periodic sync.")
        setupPeriodicWork()
    }

    /**
     * Reads the persisted sync frequency setting from [SettingsRepository] and registers
     * a unique periodic [PriceSyncWorker] with exponential backoff and network/battery constraints.
     */
    private fun setupPeriodicWork() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val savedFrequency = settingsRepository.syncFrequency.first()

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val workRequest =
                    PeriodicWorkRequestBuilder<PriceSyncWorker>(
                        savedFrequency.toLong(),
                        TimeUnit.HOURS
                    )
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            15,
                            TimeUnit.MINUTES
                        )
                        .build()

                WorkManager
                    .getInstance(this@MyApplication)
                    .enqueueUniquePeriodicWork(
                        "PriceSyncWork",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        workRequest
                    )

                Log.d("PeriodicWork", "PriceSyncWork successfully enqueued with interval: $savedFrequency hours.")
            } catch (e: Exception) {
                Log.e("PeriodicWork", "Failed to setup periodic work", e)
            }
        }
    }
}
