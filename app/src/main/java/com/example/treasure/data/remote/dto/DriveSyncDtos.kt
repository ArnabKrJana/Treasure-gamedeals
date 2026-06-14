package com.example.treasure.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName


@Keep
data class DriveSyncStatusResponse(
    @SerializedName("gameId") val gameId: String,
    @SerializedName("status") val status: String,
    @SerializedName("errorMessage") val errorMessage: String?
)

@Keep
data class DriveSyncRequest(
    @SerializedName("items") val items: List<DriveSyncItemDto>
)

@Keep
data class DriveSyncItemDto(
    @SerializedName("gameId") val gameId: String,
    @SerializedName("imageUrl") val imageUrl: String
)