package com.example.treasure.domain.uiModels

import com.example.treasure.utils.Maturity
import com.example.treasure.utils.Platform

/**
 * Presentation model encapsulating the complete detail specification of a selected video game title.
 *
 * Rendered by [com.example.treasure.ui.screens.DetailScreen] and enriched lazily from backend API endpoints.
 *
 * @property title Primary name of the game.
 * @property thumbnail Remote URL pointing to the game's high-resolution cover artwork.
 * @property description Detailed synopsis or plot overview of the title.
 * @property deals List of available storefront deals and current pricing offers across platforms.
 * @property genres Curated category and genre classification tags.
 * @property screenshots Gallery of high-definition in-game screenshots and promotional artwork.
 * @property trailerUrl Optional streaming video URL for official trailers or teaser gameplay.
 * @property systemRequirements Minimum and recommended PC hardware execution specifications.
 * @property importantDetails Publishing metadata, developers, release date, and maturity ratings.
 */
data class GameDetailUiModel(
    val title: String,
    val thumbnail: String?,
    val description: String?,
    val deals: List<StoreDeal> = emptyList(),
    val genres: List<String> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val trailerUrl: String?,
    val systemRequirements: List<SystemRequirement> = emptyList(),
    val importantDetails: ImportantDetails?
)

/**
 * Represents an individual storefront pricing deal for a game title.
 *
 * @property storeName Display name of the digital merchant (e.g., "Steam", "Epic Games Store", "GOG").
 * @property price Current and baseline [Price] structure offered by the merchant.
 * @property dealUrl Outbound link navigating the user directly to the store checkout or product page.
 */
data class StoreDeal(
    val storeName: String,
    val price: Price,
    val dealUrl: String?
)

/**
 * Publishing, development, and advisory metadata for a game title.
 *
 * @property developer Studio responsible for game development.
 * @property publisher Company financing and distributing the title.
 * @property franchise Intellectual property (IP) or game universe series name, if applicable.
 * @property releaseDate Human-readable release date string.
 * @property maturity Standardized age and content advisory classification ([Maturity]).
 * @property upVotes Community approval rating and sentiment indicators ([UpVotes]).
 * @property platform Set of compatible operating systems ([Platform]). Defaults to Windows.
 * @property genres Genre classification list.
 */
data class ImportantDetails(
    val developer: String?,
    val publisher: String?,
    val franchise: String?,
    val releaseDate: String?,
    val maturity: Maturity,
    val upVotes: UpVotes,
    val platform: Set<Platform> = mutableSetOf(Platform.WINDOWS),
    val genres: List<String> = emptyList()
)

/**
 * Hardware specification requirement entry for running the game executable.
 *
 * @property specName Component specification descriptor (e.g., "OS", "Processor", "Memory", "Graphics", "Storage").
 * @property specValue Hardware capability string required (e.g., "Intel Core i5-8400", "16 GB RAM").
 * @property requirementType Classification distinguishing minimum vs recommended tiers ([RequirementType]).
 */
data class SystemRequirement(
    val specName: String,
    val specValue: String,
    val requirementType: RequirementType
)

/**
 * Classification tier for PC hardware requirements.
 *
 * @param name Display label representing the hardware tier.
 */
enum class RequirementType(name: String) {
    /**
     * Minimum hardware baseline required to launch and play the game.
     */
    MINIMUM("Minimum"),

    /**
     * Recommended hardware configuration for optimal frame rates and visual fidelity.
     */
    MAXIMUM("Maximum")
}
