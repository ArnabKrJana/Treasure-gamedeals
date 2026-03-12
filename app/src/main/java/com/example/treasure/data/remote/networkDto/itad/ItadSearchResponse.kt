package com.example.treasure.data.remote.networkDto.itad


import com.google.gson.annotations.SerializedName

data class ItadSearchItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String, // "game" or "dlc"
    @SerializedName("assets") val assets: ItadSearchAssetsDto?
)

data class ItadSearchAssetsDto(
    @SerializedName("banner400") val bannerUrl: String?,
    @SerializedName("boxart") val boxArt: String?
)