package com.example.treasure.data

import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.SystemRequirementEntity
import com.example.treasure.data.remote.dto.GameDto
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.RequirementType
import com.example.treasure.data.remote.dto.StoreDeal as DtoStoreDeal
import com.example.treasure.domain.uiModels.StoreDeal as UiStoreDeal

/**
 * Transforms a remote [GameDto] payload into a persistent [DealEntity] tailored for Room database storage.
 *
 * Maps list feed fields, computes default price fallbacks, normalizes alternative storefront pricing models,
 * and parses system hardware requirement specifications into strongly-typed [RequirementType] tiers.
 *
 * @param category Target [DealCategory] partition (e.g., [DealCategory.HOT_DEALS], [DealCategory.SEARCH]).
 * @param listingIndex Positional index within the category feed for deterministic sorting.
 * @return Fully initialized [DealEntity] ready for database insertion.
 */
fun GameDto.toEntity(category: DealCategory, listingIndex: Int): DealEntity {
    return DealEntity(
        id = this.id,
        listingIndex = listingIndex,
        title = this.title,
        thumbnail = this.thumbnail,
        storeId = this.primaryStore ?: "Unknown",
        originalPrice = this.originalPrice ?: 0.0,
        currentPrice = this.currentPrice ?: 0.0,
        discountPercent = this.discountPercent ?: 0,
        upVotes = this.upVotes,
        upVoteColor = this.upVoteColor,
        category = category,
        platforms = this.platforms,
        description = this.description,
        screenshots = this.screenshots,
        trailerUrl = this.trailerUrl,
        genres = this.genres,
        developer = this.developer,
        publisher = this.publisher,
        franchise = this.franchise,
        releaseDate = this.releaseDate,
        maturityRating = this.maturityRating,
        otherStores = this.otherStores?.map { dtoStoreDeal ->
            UiStoreDeal(
                storeName = dtoStoreDeal.storeName,
                dealUrl = dtoStoreDeal.dealUrl,
                price = Price(
                    originalPrice = dtoStoreDeal.originalPrice,
                    currentPrice = dtoStoreDeal.currentPrice
                )
            )
        },
        systemRequirements = this.systemRequirements?.map {
            SystemRequirementEntity(
                specName = it.specName,
                specValue = it.specValue,
                type = try {
                    RequirementType.valueOf(it.type.uppercase())
                } catch (e: Exception) {
                    RequirementType.MINIMUM
                }
            )
        }
    )
}
