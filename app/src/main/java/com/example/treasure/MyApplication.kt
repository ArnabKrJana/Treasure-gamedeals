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

        Log.d(
            "PeriodicWork",
            "========== APPLICATION STARTED =========="
        )

        setupPeriodicWork()
    }

    private fun setupPeriodicWork() {

        Log.d(
            "PeriodicWork",
            "setupPeriodicWork() called"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {

                /*
                 * Read the user's saved sync frequency.
                 *
                 * Supported values from SettingsViewModel:
                 * 4, 8, 12, 24 hours.
                 */
                val savedFrequency =
                    settingsRepository.syncFrequency.first()

                Log.d(
                    "PeriodicWork",
                    "Saved sync frequency: $savedFrequency hours"
                )

                /*
                 * Same execution precautions used when the
                 * user changes the sync frequency in Settings.
                 */
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()

                /*
                 * Production periodic work.
                 *
                 * IMPORTANT:
                 * The period comes from the user's setting.
                 * Do not hardcode 15 minutes here.
                 */
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

                Log.d(
                    "PeriodicWork",
                    "Created PriceSyncWorker request"
                )

                Log.d(
                    "PeriodicWork",
                    "WorkRequest ID: ${workRequest.id}"
                )

                Log.d(
                    "PeriodicWork",
                    "Sync interval: $savedFrequency hours"
                )

                Log.d(
                    "PeriodicWork",
                    "Constraints: Network CONNECTED + Battery NOT LOW"
                )

                WorkManager
                    .getInstance(this@MyApplication)
                    .enqueueUniquePeriodicWork(
                        "PriceSyncWork",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        workRequest
                    )

                Log.d(
                    "PeriodicWork",
                    "PriceSyncWork successfully enqueued"
                )

            } catch (e: Exception) {

                Log.e(
                    "PeriodicWork",
                    "FAILED to setup periodic work",
                    e
                )
            }
        }
    }
}