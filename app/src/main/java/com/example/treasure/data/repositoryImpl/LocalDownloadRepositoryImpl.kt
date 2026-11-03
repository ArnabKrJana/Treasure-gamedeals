package com.example.treasure.data.repositoryImpl

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.treasure.data.local.dao.DownloadedAssetDao
import com.example.treasure.data.local.entity.DownloadedAssetEntity
import com.example.treasure.domain.repository.LocalDownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Concrete implementation of [LocalDownloadRepository] managing high-resolution wallpaper downloads.
 *
 * Configures platform [DownloadManager.Request] instances targeting the public `Pictures/Treasure` directory,
 * enables automatic system download completion notifications, records enqueued tasks in [DownloadedAssetDao],
 * and streams downloaded asset ID sets for O(1) UI lookup.
 *
 * @param context Application context used to acquire the platform [DownloadManager] system service.
 * @param downloadedAssetDao Injected Room DAO for tracking downloaded asset identifiers.
 */
class LocalDownloadRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadedAssetDao: DownloadedAssetDao
) : LocalDownloadRepository {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    companion object {
        private const val DIRECTORY_NAME = "Treasure"
        private const val DOWNLOAD_DESCRIPTION = "Downloading Treasure Wallpaper"
    }

    /**
     * Enqueues a wallpaper download to Android's platform [DownloadManager] and records it in Room.
     *
     * @param assetId Unique identifier of the media asset.
     * @param url Remote web URL of the high-resolution image.
     * @param fileName Target local file name in the Pictures directory.
     * @return System [DownloadManager] download request reference ID.
     */
    override suspend fun downloadWallpaper(assetId: String, url: String, fileName: String): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription(DOWNLOAD_DESCRIPTION)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "$DIRECTORY_NAME/${fileName.trimEnd()}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        downloadedAssetDao.insert(
            DownloadedAssetEntity(
                assetId = assetId,
                fileName = fileName,
                downloadId = downloadId
            )
        )

        return downloadId
    }

    /**
     * Observes the set of unique asset IDs currently downloaded to device storage.
     *
     * Maps the underlying database list to a [Set] to provide O(1) membership evaluation in UI composables.
     *
     * @return Cold [Flow] emitting the updated set of downloaded asset ID strings.
     */
    override fun getDownloadedAssetIds(): Flow<Set<String>> {
        return downloadedAssetDao.getAllDownloadedAssetIds().map { it.toSet() }
    }
}
