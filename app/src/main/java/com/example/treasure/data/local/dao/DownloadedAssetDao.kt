package com.example.treasure.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treasure.data.local.entity.DownloadedAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedAssetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: DownloadedAssetEntity): @JvmSuppressWildcards Long

    @Query("SELECT assetId FROM downloaded_asset")
    fun getAllDownloadedAssetIds(): Flow<@JvmSuppressWildcards List<String>>

    @Query("SELECT * FROM downloaded_asset WHERE downloadId = :downloadId")
    suspend fun getAssetByDownloadId(
        downloadId: Long
    ): @JvmSuppressWildcards DownloadedAssetEntity?

    @Delete
    suspend fun delete(
        asset: DownloadedAssetEntity
    ): @JvmSuppressWildcards Int
}