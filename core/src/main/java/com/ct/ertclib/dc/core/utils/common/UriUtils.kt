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

package com.ct.ertclib.dc.core.utils.common

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.blankj.utilcode.util.FileUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import java.io.File
import java.io.FileInputStream


object UriUtils {

    private const val TAG = "UriUtils"
    private val sLogger = Logger.getLogger(TAG)

    fun isContentUri(uri: Uri?): Boolean {
        if (uri == null) return false
        return TextUtils.equals(uri.scheme, ContentResolver.SCHEME_CONTENT)
    }

    fun isContentUri(path: String?): Boolean {
        if (path == null) return false
        return path.startsWith(ContentResolver.SCHEME_CONTENT)
    }

    fun isFileContentUri(uri: Uri?): Boolean {
        if (uri == null) return false
        return TextUtils.equals(uri.scheme, ContentResolver.SCHEME_FILE)
    }

    fun isFileContentUri(path: String?): Boolean {
        if (path == null) return false
        return path.startsWith(ContentResolver.SCHEME_FILE)
    }

    fun file2Uri(context: Context, file: File): Uri? {
        if (!FileUtils.isFileExists(file)) return null
        val fileUri: Uri? = try {
            FileProvider.getUriForFile(
                context,
                "com.ct.ertclib.dc.fileprovider",
                file
            )
        } catch (e: IllegalArgumentException) {
            sLogger.error("The selected file can't be shared: $file")
            Uri.fromFile(file)
        }
        return fileUri
    }

    /**
     * Uri 转 绝对路径
     */
    fun getFilePathFromContentUri(
        selectedVideoUri: Uri?,
        contentResolver: ContentResolver
    ): String? {
        val filePath: String
        val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor = contentResolver.query(selectedVideoUri!!, filePathColumn, null, null, null)
        //也可用下面的方法拿到cursor
        //Cursor  cursor  =  this.context.managedQuery(selectedVideoUri,  filePathColumn,  null,  null,  null);
        cursor!!.moveToFirst()
        val columnIndex = cursor.getColumnIndex(filePathColumn[0])
        filePath = cursor.getString(columnIndex)
        cursor.close()
        return filePath
    }

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        try {
            val fdr = context.contentResolver.openFileDescriptor(uri, "r")
            val fileDescriptor = fdr?.fileDescriptor
            val bitmap = BitmapFactory.decodeStream(FileInputStream(fileDescriptor))
            return bitmap
        } catch (e: Exception) {
            sLogger.error(e.message, e)
            return null
        }
    }

    fun fileUri2File(context: Context, uri: Uri): File? {
        if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
            //防止乱码
            var path = uri.encodedPath
            if (path != null) {
                path = Uri.decode(path)
                val cr: ContentResolver = context.contentResolver
                val buff = StringBuffer()
                buff.append("(").append(MediaStore.Images.ImageColumns.DATA).append("=").append(
                    "'$path'"
                ).append(")")
                val cur = cr.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                        MediaStore.Images.ImageColumns._ID,
                        MediaStore.Images.ImageColumns.DATA
                    ),
                    buff.toString(),
                    null,
                    null
                )
                var dataIdx: Int
                cur!!.moveToFirst()
                while (!cur!!.isAfterLast) {
                    dataIdx = cur!!.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                    path = cur!!.getString(dataIdx)
                    cur!!.moveToNext()
                }
                cur!!.close()
            }
            if (path != null) {
                return File(path)
            }
        }
        return null
    }
}
