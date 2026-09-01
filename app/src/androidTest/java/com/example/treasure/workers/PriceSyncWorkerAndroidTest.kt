package com.example.treasure.workers

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.utils.PriceNotificationManager
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PriceSyncWorkerAndroidTest {

    private lateinit var context: Context
    private lateinit var database: TreasureDatabase
    private lateinit var workManager: WorkManager
    private var testDriver: TestDriver? = null

    private val activeWorkNames = mutableListOf<String>()

    private val api: TreasureBackendApi = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val priceNotificationManager: PriceNotificationManager = mockk(relaxed = true)

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Use a fresh in-memory Room database for fast, isolated instrumented tests
        database = Room.inMemoryDatabaseBuilder(context, TreasureDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)

        val testWorkerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
                return if (workerClassName == PriceSyncWorker::class.java.name) {
                    PriceSyncWorker(
                        appContext,
                        workerParameters,
                        database,
                        api,
                        settingsRepository,
                        priceNotificationManager,
                        Dispatchers.IO
                    )
                } else {
                    null
                }
            }
        }

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setWorkerFactory(testWorkerFactory)
            .setExecutor(SynchronousExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        testDriver = WorkManagerTestInitHelper.getTestDriver(context)
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        activeWorkNames.forEach { workManager.cancelUniqueWork(it) }
        activeWorkNames.clear()
        database.close()
    }

    /**
     * Polls [WorkManager] until the execution of [id] is no longer RUNNING.
     */
    private suspend fun awaitExecutionComplete(
        id: UUID,
        timeoutMs: Long = 5000
    ): WorkInfo? {
        val startTime = System.currentTimeMillis()
        var info: WorkInfo? = null
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            info = workManager.getWorkInfoById(id).get()
            if (info != null && info.state != WorkInfo.State.RUNNING) {
                return info
            }
            delay(50)
        }
        return info ?: workManager.getWorkInfoById(id).get()
    }

    @Test
    fun periodicWorkerExecutesAndStaysEnqueuedOnFirstRun() = runBlocking {
        val testWorkName = "PriceSyncWorkerTest-${UUID.randomUUID()}"
        activeWorkNames.add(testWorkName)

        val request = PeriodicWorkRequestBuilder<PriceSyncWorker>(15, TimeUnit.MINUTES).build()

        workManager.enqueueUniquePeriodicWork(
            testWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        // Mark the 15-minute period delay as met so execution runs immediately
        testDriver?.setPeriodDelayMet(request.id)

        // Deterministically wait for worker execution to complete
        val workInfo = awaitExecutionComplete(request.id)

        // Periodic work transitions back to ENQUEUED after a successful run
        assertThat(workInfo?.state).isEqualTo(WorkInfo.State.ENQUEUED)

        // Verify backend was NOT called on 0 favorites
        coVerify(exactly = 0) { api.getWishlistPrices() }
    }

    @Test
    fun workerRun1WithZeroFavoritesSucceedsAndWorkerRun2ExecutesWhenFavoritesPopulated() = runBlocking {
        val testWorkName = "PriceSyncWorkerTest-${UUID.randomUUID()}"
        activeWorkNames.add(testWorkName)

        val request = PeriodicWorkRequestBuilder<PriceSyncWorker>(15, TimeUnit.MINUTES).build()

        workManager.enqueueUniquePeriodicWork(
            testWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        // --- RUN 1: Zero favorites in database ---
        testDriver?.setPeriodDelayMet(request.id)

        val workInfoRun1 = awaitExecutionComplete(request.id)
        assertThat(workInfoRun1?.state).isEqualTo(WorkInfo.State.ENQUEUED)

        // Backend should NOT have been called during Run 1
        coVerify(exactly = 0) { api.getWishlistPrices() }

        // --- SIMULATE USER LOGIN / CLOUD WISHLIST POPULATION ---
        database.userInteractionDao().insertInteraction(
            UserInteractionEntity(
                gameId = "game_100",
                title = "Hades II",
                thumbnail = null,
                currentPrice = 100.0,
                originalPrice = 100.0,
                storeId = "steam",
                isFavorite = true,
                latestSyncedPrice = 100.0
            )
        )

        // Mock backend price drop: 100.0 -> 80.0 (20% drop, > 3% threshold)
        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("game_100" to """{"current":80.0,"original":100.0}""")
        )

        // --- RUN 2: Make next period eligible immediately on the SAME PeriodicWorkRequest ---
        testDriver?.setPeriodDelayMet(request.id)

        val workInfoRun2 = awaitExecutionComplete(request.id)
        assertThat(workInfoRun2?.state).isEqualTo(WorkInfo.State.ENQUEUED)

        // VERIFY RUN 2 SIDE EFFECTS:
        // 1. Backend was called during Run 2
        coVerify(exactly = 1) { api.getWishlistPrices() }

        // 2. Local database price was updated to 80.0
        val updatedGame = database.userInteractionDao().getInteractionForGame("game_100")
        assertThat(updatedGame?.latestSyncedPrice).isEqualTo(80.0)

        // 3. Notification record was inserted into Room
        val notifications = database.notificationDao().getAllNotifications().first()
        assertThat(notifications).hasSize(1)
        assertThat(notifications[0].gameId).isEqualTo("game_100")
        assertThat(notifications[0].newPrice).isEqualTo(80.0)

        // 4. Android notification was requested via PriceNotificationManager
        verify(exactly = 1) {
            priceNotificationManager.showSingleGameNotification(match { it.gameId == "game_100" })
        }
    }
}
