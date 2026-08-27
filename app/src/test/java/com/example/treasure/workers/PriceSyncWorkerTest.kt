package com.example.treasure.workers

import android.app.NotificationManager
import android.content.Context
import androidx.room.withTransaction
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.dao.NotificationDao
import com.example.treasure.data.local.dao.UserInteractionDao
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PriceSyncWorkerTest {

    private lateinit var worker: PriceSyncWorker

    private val context: Context = mockk(relaxed = true)
    private val workerParams: WorkerParameters = mockk(relaxed = true)

    private val database: TreasureDatabase = mockk()
    private val api: TreasureBackendApi = mockk()
    private val settingsRepository: SettingsRepository = mockk()

    private val userInteractionDao: UserInteractionDao =
        mockk(relaxed = true)

    private val notificationDao: NotificationDao =
        mockk(relaxed = true)

    private val notificationManager: NotificationManager =
        mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {

        Dispatchers.setMain(testDispatcher)

        every {
            database.userInteractionDao()
        } returns userInteractionDao

        every {
            database.notificationDao()
        } returns notificationDao

        every {
            context.getSystemService(Context.NOTIFICATION_SERVICE)
        } returns notificationManager

        /*
         * PriceSyncWorker uses Room's withTransaction extension.
         *
         * Execute the supplied transaction block immediately so the
         * test can verify the DAO calls made inside the transaction.
         */
        mockkStatic("androidx.room.RoomDatabaseKt")

        val transactionSlot = slot<suspend () -> Any?>()

        coEvery {
            database.withTransaction(capture(transactionSlot))
        } coAnswers {
            transactionSlot.captured.invoke()
        }

        worker = PriceSyncWorker(
            context,
            workerParams,
            database,
            api,
            settingsRepository,
            testDispatcher
        )
    }

    @After
    fun tearDown() {

        Dispatchers.resetMain()

        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `doWork returns success when notifications are disabled`() =
        runTest {

            every {
                settingsRepository.areNotificationsEnabled
            } returns flowOf(false)

            val result = worker.doWork()

            assertThat(result)
                .isEqualTo(ListenableWorker.Result.success())

            coVerify(exactly = 0) {
                api.getWishlistPrices()
            }
        }

    @Test
    fun `doWork returns success when wishlist is empty`() =
        runTest {

            every {
                settingsRepository.areNotificationsEnabled
            } returns flowOf(true)

            every {
                settingsRepository.notifyThreshold
            } returns flowOf(3)

            every {
                userInteractionDao.getFavorites()
            } returns flowOf(emptyList())

            val result = worker.doWork()

            assertThat(result)
                .isEqualTo(ListenableWorker.Result.success())

            coVerify(exactly = 0) {
                api.getWishlistPrices()
            }
        }

    @Test
    fun `doWork returns retry on 500 api error`() =
        runTest {

            every {
                settingsRepository.areNotificationsEnabled
            } returns flowOf(true)

            every {
                settingsRepository.notifyThreshold
            } returns flowOf(3)

            every {
                userInteractionDao.getFavorites()
            } returns flowOf(
                listOf(mockk(relaxed = true))
            )

            coEvery {
                api.getWishlistPrices()
            } returns Response.error(
                500,
                mockk(relaxed = true)
            )

            val result = worker.doWork()

            assertThat(result)
                .isEqualTo(ListenableWorker.Result.retry())
        }

    @Test
    fun `doWork processes price drop and triggers notification`() =
        runTest {

            every {
                settingsRepository.areNotificationsEnabled
            } returns flowOf(true)

            every {
                settingsRepository.notifyThreshold
            } returns flowOf(3)

            val localItem = UserInteractionEntity(
                gameId = "game1",
                title = "Cool Game",
                thumbnail = null,
                currentPrice = 100.0,
                originalPrice = 100.0,
                storeId = "steam",
                isFavorite = true
            )

            every {
                userInteractionDao.getFavorites()
            } returns flowOf(listOf(localItem))

            coEvery {
                api.getWishlistPrices()
            } returns Response.success(
                mapOf("game1" to "90.0")
            )

            val result = worker.doWork()

            assertThat(result)
                .isEqualTo(ListenableWorker.Result.success())

            verify {
                userInteractionDao.insertInteraction(
                    match {
                        it.latestSyncedPrice == 90.0
                    }
                )
            }

            coVerify {
                notificationDao.insertNotification(any())
            }

            verify {
                notificationManager.notify(any(), any())
            }
        }

    @Test
    fun `doWork updates price but skips notification if below threshold`() =
        runTest {

            every {
                settingsRepository.areNotificationsEnabled
            } returns flowOf(true)

            every {
                settingsRepository.notifyThreshold
            } returns flowOf(10)

            val localItem = UserInteractionEntity(
                gameId = "game1",
                title = "Cool Game",
                thumbnail = null,
                currentPrice = 100.0,
                originalPrice = 100.0,
                storeId = "steam",
                isFavorite = true
            )

            every {
                userInteractionDao.getFavorites()
            } returns flowOf(listOf(localItem))

            coEvery {
                api.getWishlistPrices()
            } returns Response.success(
                mapOf("game1" to "98.0")
            )

            val result = worker.doWork()

            assertThat(result)
                .isEqualTo(ListenableWorker.Result.success())

            verify {
                userInteractionDao.insertInteraction(
                    match {
                        it.latestSyncedPrice == 98.0
                    }
                )
            }

            coVerify(exactly = 0) {
                notificationDao.insertNotification(any())
            }

            verify(exactly = 0) {
                notificationManager.notify(any(), any())
            }
        }
}