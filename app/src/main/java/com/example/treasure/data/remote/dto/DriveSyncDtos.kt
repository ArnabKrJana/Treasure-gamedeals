package com.example.treasure.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Status response tracking asynchronous Google Drive media backup jobs processed by backend RabbitMQ workers.
 *
 * @property gameId Unique identifier of the game title whose artwork is being synchronized.
 * @property status Processing status string (e.g., "PENDING", "IN_PROGRESS", "COMPLETED", "FAILED").
 * @property errorMessage Descriptive error message if the backup worker encountered a processing failure, or `null`.
 */
@Keep
data class DriveSyncStatusResponse(
    @SerializedName("gameId") val gameId: String,
    @SerializedName("status") val status: String,
    @SerializedName("errorMessage") val errorMessage: String?
)

/**
 * Request payload submitting a batch of media assets to be queued for asynchronous cloud backup.
 *
 * @property items List of media asset descriptors to back up.
 */
@Keep
data class DriveSyncRequest(
    @SerializedName("items") val items: List<DriveSyncItemDto>
)

/**
 * Individual media asset descriptor submitted within a [DriveSyncRequest].
 *
 * @property gameId Unique identifier of the associated game.
 * @property imageUrl Web URL of the high-resolution artwork or wallpaper image to download and back up.
 */
@Keep
data class DriveSyncItemDto(
    @SerializedName("gameId") val gameId: String,
    @SerializedName("imageUrl") val imageUrl: String
)
