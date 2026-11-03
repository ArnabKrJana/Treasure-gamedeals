package com.example.treasure.di.databaseModule

import android.content.Context
import androidx.room.Room
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.dao.DealDao
import com.example.treasure.data.local.dao.DownloadedAssetDao
import com.example.treasure.data.local.dao.NotificationDao
import com.example.treasure.data.local.dao.RemoteKeysDao
import com.example.treasure.data.local.dao.UserInteractionDao
import com.example.treasure.utils.Constants.DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt dependency injection module provisioning local SQLite persistence dependencies.
 *
 * Instantiates the central Room [TreasureDatabase] and binds singleton instances of its underlying
 * Data Access Objects (DAOs) into the [SingletonComponent] dependency graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Builds and provisions the singleton [TreasureDatabase] Room database instance.
     *
     * Disallows destructive migrations (`fallbackToDestructiveMigration(false)`) to protect local user data.
     *
     * @param context Host application context used to initialize the Room database.
     * @return Configured singleton [TreasureDatabase] instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TreasureDatabase =
        Room.databaseBuilder(
            context,
            TreasureDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration(false)
            .build()

    /**
     * Provisions the [DownloadedAssetDao] from the central database instance.
     *
     * @param database The singleton [TreasureDatabase].
     * @return Data access object for downloaded assets.
     */
    @Provides
    @Singleton
    fun provideDownloadedAssetDao(database: TreasureDatabase): DownloadedAssetDao =
        database.downloadedAssetDao()

    /**
     * Provisions the [DealDao] from the central database instance.
     *
     * @param database The singleton [TreasureDatabase].
     * @return Data access object for deals and Paging 3 sources.
     */
    @Provides
    @Singleton
    fun provideDealDao(database: TreasureDatabase): DealDao =
        database.dealDao()

    /**
     * Provisions the [UserInteractionDao] from the central database instance.
     *
     * @param database The singleton [TreasureDatabase].
     * @return Data access object for user interactions (cart and favorites).
     */
    @Provides
    @Singleton
    fun provideUserInteractionDao(database: TreasureDatabase): UserInteractionDao =
        database.userInteractionDao()

    /**
     * Provisions the [NotificationDao] from the central database instance.
     *
     * @param database The singleton [TreasureDatabase].
     * @return Data access object for price fluctuation notifications.
     */
    @Provides
    @Singleton
    fun provideNotificationDao(database: TreasureDatabase): NotificationDao =
        database.notificationDao()

    /**
     * Provisions the [RemoteKeysDao] from the central database instance.
     *
     * @param database The singleton [TreasureDatabase].
     * @return Data access object for Paging 3 pagination cursor keys.
     */
    @Provides
    @Singleton
    fun provideRemoteKeysDao(database: TreasureDatabase): RemoteKeysDao =
        database.remoteKeysDao()
}
