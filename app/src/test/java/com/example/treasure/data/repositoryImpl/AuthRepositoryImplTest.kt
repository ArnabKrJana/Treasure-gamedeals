package com.example.treasure.data.repositoryImpl

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.dao.UserInteractionDao
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.remote.dto.*
import com.example.treasure.utils.TokenManager
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private lateinit var repository: AuthRepositoryImpl

    private val api: TreasureBackendApi = mockk()
    private val tokenManager: TokenManager = mockk(relaxed = true)
    private val db: TreasureDatabase = mockk()
    private val userInteractionDao: UserInteractionDao = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val workManager: WorkManagerImpl = mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // Mock database DAO
        every { db.userInteractionDao() } returns userInteractionDao

        /*
         * AuthRepositoryImpl calls:
         *
         * WorkManager.getInstance(context).cancelAllWork()
         *
         * WorkManager.getInstance() delegates internally to
         * WorkManagerImpl.getInstance(context).
         *
         * Mock the actual implementation method instead of the
         * Kotlin WorkManager companion wrapper.
         */
        mockkStatic(WorkManagerImpl::class)
        every {
            WorkManagerImpl.getInstance(any())
        } returns workManager

        repository = AuthRepositoryImpl(
            api,
            tokenManager,
            db,
            testDispatcher,
            context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()

        unmockkStatic(Log::class)
        unmockkStatic(WorkManagerImpl::class)
    }

    @Test
    fun `loginWithGoogle success saves tokens and user and restores wishlist`() =
        runTest {

            // Prepare
            val idToken = "some_id_token"

            val userDto = UserDto(
                1L,
                "test@test.com",
                "Test User",
                null,
                Role.USER
            )

            val authResponse = AuthResponse(
                "access",
                "refresh",
                userDto
            )

            coEvery {
                api.login("google", any())
            } returns Response.success(authResponse)

            coEvery {
                api.getDetailedWishlist()
            } returns Response.success(emptyList())

            // Act
            val result = repository.loginWithGoogle(idToken)

            // Assert
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()?.email)
                .isEqualTo("test@test.com")

            verify {
                tokenManager.saveTokens(
                    "access",
                    "refresh"
                )
            }

            verify {
                tokenManager.saveUser(any())
            }

            coVerify {
                api.getDetailedWishlist()
            }
        }

    @Test
    fun `loginWithGoogle success but wishlist failure still returns success user`() =
        runTest {

            // Prepare
            val idToken = "some_id_token"

            val userDto = UserDto(
                1L,
                "test@test.com",
                "Test User",
                null,
                Role.USER
            )

            val authResponse = AuthResponse(
                "access",
                "refresh",
                userDto
            )

            coEvery {
                api.login("google", any())
            } returns Response.success(authResponse)

            coEvery {
                api.getDetailedWishlist()
            } throws Exception("Network Error")

            // Act
            val result = repository.loginWithGoogle(idToken)

            // Assert
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()?.email)
                .isEqualTo("test@test.com")

            verify {
                Log.e(
                    "AuthRepo",
                    "Failed to restore wishlist on login",
                    any()
                )
            }
        }

    @Test
    fun `loginWithGoogle api failure returns failure`() =
        runTest {

            // Prepare
            coEvery {
                api.login("google", any())
            } returns Response.error(
                401,
                "".toResponseBody()
            )

            // Act
            val result = repository.loginWithGoogle("invalid")

            // Assert
            assertThat(result.isFailure).isTrue()

            verify(exactly = 0) {
                tokenManager.saveTokens(any(), any())
            }
        }

    @Test
    fun `logout clears token manager and db and cancels work`() =
        runTest {

            // Prepare
            every {
                tokenManager.getRefreshToken()
            } returns "refresh"

            coEvery {
                api.logout(any())
            } returns Response.success(
                MessageResponse("ok")
            )

            every {
                db.clearAllTables()
            } just Runs

            // Act
            val result = repository.logout()

            // Assert
            assertThat(result.isSuccess).isTrue()

            verify {
                tokenManager.clearSession()
            }

            verify {
                db.clearAllTables()
            }

            verify {
                workManager.cancelAllWork()
            }
        }

    @Test
    fun `logout proceeds even if api fails`() =
        runTest {

            // Prepare
            every {
                tokenManager.getRefreshToken()
            } returns "refresh"

            coEvery {
                api.logout(any())
            } throws Exception("Server down")

            every {
                db.clearAllTables()
            } just Runs

            // Act
            val result = repository.logout()

            // Assert
            assertThat(result.isSuccess).isTrue()

            verify {
                tokenManager.clearSession()
            }

            verify {
                db.clearAllTables()
            }
        }

    @Test
    fun `deleteAccount success wipes local data`() =
        runTest {

            // Prepare
            coEvery {
                api.deleteMyAccount()
            } returns Response.success(
                MessageResponse("deleted")
            )

            every {
                db.clearAllTables()
            } just Runs

            // Act
            val result = repository.deleteAccount()

            // Assert
            assertThat(result.isSuccess).isTrue()

            verify {
                tokenManager.clearSession()
            }

            verify {
                db.clearAllTables()
            }

            verify {
                workManager.cancelAllWork()
            }
        }

    @Test
    fun `deleteAccount failure returns failure`() =
        runTest {

            // Prepare
            coEvery {
                api.deleteMyAccount()
            } returns Response.error(
                500,
                "".toResponseBody()
            )

            // Act
            val result = repository.deleteAccount()

            // Assert
            assertThat(result.isFailure).isTrue()

            verify(exactly = 0) {
                tokenManager.clearSession()
            }
        }
}