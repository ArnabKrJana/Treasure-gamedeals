package com.example.treasure.data.remote.networkDto.itad



import com.google.gson.annotations.SerializedName

data class ItadGameInfoDto(
    @SerializedName("id") val id: String,               //
    @SerializedName("title") val title: String,         //
    @SerializedName("appid") val steamAppId: Int?,      // Crucial for Steam Call
    @SerializedName("assets") val assets: ItadAssetsDto?, //
    @SerializedName("developers") val developers: List<ItadCompanyDto>?, //]
    @SerializedName("publishers") val publishers: List<ItadCompanyDto>?  //]
)

data class ItadCompanyDto(
    @SerializedName("name") val name: String            //
)