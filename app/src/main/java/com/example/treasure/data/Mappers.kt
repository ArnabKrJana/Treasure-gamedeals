package com.example.treasure.data

import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.SystemRequirementEntity
import com.example.treasure.data.remote.dto.GameDto
import com.example.treasure.domain.uiModels.RequirementType
import com.example.treasure.domain.uiModels.Price

// Use aliases to prevent import clashes between the two StoreDeals
import com.example.treasure.data.remote.dto.StoreDeal as DtoStoreDeal
import com.example.treasure.domain.uiModels.StoreDeal as UiStoreDeal

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

        // Map the DTO StoreDeal to the UI StoreDeal
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