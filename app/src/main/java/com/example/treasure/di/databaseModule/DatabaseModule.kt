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


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TreasureDatabase =
        Room.databaseBuilder(
            context,
            TreasureDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration(false)
            .build()

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