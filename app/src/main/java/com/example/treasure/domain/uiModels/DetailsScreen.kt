package com.example.treasure.domain.uiModels

import com.example.treasure.utils.Maturity
import com.example.treasure.utils.Platform


data class GameDetailUiModel(
    val title: String, //same as GameCardItem
    val thumbnail: String?,//same as GameCardItem
    val description: String?,
    val deals: List<StoreDeal> = emptyList(),
    val genres: List<String> = emptyList(), //same as GameCardItem
    val screenshots: List<String> = emptyList(),
    val trailerUrl: String?,
    val systemRequirements: List<SystemRequirement> = emptyList(),
    val importantDetails: ImportantDetails?
)

data class StoreDeal(
    val storeName: String,
    val price: Price,
    val dealUrl: String?
)

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

data class SystemRequirement(
    val specName: String,
    val specValue: String,
    val requirementType:RequirementType
)

enum class RequirementType(name: String){
    MINIMUM("Minimum"),
    MAXIMUM("Maximum")
}