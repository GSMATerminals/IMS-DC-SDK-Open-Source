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

package com.ct.ertclib.dc.core.common

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.ct.ertclib.dc.core.data.model.MessageEntity
import com.ct.ertclib.dc.core.data.common.ContentType
import com.ct.ertclib.dc.core.utils.common.UriUtils

class CompressRunnable : Runnable {

    private val list = mutableListOf<MessageEntity>()
    private var context: Context? = null

    constructor(context: Context, dataList: List<MessageEntity>) {
        this.context = context
        this.list.clear()
        this.list.addAll(dataList)
    }

    override fun run() {
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val messageEntity = iterator.next()
            if (ContentType.isImage(messageEntity.type)) {
                val sourcePath = messageEntity.mediaUri
                val targetPath = messageEntity.thumbnailUri
                if (TextUtils.isEmpty(sourcePath) || TextUtils.isEmpty(targetPath)) {
                    return
                }
                val filePathFromContentUri = if (UriUtils.isContentUri(sourcePath)) {
                    val parse = Uri.parse(sourcePath)
                    if ("com.ct.ertclib.dc.fileprovider" == parse.authority) {
                        getPathFromUri(parse)
                    } else if ("com.ct.ertclib.dc.utilcode.fileprovider" == parse.authority) {
                        getPathFromUri(parse)
                    } else {
                        "" + UriUtils.getFilePathFromContentUri(parse, context!!.contentResolver)
                    }
                } else if (UriUtils.isFileContentUri(sourcePath)) {
                    sourcePath.substring("file:///".length)
                } else {
                    ""
                }
                BitMapPolicy().beginCompress(filePathFromContentUri, targetPath!!)
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String {
        val path = uri.path ?: return ""
        return if (path.startsWith("/external_path")) {
            "" + "storage/emulated/0/" + path.substring("/external_path".length)
        } else if (path.startsWith("/cache_path")) {
            "" + context!!.cacheDir + path.substring("/cache_path".length)
        } else if (path.startsWith("/nc_cache")) {
            "" + context!!.cacheDir + path.substring("/nc_cache".length)
        } else {
            ""
        }
    }
}