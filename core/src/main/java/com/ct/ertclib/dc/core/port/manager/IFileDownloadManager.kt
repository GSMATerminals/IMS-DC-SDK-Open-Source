package com.ct.ertclib.dc.core.port.manager

import com.ct.ertclib.dc.core.data.common.DownloadData
import com.ct.ertclib.dc.core.port.listener.IDownloadListener

interface IFileDownloadManager {

    var isDownloading: Boolean

    fun startDownload(downloadData: DownloadData, downloadListener: IDownloadListener)

    fun removeDownload(downloadId: Long)
}