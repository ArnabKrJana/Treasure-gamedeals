package com.example.treasure.workers

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.treasure.MainActivity
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.dao.DealDao
import com.example.treasure.data.local.dao.DownloadedAssetDao
import com.example.treasure.data.local.dao.NotificationDao
import com.example.treasure.data.local.dao.RemoteKeysDao
import com.example.treasure.data.local.dao.UserInteractionDao
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.remote.dto.GameDto
import com.example.treasure.data.remote.dto.SpringPageResponse
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.di.apiModule.NetworkModule
import com.example.treasure.di.databaseModule.DatabaseModule
import com.example.treasure.domain.uiModels.User
import com.example.treasure.utils.PriceNotificationManager
import com.example.treasure.utils.TokenManager
import com.google.common.truth.Truth.assertThat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * End-to-end integration tests verifying background price drop detection, notification dispatching,
 * and subsequent UI rendering on [MainActivity].
 */
@HiltAndroidTest
@UninstallModules(DatabaseModule::class, NetworkModule::class)
@RunWith(AndroidJUnit4::class)
class PriceSyncWorkerEndToEndTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: TreasureDatabase

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var realPriceNotificationManager: PriceNotificationManager

    @BindValue
    @JvmField
    val api: TreasureBackendApi = mockk()

    @Module
    @InstallIn(SingletonComponent::class)
    object TestDatabaseModule {
        @Provides
        @Singleton
        fun provideTestDatabase(@ApplicationContext context: Context): TreasureDatabase {
            return Room.inMemoryDatabaseBuilder(context, TreasureDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }

        @Provides
        @Singleton
        fun provideDownloadedAssetDao(database: TreasureDatabase): DownloadedAssetDao =
            database.downloadedAssetDao()

        @Provides
        @Singleton
        fun provideDealDao(database: TreasureDatabase): DealDao =
            database.dealDao()

        @Provides
        @Singleton
        fun provideUserInteractionDao(database: TreasureDatabase): UserInteractionDao =
            database.userInteractionDao()

        @Provides
        @Singleton
        fun provideNotificationDao(database: TreasureDatabase): NotificationDao =
            database.notificationDao()

        @Provides
        @Singleton
        fun provideRemoteKeysDao(database: TreasureDatabase): RemoteKeysDao =
            database.remoteKeysDao()
    }

    private lateinit var context: Context
    private lateinit var uiDevice: UiDevice
    private val workerParams: WorkerParameters = mockk(relaxed = true)

    @Before
    fun setup() {
        hiltRule.inject()

        context = InstrumentationRegistry.getInstrumentation().targetContext
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        val emptyPage = Response.success(
            SpringPageResponse<GameDto>(
                content = emptyList(),
                pageable = null,
                totalElements = 0,
                totalPages = 0,
                last = true,
                size = 20,
                number = 0,
                sort = null,
                numberOfElements = 0,
                first = true,
                empty = true
            )
        )
        coEvery { api.getDeals(any(), any(), any()) } returns emptyPage
        coEvery { api.getPlatformDeals(any(), any(), any()) } returns emptyPage
        coEvery { api.getAnticipatedGames(any(), any()) } returns Response.success(emptyPage.body())
        coEvery { api.getMyWishlistIds() } returns Response.success(emptyList())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val pkg = context.packageName
            uiDevice.executeShellCommand("pm grant $pkg android.permission.POST_NOTIFICATIONS")
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        tokenManager.saveTokens("test_access_token_123", "test_refresh_token_123")
        tokenManager.saveUser(
            User(
                id = 1L,
                email = "testuser@example.com",
                fullName = "Test User",
                profilePicture = null,
                role = "USER"
            )
        )

        runBlocking {
            settingsRepository.setNotificationsEnabled(true)
            settingsRepository.setNotifyThreshold(3)
        }
    }

    @After
    fun tearDown() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        runBlocking {
            database.clearAllTables()
            settingsRepository.setNotificationsEnabled(true)
            settingsRepository.setNotifyThreshold(3)
        }
    }

    @Test
    fun completeEndToEndWishlistPriceDropNotificationFlow() = runBlocking {
        settingsRepository.setNotificationsEnabled(true)
        settingsRepository.setNotifyThreshold(3)

        val wishlistItem = UserInteractionEntity(
            gameId = "integration_test_hades",
            title = "Hades II",
            thumbnail = null,
            currentPrice = 1000.0,
            originalPrice = 1200.0,
            storeId = "steam",
            isFavorite = true,
            latestSyncedPrice = 1000.0
        )
        database.userInteractionDao().insertInteraction(wishlistItem)

        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("integration_test_hades" to """{"current":700.0,"original":1200.0}""")
        )

        val worker = PriceSyncWorker(
            context,
            workerParams,
            database,
            api,
            settingsRepository,
            realPriceNotificationManager,
            Dispatchers.IO
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        val updatedInteraction = database.userInteractionDao().getInteractionForGame("integration_test_hades")
        assertThat(updatedInteraction?.latestSyncedPrice).isEqualTo(700.0)

        val notificationsInDb = database.notificationDao().getAllNotifications().first()
        assertThat(notificationsInDb).hasSize(1)
        val dbNotification = notificationsInDb[0]
        assertThat(dbNotification.gameId).isEqualTo("integration_test_hades")
        assertThat(dbNotification.title).isEqualTo("Hades II")
        assertThat(dbNotification.oldPrice).isEqualTo(1000.0)
        assertThat(dbNotification.newPrice).isEqualTo(700.0)

        uiDevice.openNotification()

        val notificationTitleFound = uiDevice.wait(
            Until.hasObject(By.textContains("Price Drop: Hades II")),
            3000
        )
        val notificationTextFound = uiDevice.wait(
            Until.hasObject(By.textContains("Price updated to ₹700")),
            3000
        )

        assertThat(notificationTitleFound).isTrue()
        assertThat(notificationTextFound).isTrue()

        uiDevice.pressBack()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Notifications").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hades II").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹1000").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹700").assertIsDisplayed()
    }

    @Test
    fun notificationsDisabledWorkerSucceedsWithoutCallingBackendOrPostingNotifications() = runBlocking {
        settingsRepository.setNotificationsEnabled(false)

        val wishlistItem = UserInteractionEntity(
            gameId = "integration_test_hades",
            title = "Hades II",
            thumbnail = null,
            currentPrice = 1000.0,
            originalPrice = 1200.0,
            storeId = "steam",
            isFavorite = true,
            latestSyncedPrice = 1000.0
        )
        database.userInteractionDao().insertInteraction(wishlistItem)

        val worker = PriceSyncWorker(
            context,
            workerParams,
            database,
            api,
            settingsRepository,
            realPriceNotificationManager,
            Dispatchers.IO
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 0) { api.getWishlistPrices() }

        val notifications = database.notificationDao().getAllNotifications().first()
        assertThat(notifications).isEmpty()
    }

    @Test
    fun thresholdRespectedNoNotificationBelowThresholdAndNotificationAboveThreshold() = runBlocking {
        settingsRepository.setNotificationsEnabled(true)
        settingsRepository.setNotifyThreshold(10)

        val wishlistItem = UserInteractionEntity(
            gameId = "integration_test_hades",
            title = "Hades II",
            thumbnail = null,
            currentPrice = 1000.0,
            originalPrice = 1200.0,
            storeId = "steam",
            isFavorite = true,
            latestSyncedPrice = 1000.0
        )
        database.userInteractionDao().insertInteraction(wishlistItem)

        val worker = PriceSyncWorker(
            context,
            workerParams,
            database,
            api,
            settingsRepository,
            realPriceNotificationManager,
            Dispatchers.IO
        )

        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("integration_test_hades" to """{"current":950.0,"original":1200.0}""")
        )

        val result1 = worker.doWork()
        assertThat(result1).isEqualTo(ListenableWorker.Result.success())

        var notifications = database.notificationDao().getAllNotifications().first()
        assertThat(notifications).isEmpty()

        val updatedItem = database.userInteractionDao().getInteractionForGame("integration_test_hades")
        assertThat(updatedItem?.latestSyncedPrice).isEqualTo(950.0)

        coEvery { api.getWishlistPrices() } returns Response.success(
            mapOf("integration_test_hades" to """{"current":800.0,"original":1200.0}""")
        )

        val result2 = worker.doWork()
        assertThat(result2).isEqualTo(ListenableWorker.Result.success())

        notifications = database.notificationDao().getAllNotifications().first()
        assertThat(notifications).hasSize(1)
        assertThat(notifications[0].oldPrice).isEqualTo(950.0)
        assertThat(notifications[0].newPrice).isEqualTo(800.0)
    }
}
