package com.example.treasure.domain.uiModels

data class EpicFreeGameCard(
    val epicId: String,
    val title: String,
    val thumbnail: String?,
    val freeUntil: String?,
    val isMystery: Boolean,
    val isVaulted: Boolean
)

