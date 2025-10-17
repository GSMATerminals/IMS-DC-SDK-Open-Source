package com.ct.ertclib.dc.core.port.listener

interface IDownloadListener {

    fun onDownloadProgress(progress: Int)

    fun onDownloadSuccess()

    fun onDownloadFailed()
}