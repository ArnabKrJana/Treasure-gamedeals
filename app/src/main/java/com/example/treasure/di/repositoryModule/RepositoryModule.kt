package com.example.treasure.di.repositoryModule

import com.example.treasure.data.repositoryImpl.GameRepositoryImpl
import com.example.treasure.domain.repository.GameRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindsGameRepository(gameRepositoryImpl: GameRepositoryImpl): GameRepository
}