package com.example.treasure.data

import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.remote.dto.GameDto
import com.example.treasure.data.remote.dto.SystemRequirement
import com.example.treasure.domain.uiModels.RequirementType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MappersTest {

    @Test
    fun `GameDto toEntity maps all basic fields correctly`() {
        val dto = GameDto(
            id = "123",
            title = "Test Game",
            thumbnail = "thumb.jpg",
            primaryStore = "Steam",
            originalPrice = 59.99,
            currentPrice = 29.99,
            discountPercent = 50,
            upVotes = "90%",
            upVoteColor = "GREEN",
            expectedReleaseDate = null,
            hypeScore = null,
            description = "Description",
            developer = "Dev",
            publisher = "Pub",
            franchise = "Franchise",
            releaseDate = "2024-01-01",
            maturityRating = "M",
            trailerUrl = "trailer.mp4",
            screenshots = listOf("s1.jpg", "s2.jpg"),
            genres = listOf("RPG", "Action"),
            platforms = listOf("Windows", "Mac"),
            systemRequirements = null,
            otherStores = null,
            lastEnrichedAt = null
        )

        val entity = dto.toEntity(DealCategory.HOT_DEALS, 1)

        assertThat(entity.id).isEqualTo("123")
        assertThat(entity.title).isEqualTo("Test Game")
        assertThat(entity.thumbnail).isEqualTo("thumb.jpg")
        assertThat(entity.storeId).isEqualTo("Steam")
        assertThat(entity.originalPrice).isEqualTo(59.99)
        assertThat(entity.currentPrice).isEqualTo(29.99)
        assertThat(entity.discountPercent).isEqualTo(50)
        assertThat(entity.upVotes).isEqualTo("90%")
        assertThat(entity.upVoteColor).isEqualTo("GREEN")
        assertThat(entity.category).isEqualTo(DealCategory.HOT_DEALS)
        assertThat(entity.listingIndex).isEqualTo(1)
        assertThat(entity.description).isEqualTo("Description")
        assertThat(entity.developer).isEqualTo("Dev")
        assertThat(entity.publisher).isEqualTo("Pub")
        assertThat(entity.franchise).isEqualTo("Franchise")
        assertThat(entity.releaseDate).isEqualTo("2024-01-01")
        assertThat(entity.maturityRating).isEqualTo("M")
        assertThat(entity.trailerUrl).isEqualTo("trailer.mp4")
        assertThat(entity.screenshots).containsExactly("s1.jpg", "s2.jpg")
        assertThat(entity.genres).containsExactly("RPG", "Action")
        assertThat(entity.platforms).containsExactly("Windows", "Mac")
    }

    @Test
    fun `GameDto toEntity handles null optional fields with defaults`() {
        val dto = GameDto(
            id = "123",
            title = "Test Game",
            thumbnail = null,
            primaryStore = null,
            originalPrice = null,
            currentPrice = null,
            discountPercent = null,
            upVotes = null,
            upVoteColor = null,
            expectedReleaseDate = null,
            hypeScore = null,
            description = null,
            developer = null,
            publisher = null,
            franchise = null,
            releaseDate = null,
            maturityRating = null,
            trailerUrl = null,
            screenshots = null,
            genres = null,
            platforms = null,
            systemRequirements = null,
            otherStores = null,
            lastEnrichedAt = null
        )

        val entity = dto.toEntity(DealCategory.SEARCH, 0)

        assertThat(entity.storeId).isEqualTo("Unknown")
        assertThat(entity.originalPrice).isEqualTo(0.0)
        assertThat(entity.currentPrice).isEqualTo(0.0)
        assertThat(entity.discountPercent).isEqualTo(0)
        assertThat(entity.screenshots).isNull()
        assertThat(entity.genres).isNull()
    }

    @Test
    fun `GameDto toEntity maps system requirements correctly`() {
        val dto = GameDto(
            id = "123",
            title = "Test",
            thumbnail = null,
            primaryStore = null,
            originalPrice = null,
            currentPrice = null,
            discountPercent = null,
            upVotes = null,
            upVoteColor = null,
            expectedReleaseDate = null,
            hypeScore = null,
            description = null,
            developer = null,
            publisher = null,
            franchise = null,
            releaseDate = null,
            maturityRating = null,
            trailerUrl = null,
            screenshots = null,
            genres = null,
            platforms = null,
            systemRequirements = listOf(
                SystemRequirement(specName = "OS", specValue = "Windows 10", type = "minimum"),
                SystemRequirement(specName = "Memory", specValue = "16GB", type = "maximum"),
                SystemRequirement(specName = "Invalid", specValue = "Value", type = "unknown")
            ),
            otherStores = null,
            lastEnrichedAt = null
        )

        val entity = dto.toEntity(DealCategory.SEARCH, 0)

        assertThat(entity.systemRequirements).hasSize(3)
        assertThat(entity.systemRequirements!![0].type).isEqualTo(RequirementType.MINIMUM)
        assertThat(entity.systemRequirements!![1].type).isEqualTo(RequirementType.MAXIMUM)
        assertThat(entity.systemRequirements!![2].type).isEqualTo(RequirementType.MINIMUM) // Fallback for invalid type
    }
}
