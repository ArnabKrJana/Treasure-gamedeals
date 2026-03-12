package com.example.treasure.data.remote.networkDto.itad


import com.google.gson.annotations.SerializedName

// 1. Root Response
data class ItadResponseDto(
    @SerializedName("nextOffset") val nextOffset: Int?, //
    @SerializedName("hasMore") val hasMore: Boolean,    //
    @SerializedName("list") val list: List<ItadDealItemDto>
)

// 2. The Game Item (The objects inside "list")
data class ItadDealItemDto(
    @SerializedName("id") val id: String,           //
    @SerializedName("title") val title: String,     //
    @SerializedName("slug") val slug: String,       //
    @SerializedName("type") val type: String,       // "game" or "dlc"
    @SerializedName("assets") val assets: ItadAssetsDto?, // Contains images
    @SerializedName("deal") val deal: ItadDealDetailDto   // Contains price/shop info
)

// 3. Assets (Images)
data class ItadAssetsDto(
    // I selected banner400 as a good balance for a card thumbnail
    @SerializedName("banner400") val bannerUrl: String?, //
    @SerializedName("boxart") val boxArt: String?
)

// 4. Deal Details (The "deal" object)
data class ItadDealDetailDto(
    @SerializedName("shop") val shop: ItadShopDto,
    @SerializedName("price") val price: ItadPriceDto,      // Current Price
    @SerializedName("regular") val regular: ItadPriceDto,  // Original Price
    @SerializedName("cut") val cut: Int,                   // Discount %
    @SerializedName("url") val url: String,                // Deal Link
    @SerializedName("platforms") val platforms: List<ItadPlatformDto>?
)

// 5. Helper Objects
data class ItadShopDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String           //
)

data class ItadPriceDto(
    @SerializedName("amount") val amount: Double       //
)

data class ItadPlatformDto(
    @SerializedName("name") val name: String           //
)