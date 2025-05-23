/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.miniapp.bridge

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.webkit.DownloadListener
import com.ct.ertclib.dc.core.utils.logger.Logger

class CTDownloadListener(val context: Context) : DownloadListener {

    companion object {
        private const val TAG = "CTDownloadListener"
    }

    private val sLogger = Logger.getLogger(TAG)
    /**
     *
     *
     * @param p0 String url
     * @param p1 String userAgent
     * @param p2 String contentDisposition
     * @param p3 String mimetype
     * @param p4 Long contentLength
     */
    override fun onDownloadStart(p0: String?, p1: String?, p2: String?, p3: String?, p4: Long) {
        sLogger.info("begin download $p0")
        val request = DownloadManager.Request(Uri.parse(p0))
        context.getSystemService(DownloadListener::class.java)

    }


}