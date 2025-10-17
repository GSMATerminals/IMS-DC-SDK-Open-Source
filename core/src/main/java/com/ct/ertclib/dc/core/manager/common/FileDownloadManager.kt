package com.ct.ertclib.dc.core.manager.common

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import com.ct.ertclib.dc.core.data.common.DownloadData
import com.ct.ertclib.dc.core.utils.common.LogUtils
import androidx.core.net.toUri
import com.ct.ertclib.dc.core.port.listener.IDownloadListener
import com.ct.ertclib.dc.core.port.manager.IFileDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FileDownloadManager(private val context: Context): IFileDownloadManager {

    companion object {
        private const val TAG = "FileDownloadManager"
        private const val QUERY_INTERVAL_TIME = 5000L
    }

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    private val downloadListeners = mutableMapOf<Long, IDownloadListener>()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    @Volatile
    private var isDownloading = false

    override fun startDownload(downloadData: DownloadData, downloadListener: IDownloadListener) {
        kotlin.runCatching {
            downloadManager?.let {
                val request = DownloadManager.Request(downloadData.url.toUri())
                request.setTitle(downloadData.title)
                request.setDescription(downloadData.description)
                request.setDestinationInExternalFilesDir(context, "", downloadData.filePath)
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                val downloadId = it.enqueue(request)
                downloadListeners[downloadId] = downloadListener
                startQueryStatus()
            }
        }.onFailure {
            LogUtils.debug(TAG, "startDownload failed: $it")
        }
    }

    override fun removeDownload(downloadId: Long) {
        downloadManager?.remove(downloadId)
        downloadListeners.remove(downloadId)
        if (downloadListeners.isEmpty()) {
            isDownloading = false
        }
    }

    private fun startQueryStatus() {
        if (!isDownloading) {
            isDownloading = true
            scope.launch {
                while (isDownloading && scope.isActive) {
                    delay(QUERY_INTERVAL_TIME)
                    updateDownloadProgress()
                }
            }
        }
    }

    @SuppressLint("Range")
    private fun updateDownloadProgress() {
        kotlin.runCatching {
            downloadListeners.forEach { (downloadId, listener) ->
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager?.let {
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        LogUtils.debug(TAG, "updateDownloadProgress status: $status")
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                listener.onDownloadSuccess()
                                scope.launch {
                                    delay(500L)
                                    downloadListeners.remove(downloadId)
                                    isDownloading = false
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                listener.onDownloadFailed()
                            }
                            else -> {
                                val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                val totalSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                if (totalSize > 0) {
                                    val progress = (bytesDownloaded * 100 / totalSize).toInt()
                                    listener.onDownloadProgress(progress)
                                } else {
                                    listener.onDownloadProgress(0)
                                }
                            }
                        }
                    }
                }
            }
        }.onFailure {
            LogUtils.error(TAG, "updateDownloadProgress failed: $it")
        }
    }
}