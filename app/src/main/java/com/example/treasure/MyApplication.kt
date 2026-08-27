package com.example.treasure

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.asFlow
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

        Log.d("PeriodicWork", "========== APPLICATION STARTED ==========")

        setupPeriodicWork()

        // Enable this while debugging the price-sync problem.
        observeWorkStatus()
    }

    private fun setupPeriodicWork() {

        Log.d(
            "PeriodicWork",
            "setupPeriodicWork() called"
        )

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val savedFrequency =
                    settingsRepository.syncFrequency.first()

                Log.d(
                    "PeriodicWork",
                    "Saved sync frequency: $savedFrequency hours"
                )

//                val workRequest =
//                    PeriodicWorkRequestBuilder<PriceSyncWorker>(
//                        savedFrequency.toLong(),
//                        TimeUnit.HOURS
//                    )
//                        .setBackoffCriteria(
//                            BackoffPolicy.EXPONENTIAL,
//                            15,
//                            TimeUnit.MINUTES
//                        )
//                        .build()
                val workRequest =
                    PeriodicWorkRequestBuilder<PriceSyncWorker>(
                        15,
                        TimeUnit.MINUTES
                    )
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

    private fun observeWorkStatus() {

        Log.d(
            "WorkManagerStatus",
            "Starting WorkManager observer"
        )

        val workManager = WorkManager.getInstance(this)

        workManager
            .getWorkInfosForUniqueWorkLiveData("PriceSyncWork")
            .asFlow()
            .onEach { workInfos ->

                if (workInfos.isNullOrEmpty()) {

                    Log.d(
                        "WorkManagerStatus",
                        "PriceSyncWork: NO WORK FOUND"
                    )

                } else {

                    workInfos.forEach { workInfo ->

                        Log.d(
                            "WorkManagerStatus",
                            """
                            --------------------------------
                            WorkManager Status
                            ID: ${workInfo.id}
                            State: ${workInfo.state}
                            Tags: ${workInfo.tags}
                            RunAttemptCount: ${workInfo.runAttemptCount}
                            --------------------------------
                            """.trimIndent()
                        )
                    }
                }

            }
            .launchIn(
                CoroutineScope(Dispatchers.Main)
            )
    }
}