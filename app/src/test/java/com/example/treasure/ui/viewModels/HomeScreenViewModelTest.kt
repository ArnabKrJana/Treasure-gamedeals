package com.example.treasure.ui.viewModels

import app.cash.turbine.test
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.uiModels.GameCardItem
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [HomeScreenViewModel] deal streams, carousel state, and favorite interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {

    private lateinit var viewModel: HomeScreenViewModel
    private val repository: GameRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { repository.getDealsPaged(any()) } returns flowOf()
        every { repository.getWishlistItems() } returns flowOf(emptyList())
        every { repository.getCartItems() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchAnticipatedGames updates anticipatedGames state`() = runTest {
        val mockGames = listOf(
            GameCardItem(
                id = "1",
                listingIndex = 0,
                title = "Game 1",
                thumbnail = null,
                store = "Steam",
                upVotes = null,
                price = null
            )
        )
        coEvery { repository.getAnticipatedGames() } returns mockGames

        viewModel = HomeScreenViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.anticipatedGames.test {
            val result = awaitItem()
            assertThat(result).isEqualTo(mockGames)
        }
    }

    @Test
    fun `toggleFavorite calls repository toggleFavorite`() = runTest {
        val game = GameCardItem(
            id = "1",
            listingIndex = 0,
            title = "Game 1",
            thumbnail = null,
            store = "Steam",
            upVotes = null,
            price = null
        )

        viewModel = HomeScreenViewModel(repository)
        viewModel.toggleFavorite(game)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.toggleFavorite(game) }
    }
}
