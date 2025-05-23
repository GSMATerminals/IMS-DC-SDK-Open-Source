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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.text.TextUtils
import com.blankj.utilcode.util.FileUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.port.common.ICompressPolicy
import java.io.File
import java.io.FileOutputStream

class BitMapPolicy : ICompressPolicy {

    companion object {
        private const val TAG = "BitMapPolicy"
    }

    private val sLogger = Logger.getLogger(TAG)
    override fun beginCompress(originPath: String, targetPath: String) {
        if (TextUtils.isEmpty(originPath) || TextUtils.isEmpty(targetPath)) {
            sLogger.warn("compress bitmap failed, originPath or targetPath is empty")
        }
        val fileLength = FileUtils.getFileLength(originPath)
        var newOpts = BitmapFactory.Options()
        //只获取宽高
        newOpts.inJustDecodeBounds = true
        try {
            BitmapFactory.decodeFile(originPath, newOpts)
        } catch (e: Exception) {
            sLogger.warn(e.message, e)
        }
        val compressPercent = 200.0 * 1024.0 / fileLength
        val changeWidth = compressPercent * newOpts.outWidth
        val changeHeight = compressPercent * newOpts.outHeight

        val expireSize = compressPercent * fileLength

        newOpts.inJustDecodeBounds = false
        newOpts.inSampleSize = compressPercent.toInt()
        newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888
        var originBitmap: Bitmap? = null
        try {
            originBitmap = BitmapFactory.decodeFile(originPath, newOpts)

            val middleBitmap = Bitmap.createScaledBitmap(
                originBitmap,
                changeWidth.toInt(),
                changeHeight.toInt(),
                true
            )

            val file = File(targetPath)
            file.createNewFile()
            val fos = FileOutputStream(file)
            var fos2: FileOutputStream? = null
            var options = 97
            var isCompressSuccess = middleBitmap.compress(Bitmap.CompressFormat.JPEG, options, fos)
            fos.flush()

            val compressFileSize = file.length()

            sLogger.info("isCompressSuccess:$isCompressSuccess originFileSize = $fileLength, compressFileSize = $compressFileSize, expireFileSize = $expireSize")
            while (compressPercent > expireSize) {
                sLogger.info("compress again")
                if (file.exists()) {
                    val deleteResult = file.delete()
                    sLogger.info("delete ${file.name}:$deleteResult")
                }
                options -= 10
                if (options <= 0) break
                file.createNewFile()
                fos2 = FileOutputStream(file)
                isCompressSuccess = middleBitmap.compress(Bitmap.CompressFormat.JPEG, options, fos2)
                sLogger.info("compress again isCompressSuccess:$isCompressSuccess")
                fos2.flush()
                fos2.close()
                fos2 = null
            }
            fos.flush()
            fos.close()

            val exifInterface = ExifInterface(originPath)
            val newExif = ExifInterface(targetPath)
            newExif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)
            )
            newExif.saveAttributes()
        } catch (e: Exception) {
            sLogger.warn(e.message, e)
        }
    }

    override fun stopCompress() {

    }
}