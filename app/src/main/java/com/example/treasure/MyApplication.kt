package com.example.treasure

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.asFlow
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.utils.TokenManager
import com.example.treasure.workers.PriceSyncWorker
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject




@HiltAndroidApp
class MyApplication: Application(), Configuration.Provider{
    @Inject lateinit var tokenManager: TokenManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Inject repository to read saved frequency
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Remote Config
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0 // Fetch once per hour
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // 2. Fetch and Activate
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d("RemoteConfig", "Config params updated: $updated")

                    // 3. Get Key and Save Securely
                    val apiKey = remoteConfig.getString("itad_api_key")

                    if (apiKey.isNotBlank()) {
                        tokenManager.saveApiKey(apiKey)
                        Log.d("RemoteConfig", "API Key saved securely")
                    }
                } else {
                    Log.e("RemoteConfig", "Fetch failed")
                    // Fallback: Use local backup if needed, or handle error
                }
            }

        setupPeriodicWork()
       // observeWorkStatus()
    }

    private fun setupPeriodicWork() {
        // Use IO Dispatcher to read from DataStore without blocking Main Thread
        CoroutineScope(Dispatchers.IO).launch {
            // Fetch saved frequency, default is 8 if not set
            val savedFrequency = settingsRepository.syncFrequency.first()

            val workRequest = PeriodicWorkRequestBuilder<PriceSyncWorker>(savedFrequency.toLong(), TimeUnit.HOURS)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(this@MyApplication).enqueueUniquePeriodicWork(
                "PriceSyncWork",
                ExistingPeriodicWorkPolicy.UPDATE, // UPDATE policy ensures if logic changes, worker is updated
                workRequest
            )
        }
    }

    private fun observeWorkStatus() {
        val workManager = WorkManager.getInstance(this)
        // Observe by unique name
        CoroutineScope(Dispatchers.Main).launch {
            workManager.getWorkInfosForUniqueWorkLiveData("PriceSyncWork")
                .asFlow()
                .collect { workInfos ->
                    workInfos?.forEach { workInfo ->
                        Log.d("WorkManagerStatus", "Work: ${workInfo.tags} | State: ${workInfo.state}")
                    }
                }
        }
    }
}