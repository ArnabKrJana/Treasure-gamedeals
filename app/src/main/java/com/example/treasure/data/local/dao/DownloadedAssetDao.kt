package com.example.treasure.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treasure.data.local.entity.DownloadedAssetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing downloaded media assets (wallpapers and screenshots).
 *
 * Links system [android.app.DownloadManager] task identifiers with internal asset identifiers.
 */
@Dao
interface DownloadedAssetDao {

    /**
     * Persists or replaces a downloaded asset tracking record.
     *
     * @param asset The [DownloadedAssetEntity] containing asset, file, and download task IDs.
     * @return Generated row identifier.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: DownloadedAssetEntity): @JvmSuppressWildcards Long

    /**
     * Continuously observes the list of all unique asset IDs currently downloaded to device storage.
     *
     * @return Cold [Flow] emitting the updated list of downloaded asset ID strings.
     */
    @Query("SELECT assetId FROM downloaded_asset")
    fun getAllDownloadedAssetIds(): Flow<@JvmSuppressWildcards List<String>>

    /**
     * Locates a tracked asset record matching a platform DownloadManager request reference ID.
     *
     * @param downloadId System reference identifier issued by DownloadManager.
     * @return The matching [DownloadedAssetEntity], or `null` if untracked.
     */
    @Query("SELECT * FROM downloaded_asset WHERE downloadId = :downloadId")
    suspend fun getAssetByDownloadId(
        downloadId: Long
    ): @JvmSuppressWildcards DownloadedAssetEntity?

    /**
     * Removes an asset record from tracking upon local file deletion.
     *
     * @param asset The [DownloadedAssetEntity] to remove.
     * @return The number of affected database rows.
     */
    @Delete
    suspend fun delete(
        asset: DownloadedAssetEntity
    ): @JvmSuppressWildcards Int
}
