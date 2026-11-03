package com.example.treasure.domain.uiModels

/**
 * Presentation model encapsulating comprehensive metadata for an Epic Games promotional free game.
 *
 * Rendered when expanding free game spotlight cards into detailed presentation sheets.
 *
 * @property epicId Unique identifier assigned by Epic Games Store.
 * @property title Primary title of the game.
 * @property description Detailed synopsis or promotional summary.
 * @property images List of high-resolution screenshot and artwork URLs.
 * @property trailerUrl Streaming video URL for trailers or gameplay videos.
 * @property developer Studio that developed the game.
 * @property publisher Publishing organization distributing the title.
 * @property freeFrom Formatted date/time string marking the start of the free promotional window.
 * @property freeUntil Formatted date/time string marking the conclusion of the free promotional window.
 * @property isMystery `true` if the game is still concealed as an upcoming mystery title.
 */
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
