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

package com.ct.ertclib.dc.core.picker

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import com.ct.ertclib.dc.core.utils.common.UriUtils
import com.ct.ertclib.dc.core.data.common.MediaInfo
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.github.gzuliyujiang.filepicker.ExplorerConfig
import com.github.gzuliyujiang.filepicker.FilePicker
import com.github.gzuliyujiang.filepicker.annotation.ExplorerMode
import com.github.gzuliyujiang.filepicker.contract.OnFilePickedListener
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.luck.picture.lib.entity.LocalMedia

private val SUPPORT_FILE_TYPE = arrayOf(".pdf", ".zip", ".txt", ".mp3")

/**
 *
 * @receiver Context
 * @param isPicture Boolean true拍照 false录像
 * @param dirPath String 指定存储路径
 * @param callback OnPickMediaCallbackListener
 */
fun Context.pickCamera(isPicture: Boolean, dirPath:String, callback: OnPickMediaCallbackListener) {
    PictureUtils.openCamera(this,isPicture,false,dirPath) { result ->
        val mediaList = result.map {
            var filePath = it.path
            if (!TextUtils.isEmpty(filePath) && FileUtils.isUri(filePath)){
                val path = UriUtils.fileUri2File(this,Uri.parse(filePath))?.absolutePath
                if (!TextUtils.isEmpty(path)){
                    filePath = path
                }
            }
            MediaInfo().apply {
                this.id = it.id
                this.bucketId = it.bucketId
                this.path = filePath
                this.absolutePath = filePath
                this.mimeType = it.mimeType
                this.width = it.width
                this.height = it.height
                this.cropOffsetX = it.cropOffsetX
                this.cropOffsetY = it.cropOffsetY
                this.duration = it.duration
                this.size = it.size
                this.sandboxPath = it.sandboxPath
                this.originalPath = it.originalPath
                this.compressPath = it.compressPath
                this.watermarkPath = it.watermarkPath
                this.videoThumbnailPath = it.videoThumbnailPath
                this.displayName = it.fileName
            }
        }
        callback.onResult(mediaList)
    }
}

fun Context.pickPicture(callback: OnPickMediaCallbackListener) {
    PictureUtils.createImageMin(this, ArrayList<LocalMedia>()) { result ->
        val mediaList = result.map {
            var filePath = it.path
            if (!TextUtils.isEmpty(filePath) && FileUtils.isUri(filePath)){
                val path = com.blankj.utilcode.util.UriUtils.uri2File(Uri.parse(filePath)).absolutePath
                if (!TextUtils.isEmpty(path)){
                    filePath = path
                }
            }
            MediaInfo().apply {
                this.id = it.id
                this.bucketId = it.bucketId
                this.path = filePath
                this.absolutePath = filePath
                this.mimeType = it.mimeType
                this.width = it.width
                this.height = it.height
                this.cropOffsetX = it.cropOffsetX
                this.cropOffsetY = it.cropOffsetY
                this.duration = it.duration
                this.size = it.size
                this.sandboxPath = it.sandboxPath
                this.originalPath = it.originalPath
                this.compressPath = it.compressPath
                this.watermarkPath = it.watermarkPath
                this.videoThumbnailPath = it.videoThumbnailPath
                this.displayName = it.fileName
            }
        }
        callback.onResult(mediaList)
    }
}

fun Context.pickFile(callback: OnPickMediaCallbackListener) {
    XXPermissions.with(this)
        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
        .unchecked().request(object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                val config = ExplorerConfig(this@pickFile)
                config.rootDir = Environment.getExternalStorageDirectory()
                config.isLoadAsync = true
                config.explorerMode = ExplorerMode.FILE
                config.isShowHomeDir = true
                config.isShowUpDir = true
                config.isShowHideDir = true
                config.allowExtensions = SUPPORT_FILE_TYPE
                config.onFilePickedListener =
                    OnFilePickedListener { file ->
                        val mediaInfo = MediaInfo()
                        mediaInfo.path = file.path
                        mediaInfo.absolutePath = file.absolutePath
                        mediaInfo.displayName = file.name
                        mediaInfo.size = file.length()
                        mediaInfo.mimeType = file.toURL()?.openConnection()?.contentType
                        callback.onResult(listOf(mediaInfo))
                    }
                val picker = FilePicker(this@pickFile as Activity)
                picker.setExplorerConfig(config)
                picker.show()
            }

            override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                super.onDenied(permissions, doNotAskAgain)
                callback.onCancel()
            }
        })
}

fun Context.resourceUri(resourceId: Int): Uri = with(resources) {
    Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(getResourcePackageName(resourceId))
        .appendPath(getResourceTypeName(resourceId))
        .appendPath(getResourceEntryName(resourceId))
        .build()
}
