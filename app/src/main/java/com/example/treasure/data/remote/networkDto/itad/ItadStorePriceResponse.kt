package com.example.treasure.data.remote.networkDto.itad

import com.google.gson.annotations.SerializedName


data class ItadOverviewResponseDto(
    @SerializedName("prices") val prices: List<ItadOverviewPriceDto>
)

data class ItadOverviewPriceDto(
    @SerializedName("id") val id: String,
    @SerializedName("current") val current: ItadOverviewDealDto?, // Nullable if no active deal
    @SerializedName("lowest") val lowest: ItadOverviewDealDto?    // Historical Low
)

data class ItadOverviewDealDto(
    @SerializedName("shop") val shop: ItadShopDto,
    @SerializedName("price") val price: ItadPriceDto,
    @SerializedName("regular") val regular: ItadPriceDto,
    @SerializedName("cut") val cut: Int,
    @SerializedName("url") val url: String?, // The Redirect URL!
    @SerializedName("timestamp") val timestamp: String?
)