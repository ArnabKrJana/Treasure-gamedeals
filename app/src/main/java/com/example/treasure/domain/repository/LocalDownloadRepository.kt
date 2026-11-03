package com.example.treasure.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract managing wallpaper and media asset downloads.
 *
 * Enqueues high-resolution image downloads via Android's platform [android.app.DownloadManager],
 * tracks download requests in local Room persistence, and observes downloaded asset IDs.
 */
interface LocalDownloadRepository {

    /**
     * Enqueues a remote wallpaper image for download to device public external storage.
     *
     * @param assetId Unique identifier of the media asset.
     * @param url Remote web URL of the high-resolution wallpaper image.
     * @param fileName Target local file name for the downloaded image in the Pictures directory.
     * @return System [android.app.DownloadManager] download request reference ID.
     */
    suspend fun downloadWallpaper(assetId: String, url: String, fileName: String): Long

    /**
     * Observes the set of unique asset IDs that have already been downloaded to local storage.
     *
     * Enables UI components to render downloaded badges or disable re-download triggers.
     *
     * @return Cold [Flow] emitting the updated [Set] of completed asset ID strings.
     */
    fun getDownloadedAssetIds(): Flow<Set<String>>
}
