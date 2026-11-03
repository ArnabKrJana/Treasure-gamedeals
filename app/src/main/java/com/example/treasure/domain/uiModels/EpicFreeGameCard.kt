package com.example.treasure.domain.uiModels

/**
 * Presentation model representing an Epic Games Store promotional free game card.
 *
 * Rendered in the Free Games showcase row on the Home Screen.
 *
 * @property epicId Unique identifier assigned by the Epic Games Store API.
 * @property title Name of the promotional game.
 * @property thumbnail Web URL of the promotional key art or banner image.
 * @property freeUntil Formatted date/time string denoting when the 100% discount promotional period expires.
 * @property isMystery `true` if the promotional game identity is sealed under Epic's "Mystery Game" promotion.
 * @property isVaulted `true` if the promotional title is part of Epic's exclusive Vault campaign.
 */
data class EpicFreeGameCard(
    val epicId: String,
    val title: String,
    val thumbnail: String?,
    val freeUntil: String?,
    val isMystery: Boolean,
    val isVaulted: Boolean
)
