package com.example.treasure.data

import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.remote.networkDto.itad.ItadDealItemDto

fun ItadDealItemDto.toEntity(category: DealCategory, listingIndex: Int): DealEntity {
    return DealEntity(
        id = this.id,
        listingIndex = listingIndex,
        title = this.title,


        thumbnail = this.assets?.bannerUrl ?: this.assets?.boxArt,


        storeId = this.deal.shop.name,
        originalPrice = this.deal.regular.amount,
        currentPrice = this.deal.price.amount,
        discountPercent = this.deal.cut,

        //Initial State (Pre-Enrichment)
        upVotes = null,
        upVoteColor = null,
        category = category,

        platforms = this.deal.platforms?.map { it.name },

        // (Nullable - Filled later by Enrichment)
        description = null,
        screenshots = null,
        trailerUrl = null,
        otherStores = null,
        genres = null,
        systemRequirements = null,
        developer = null,
        publisher = null,
        franchise = null,
        releaseDate = null,
        maturityRating = null
    )
}