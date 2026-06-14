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

class LocalDownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadedAssetDao: DownloadedAssetDao
) : LocalDownloadRepository {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    override suspend fun downloadWallpaper(assetId: String, url: String, fileName: String): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading Treasure Wallpaper")
            // This tells the Android system to manage the notification for us automatically
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // Saves directly to the public Pictures/Treasure folder
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Treasure/${fileName.trimEnd()}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        // The system returns a unique ID for this specific download
        val downloadId = downloadManager.enqueue(request)

        // Log it to Room so the UI immediately shows it as "Downloaded"
        downloadedAssetDao.insert(
            DownloadedAssetEntity(
                assetId = assetId,
                fileName = fileName,
                downloadId = downloadId
            )
        )

        return downloadId
    }

    override fun getDownloadedAssetIds(): Flow<Set<String>> {
        // Convert the List to a Set for O(1) fast lookups in the UI
        return downloadedAssetDao.getAllDownloadedAssetIds().map { it.toSet() }
    }
}