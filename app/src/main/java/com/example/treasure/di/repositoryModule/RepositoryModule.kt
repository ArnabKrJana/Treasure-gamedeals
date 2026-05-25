package com.example.treasure.di.repositoryModule

import com.example.treasure.data.remote.auth.googleAuthHelper.GoogleAuthHelper
import com.example.treasure.data.remote.auth.googleAuthHelper.GoogleAuthHelperImpl
import com.example.treasure.data.repositoryImpl.AuthRepositoryImpl
import com.example.treasure.data.repositoryImpl.GameRepositoryImpl
import com.example.treasure.domain.repository.AuthRepository
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

    @Binds
    @Singleton
    abstract fun bindsAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGoogleAuthHelper(
        googleAuthHelperImpl: GoogleAuthHelperImpl
    ): GoogleAuthHelper
}