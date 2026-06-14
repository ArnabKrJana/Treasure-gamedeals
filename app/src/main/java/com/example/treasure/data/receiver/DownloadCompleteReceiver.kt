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

@AndroidEntryPoint
class DownloadCompleteReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloadedAssetDao: DownloadedAssetDao

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
                                Log.d("DownloadReceiver", "Successfully downloaded: ${asset.fileName}")
                                // It's already in the DB as pending, so we just leave it to keep the UI checked!
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                Log.e("DownloadReceiver", "Failed to download: ${asset.fileName}")
                                // Delete it so the UI reverts from a "Checked" icon back to the "Download" icon
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