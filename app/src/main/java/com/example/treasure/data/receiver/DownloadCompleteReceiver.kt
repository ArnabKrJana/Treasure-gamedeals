package com.example.treasure.data.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.treasure.data.local.dao.DownloadedAssetDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * System [BroadcastReceiver] listening for platform [DownloadManager.ACTION_DOWNLOAD_COMPLETE] broadcasts.
 *
 * Reconciles the terminal download status reported by the system [DownloadManager] with the local
 * [DownloadedAssetDao] tracking table:
 * - If [DownloadManager.STATUS_SUCCESSFUL]: Retains the database record, maintaining the "Downloaded" state in UI.
 * - If [DownloadManager.STATUS_FAILED]: Evicts the tracking record from [DownloadedAssetDao], resetting the UI action button back to "Download".
 */
@AndroidEntryPoint
class DownloadCompleteReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DownloadReceiver"
    }

    /**
     * Injected DAO for managing downloaded wallpaper records in local Room persistence.
     */
    @Inject
    lateinit var downloadedAssetDao: DownloadedAssetDao

    /**
     * Handles incoming broadcast intents from the Android DownloadManager service.
     *
     * @param context Host Android context.
     * @param intent Received broadcast intent containing the system download ID.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == -1L || context == null) return

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex >= 0) {
                    val status = cursor.getInt(statusIndex)

                    CoroutineScope(Dispatchers.IO).launch {
                        val asset = downloadedAssetDao.getAssetByDownloadId(downloadId)

                        if (asset != null) {
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                Log.d(TAG, "Successfully downloaded: ${asset.fileName}")
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                Log.e(TAG, "Failed to download: ${asset.fileName}")
                                downloadedAssetDao.delete(asset)
                            }
                        }
                    }
                }
            }
            cursor.close()
        }
    }
}
