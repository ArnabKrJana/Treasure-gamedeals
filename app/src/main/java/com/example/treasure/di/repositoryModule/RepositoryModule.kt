package com.example.treasure.di.repositoryModule

import com.example.treasure.data.remote.auth.googleAuthHelper.GoogleAuthHelper
import com.example.treasure.data.remote.auth.googleAuthHelper.GoogleAuthHelperImpl
import com.example.treasure.data.repositoryImpl.AuthRepositoryImpl
import com.example.treasure.data.repositoryImpl.GameRepositoryImpl
import com.example.treasure.data.repositoryImpl.LocalDownloadRepositoryImpl
import com.example.treasure.domain.repository.AuthRepository
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.repository.LocalDownloadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt dependency injection module binding domain repository interfaces to concrete data implementations.
 *
 * Utilizes `@Binds` declarations to wire implementations directly into the [SingletonComponent] graph
 * with zero reflection overhead.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds [GameRepositoryImpl] to the domain [GameRepository] contract.
     *
     * @param gameRepositoryImpl Concrete game catalog repository implementation.
     * @return Bound [GameRepository] contract.
     */
    @Binds
    @Singleton
    abstract fun bindsGameRepository(gameRepositoryImpl: GameRepositoryImpl): GameRepository

    /**
     * Binds [AuthRepositoryImpl] to the domain [AuthRepository] contract.
     *
     * @param authRepositoryImpl Concrete authentication repository implementation.
     * @return Bound [AuthRepository] contract.
     */
    @Binds
    @Singleton
    abstract fun bindsAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    /**
     * Binds [GoogleAuthHelperImpl] to the [GoogleAuthHelper] identity interface.
     *
     * @param googleAuthHelperImpl Concrete Credential Manager Google Sign-In helper.
     * @return Bound [GoogleAuthHelper] contract.
     */
    @Binds
    @Singleton
    abstract fun bindGoogleAuthHelper(
        googleAuthHelperImpl: GoogleAuthHelperImpl
    ): GoogleAuthHelper

    /**
     * Binds [LocalDownloadRepositoryImpl] to the domain [LocalDownloadRepository] contract.
     *
     * @param localDownloadRepositoryImpl Concrete wallpaper download repository implementation.
     * @return Bound [LocalDownloadRepository] contract.
     */
    @Binds
    @Singleton
    abstract fun bindLocalDownloadRepository(
        localDownloadRepositoryImpl: LocalDownloadRepositoryImpl
    ): LocalDownloadRepository
}
