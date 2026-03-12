package com.example.treasure.domain.uiModels

data class EpicFreeGameDetail(
    val epicId: String,
    val title: String,
    val description: String?,
    val images: List<String>,
    val trailerUrl: String?,
    val developer: String?,
    val publisher: String?,
    val freeFrom: String?,
    val freeUntil: String?,
    val isMystery: Boolean
)
