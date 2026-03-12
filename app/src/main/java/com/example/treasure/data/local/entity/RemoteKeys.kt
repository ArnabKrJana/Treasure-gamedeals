package com.example.treasure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey
    val queryId: String,  //"${dealDto.id}_${category.name}"
    val prevKey: Int?,    // Previous Page Number
    val nextKey: Int?     // Next Page Number
)
