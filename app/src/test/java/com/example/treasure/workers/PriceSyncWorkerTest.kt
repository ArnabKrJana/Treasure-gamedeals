package com.example.treasure.workers

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.dao.NotificationDao
import com.example.treasure.data.local.dao.UserInteractionDao
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.utils.PriceNotificationManager
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val priceNotificationManager: PriceNotificationManager = mockk(relaxed = true)

    private val userInteractionDao: UserInteractionDao = mockk(relaxed = true)
    private val notificationDao: NotificationDao = mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0

        every { context.applicationContext } returns context
        every { database.userInteractionDao() } returns userInteractionDao
        every { database.notificationDao() } returns notificationDao

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
            priceNotificationManager,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    // 1. notifications disabled
    @Test
    fun `doWork returns success when notifications are disabled`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(false)

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 0) { api.getWishlistPrices() }
        verify(exactly = 0) { userInteractionDao.getFavorites() }
    }

    // 2. empty local wishlist
    @Test
    fun `doWork returns success when wishlist is empty`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)
        every { userInteractionDao.getFavorites() } returns flowOf(emptyList())

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 0) { api.getWishlistPrices() }
    }

    // 3. backend 5xx error
    @Test
    fun `doWork returns retry on 5xx server error`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(createLocalItem("game1", 100.0)))
        coEvery { api.getWishlistPrices() } returns Response.error(500, mockk(relaxed = true))

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    // 4. backend 429 error
    @Test
    fun `doWork returns retry on 429 rate limit error`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(createLocalItem("game1", 100.0)))
        coEvery { api.getWishlistPrices() } returns Response.error(429, mockk(relaxed = true))

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    // 5. backend non-retryable HTTP error
    @Test
    fun `doWork returns failure on 400 client error`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(createLocalItem("game1", 100.0)))
        coEvery { api.getWishlistPrices() } returns Response.error(400, mockk(relaxed = true))

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    // 6. malformed price JSON
    @Test
    fun `malformed price json for one game does not crash worker and processes valid games`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)

        val game1 = createLocalItem("game1", 100.0)
        val game2 = createLocalItem("game2", 100.0)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(game1, game2))

        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf(
                "game1" to "INVALID_JSON_STRING",
                "game2" to """{"current":90.0,"original":100.0}"""
            )
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        // game2 should still be processed and updated
        verify {
            userInteractionDao.insertInteraction(match { it.gameId == "game2" && it.latestSyncedPrice == 90.0 })
        }
        verify {
            priceNotificationManager.showSingleGameNotification(match { it.gameId == "game2" })
        }
    }

    // 7. game missing from backend price map
    @Test
    fun `game missing from backend price map is skipped safely`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(createLocalItem("game1", 100.0)))

        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("other_game" to """{"current":90.0,"original":100.0}""")
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(exactly = 0) { userInteractionDao.insertInteraction(any()) }
        coVerify(exactly = 0) { notificationDao.insertNotification(any()) }
        verify(exactly = 0) { priceNotificationManager.showSingleGameNotification(any()) }
    }

    // 8. first synchronization
    @Test
    fun `first sync establishes baseline without notification`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)

        val localItem = createLocalItem("game1", latestSyncedPrice = null)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(localItem))

        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("game1" to """{"current":1000.0,"original":1000.0}""")
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify {
            userInteractionDao.insertInteraction(match { it.latestSyncedPrice == 1000.0 })
        }
        coVerify(exactly = 0) { notificationDao.insertNotification(any()) }
        verify(exactly = 0) { priceNotificationManager.showSingleGameNotification(any()) }
    }

    // 9. price change below threshold
    @Test
    fun `price change below threshold updates baseline without notification`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(10)

        val localItem = createLocalItem("game1", latestSyncedPrice = 100.0)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(localItem))

        // 2% drop (< 10% threshold)
        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("game1" to """{"current":98.0,"original":100.0}""")
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify {
            userInteractionDao.insertInteraction(match { it.latestSyncedPrice == 98.0 })
        }
        coVerify(exactly = 0) { notificationDao.insertNotification(any()) }
        verify(exactly = 0) { priceNotificationManager.showSingleGameNotification(any()) }
    }

    // 10. price drop above threshold
    @Test
    fun `price drop above threshold updates baseline and sends notification`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)

        val localItem = createLocalItem("game1", title = "Cool Game", latestSyncedPrice = 100.0)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(localItem))

        // 10% drop (> 3% threshold)
        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("game1" to """{"current":90.0,"original":100.0}""")
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify {
            userInteractionDao.insertInteraction(match { it.latestSyncedPrice == 90.0 })
        }
        coVerify {
            notificationDao.insertNotification(match {
                it.gameId == "game1" && it.oldPrice == 100.0 && it.newPrice == 90.0
            })
        }
        verify {
            priceNotificationManager.showSingleGameNotification(match { it.gameId == "game1" })
        }
    }

    // 11. price increase above threshold
    @Test
    fun `price increase above threshold also triggers notification`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)

        val localItem = createLocalItem("game1", latestSyncedPrice = 100.0)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(localItem))

        // 10% increase (> 3% threshold)
        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("game1" to """{"current":110.0,"original":100.0}""")
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify {
            userInteractionDao.insertInteraction(match { it.latestSyncedPrice == 110.0 })
        }
        coVerify {
            notificationDao.insertNotification(match {
                it.gameId == "game1" && it.oldPrice == 100.0 && it.newPrice == 110.0
            })
        }
        verify {
            priceNotificationManager.showSingleGameNotification(match { it.gameId == "game1" })
        }
    }

    // 12. price change exactly equal to threshold
    @Test
    fun `price change exactly equal to threshold triggers notification`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(10)

        val localItem = createLocalItem("game1", latestSyncedPrice = 100.0)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(localItem))

        // Exactly 10% drop
        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("game1" to """{"current":90.0,"original":100.0}""")
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify {
            notificationDao.insertNotification(match { it.oldPrice == 100.0 && it.newPrice == 90.0 })
        }
        verify {
            priceNotificationManager.showSingleGameNotification(any())
        }
    }

    // 13. multiple games above threshold
    @Test
    fun `multiple games above threshold create multiple notification records and batch notification`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)

        val game1 = createLocalItem("game1", title = "Game One", latestSyncedPrice = 100.0)
        val game2 = createLocalItem("game2", title = "Game Two", latestSyncedPrice = 200.0)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(game1, game2))

        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf(
                "game1" to """{"current":90.0,"original":100.0}""",
                "game2" to """{"current":180.0,"original":200.0}"""
            )
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(exactly = 2) { userInteractionDao.insertInteraction(any()) }
        coVerify(exactly = 2) { notificationDao.insertNotification(any()) }
        verify { priceNotificationManager.showBatchedNotification(match { it.size == 2 }) }
    }

    // 14. zero/invalid previous price
    @Test
    fun `zero previous price establishes baseline without notification`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)

        val localItem = createLocalItem("game1", latestSyncedPrice = 0.0)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(localItem))

        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("game1" to """{"current":100.0,"original":100.0}""")
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify {
            userInteractionDao.insertInteraction(match { it.latestSyncedPrice == 100.0 })
        }
        coVerify(exactly = 0) { notificationDao.insertNotification(any()) }
        verify(exactly = 0) { priceNotificationManager.showSingleGameNotification(any()) }
    }

    // 15. unchanged price
    @Test
    fun `unchanged price does not create notification or unnecessary update`() = runTest(testDispatcher) {
        every { settingsRepository.areNotificationsEnabled } returns flowOf(true)
        every { settingsRepository.notifyThreshold } returns flowOf(3)

        val localItem = createLocalItem("game1", latestSyncedPrice = 100.0)
        every { userInteractionDao.getFavorites() } returns flowOf(listOf(localItem))

        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("game1" to """{"current":100.0,"original":100.0}""")
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(exactly = 0) { userInteractionDao.insertInteraction(any()) }
        coVerify(exactly = 0) { notificationDao.insertNotification(any()) }
        verify(exactly = 0) { priceNotificationManager.showSingleGameNotification(any()) }
    }

    private fun createLocalItem(
        gameId: String,
        latestSyncedPrice: Double?,
        title: String = "Test Game"
    ): UserInteractionEntity {
        return UserInteractionEntity(
            gameId = gameId,
            title = title,
            thumbnail = null,
            currentPrice = 100.0,
            originalPrice = 100.0,
            storeId = "steam",
            isFavorite = true,
            latestSyncedPrice = latestSyncedPrice
        )
    }
}
