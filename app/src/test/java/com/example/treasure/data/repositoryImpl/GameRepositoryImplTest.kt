package com.example.treasure.data.repositoryImpl

import android.util.Log
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.dao.DealDao
import com.example.treasure.data.local.dao.UserInteractionDao
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.remote.dto.GameDto
import com.example.treasure.data.remote.dto.SpringPageResponse
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class GameRepositoryImplTest {

    private lateinit var repository: GameRepositoryImpl
    private val api: TreasureBackendApi = mockk()
    private val db: TreasureDatabase = mockk()
    private val dealDao: DealDao = mockk(relaxed = true)
    private val userInteractionDao: UserInteractionDao = mockk(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        every { db.dealDao() } returns dealDao
        every { db.userInteractionDao() } returns userInteractionDao
        
        repository = GameRepositoryImpl(api, db)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `getAnticipatedGames returns server data when cache is empty`() = runTest {
        // Prepare
        val gameDto = GameDto(
            id = "1", title = "Test Game", thumbnail = null, primaryStore = null,
            originalPrice = 60.0, currentPrice = 30.0, discountPercent = 50,
            upVotes = "90%", upVoteColor = "green", expectedReleaseDate = null,
            hypeScore = null, description = null, developer = null, publisher = null,
            franchise = null, releaseDate = null, maturityRating = null, trailerUrl = null,
            screenshots = null, genres = null, platforms = null, systemRequirements = null,
            otherStores = null, lastEnrichedAt = null
        )
        val response = SpringPageResponse(
            content = listOf(gameDto),
            pageable = null,
            totalElements = 1L,
            totalPages = 1,
            last = true,
            size = 1,
            number = 0,
            sort = null,
            numberOfElements = 1,
            first = true,
            empty = false
        )
        coEvery { api.getAnticipatedGames() } returns Response.success(response)

        // Act
        val result = repository.getAnticipatedGames()

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Test Game")
        coVerify(exactly = 1) { api.getAnticipatedGames() }
    }

    @Test
    fun `getAnticipatedGames returns cached data when fresh`() = runTest {
        // Prepare
        val gameDto = GameDto(id = "1", title = "Test Game", thumbnail = null, primaryStore = null, originalPrice = null, currentPrice = null, discountPercent = null, upVotes = null, upVoteColor = null, expectedReleaseDate = null, hypeScore = null, description = null, developer = null, publisher = null, franchise = null, releaseDate = null, maturityRating = null, trailerUrl = null, screenshots = null, genres = null, platforms = null, systemRequirements = null, otherStores = null, lastEnrichedAt = null)
        val response = SpringPageResponse(
            content = listOf(gameDto),
            pageable = null,
            totalElements = 1L,
            totalPages = 1,
            last = true,
            size = 1,
            number = 0,
            sort = null,
            numberOfElements = 1,
            first = true,
            empty = false
        )
        coEvery { api.getAnticipatedGames() } returns Response.success(response)

        // First call to fill cache
        repository.getAnticipatedGames()
        
        // Act - Second call
        val result = repository.getAnticipatedGames()

        // Assert
        assertThat(result).hasSize(1)
        // api should only be called once because of cache
        coVerify(exactly = 1) { api.getAnticipatedGames() }
    }

    @Test
    fun `searchGames returns list of games`() = runTest {
        // Prepare
        val gameDto = GameDto(id = "1", title = "Search Result", thumbnail = null, primaryStore = null, originalPrice = null, currentPrice = null, discountPercent = null, upVotes = null, upVoteColor = null, expectedReleaseDate = null, hypeScore = null, description = null, developer = null, publisher = null, franchise = null, releaseDate = null, maturityRating = null, trailerUrl = null, screenshots = null, genres = null, platforms = null, systemRequirements = null, otherStores = null, lastEnrichedAt = null)
        coEvery { api.searchGames("query") } returns Response.success(listOf(gameDto))

        // Act
        val result = repository.searchGames("query")

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Search Result")
    }

    @Test
    fun `toggleFavorite updates local DB and syncs with cloud`() = runTest {
        // For unit test simplicity, we will mock the dao directly and assume transaction works
        
        val game = com.example.treasure.domain.uiModels.GameCardItem(
            id = "1",
            listingIndex = 0,
            title = "Game",
            thumbnail = null,
            store = "Steam",
            upVotes = null,
            price = null
        )
        
        every { userInteractionDao.getInteractionForGame("1") } returns null
        coEvery { api.toggleWishlist("1") } returns Response.success(Unit)

        // Act
        repository.toggleFavorite(game)

        // Assert
        verify { userInteractionDao.insertInteraction(any()) }
        coVerify { api.toggleWishlist("1") }
    }
}
