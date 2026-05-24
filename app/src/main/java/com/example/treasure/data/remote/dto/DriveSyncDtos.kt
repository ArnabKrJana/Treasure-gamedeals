package com.example.treasure.data.remote.dto

data class DriveSyncRequest(
    val items: List<DriveSyncItemDto>
)

data class DriveSyncItemDto(
    val gameId: String,
    val imageUrl: String
)

data class DriveSyncStatusResponse(
    val gameId: String,
    val status: String,
    val errorMessage: String?
)