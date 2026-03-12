package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "free_games")
data class FreeGameEntity(
    @PrimaryKey
    val epicId: String,             // (EpicFreeGameCard.epicId)
    val title: String,              //
    val thumbnail: String?,         //
    val freeUntil: String?,         // (ISO Date String)
    val isMystery: Boolean,         //
    val isVaulted: Boolean,         //

    // --- DETAIL DATA ---
    val description: String? = null, // (EpicFreeGameDetail)
    val publisher: String? = null,
    val startDate: String? = null
)
