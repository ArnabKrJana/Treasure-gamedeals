package com.example.treasure.di.databaseModule

import android.content.Context
import androidx.room.Room
import com.example.treasure.data.local.TreasureDatabase
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
        ).fallbackToDestructiveMigration()
            .build()
}