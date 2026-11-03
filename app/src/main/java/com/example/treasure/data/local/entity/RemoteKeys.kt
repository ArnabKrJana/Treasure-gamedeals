package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity storing pagination cursor metadata for AndroidX Paging 3.
 *
 * Utilized by [com.example.treasure.data.local.remoteMediators.DealRemoteMediator] to determine
 * previous and next page indices when fetching subsequent pages or prepending previous pages.
 *
 * @property queryId Composite identifier formatted as `"${dealId}_${category.name}"` serving as primary key.
 * @property prevKey Page index of the previous page, or `null` if the first page is reached.
 * @property nextKey Page index of the next page to fetch, or `null` if pagination end is reached.
 */
@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey
    val queryId: String,
    val prevKey: Int?,
    val nextKey: Int?
)
