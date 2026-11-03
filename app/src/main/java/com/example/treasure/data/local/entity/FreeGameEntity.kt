package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity representing promotional free games offered by the Epic Games Store.
 *
 * Persists promotional window bounds, mystery/vault classification flags, and detailed synopsis
 * for offline display in the Home Screen free game carousel.
 *
 * @property epicId Unique promotional identifier assigned by Epic Games Store, serving as the primary key.
 * @property title Primary title of the promotional game.
 * @property thumbnail Remote URL pointing to the promotional key artwork.
 * @property freeUntil ISO-8601 formatted date/time string marking the end of the free claim window.
 * @property isMystery `true` if the title is an unrevealed mystery release.
 * @property isVaulted `true` if the promotion is part of the Epic Games Store Vault campaign.
 * @property description Detailed synopsis or plot summary.
 * @property publisher Publishing organization distributing the title.
 * @property startDate ISO-8601 formatted date/time string marking the start of the free claim window.
 */
@Entity(tableName = "free_games")
data class FreeGameEntity(
    @PrimaryKey
    val epicId: String,
    val title: String,
    val thumbnail: String?,
    val freeUntil: String?,
    val isMystery: Boolean,
    val isVaulted: Boolean,
    val description: String? = null,
    val publisher: String? = null,
    val startDate: String? = null
)
