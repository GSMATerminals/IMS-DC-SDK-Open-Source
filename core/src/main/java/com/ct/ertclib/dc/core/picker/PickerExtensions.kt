/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.picker

import android.content.Context
import com.ct.ertclib.dc.core.utils.common.UriUtils
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.utils.common.FileUtils
import androidx.core.net.toUri

/**
 *
 * @receiver Context
 * @param isPicture Boolean true拍照 false录像
 * @param dirPath String 指定存储路径
 * @param callback OnPickMediaCallbackListener
 */
fun Context.pickCamera(isPicture: Boolean, dirPath:String, callback: OnPickMediaCallbackListener) {
    PictureUtils.openCamera(this, isPicture, false, dirPath) { result ->
        result.forEach { media ->
            var filePath = media.path
            if (!filePath.isNullOrEmpty() && FileUtils.isUri(filePath)) {
                val path = UriUtils.fileUri2File(this, filePath.toUri())?.absolutePath
                if (!path.isNullOrEmpty()) {
                    filePath = path
                }
            }
            media.path = filePath
            media.absolutePath = filePath
        }
        callback.onResult(result)
    }
}
