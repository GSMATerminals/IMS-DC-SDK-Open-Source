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

package com.ct.oemec.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Base64
import androidx.annotation.RequiresApi
import com.ct.oemec.utils.logger.Logger
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.zip.ZipInputStream


object FileUtils {
    private const val TAG = "FileUtils"
    private val sLogger = Logger.getLogger(TAG)
    fun isFileExists(path: String): Boolean {
        return isFileExists(newFile(path))
    }

    private fun isFileExists(file: File?): Boolean {
        return file != null && file.exists()
    }

    private fun newFile(path: String): File? {
        if (isValidPath(path)) {
            return null
        }
        return File(path)
    }

    private fun isValidPath(str: String?): Boolean {
        if (str == null) {
            return true
        }
        val length = str.length
        for (i in 0 until length) {
            if (!Character.isWhitespace(str[i])) {
                return false
            }
        }
        return true
    }

    fun deletePath(path: String): Boolean {
        return deleteFile(newFile(path))
    }

    private fun deleteFile(file: File?): Boolean {
        if (file == null) {
            return false
        }
        if (!file.exists()) {
            return true
        }
        if (!file.isDirectory) {
            return false
        }

        val listFiles = file.listFiles()
        if (!listFiles.isNullOrEmpty()) {
            listFiles.forEach {
                if (it.isFile) {
                    if (!it.delete()) {
                        return false
                    }
                } else if (it.isDirectory && !deleteFile(it)) {
                    return false
                }
            }
        }
        return file.delete()
    }

    fun getPathFiles(path: String): List<File>? {
        return getFileList(newFile(path), false)
    }

    private fun getFileList(file: File?, isGetChildFile: Boolean): ArrayList<File>? {
        if (!isDirectory(file)) {
            return null
        }
        val listFiles = file!!.listFiles()
        if (listFiles.isNullOrEmpty()) {
            return null
        }
        val fileFilter = FileFilter { true }
        val fileList = ArrayList<File>()
        listFiles.forEach {
            if (fileFilter.accept(it)) {
                fileList.add(it)
            }
            if (isGetChildFile && it.isDirectory) {
                getFileList(it, true)?.let { childList -> fileList.addAll(childList) }
            }
        }
        return fileList
    }

    fun isDirectory(file: File?): Boolean {
        return file != null && file.exists() && file.isDirectory
    }

    fun isFile(file: File?): Boolean {
        return file != null && file.exists() && file.isFile
    }

    fun getLastPathName(path: String): String {
        if (isValidPath(path)) {
            return ""
        }

        val lastIndex = path.lastIndexOf(File.separator)
        if (lastIndex != -1) {
            return path.substring(lastIndex + 1)
        }
        return path
    }

    //根据文件路径获取文件byte[]
    fun getFileBytes(filePath: String): ByteArray? {
        if (isFileExists(filePath)) {
            val file = File(filePath)
            if (file.exists()) {
                return file.readBytes()
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getPath(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                if (!TextUtils.isEmpty(id)) {
                    return if (id.startsWith("raw:")) {
                        id.replaceFirst("raw:".toRegex(), "")
                    } else if (id.startsWith("msf:")) {
                        getDataColumn(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, "_id=?",
                            arrayOf(id.replaceFirst("msf:".toRegex(), "")))
                    } else try {
                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/all_downloads"),
                            java.lang.Long.valueOf(id)
                        )
                        getDataColumn(
                            context,
                            contentUri,
                            null,
                            null
                        )
                    } catch (e: NumberFormatException) {
                        null
                    }
                }
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
                return getDataColumn(
                    context,
                    contentUri,
                    selection,
                    selectionArgs
                )
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) {
                uri.lastPathSegment
            } else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        if (uri == null) {
            sLogger.warn("The Uri is null when getting data column.")
            return null
        }
        sLogger.warn("URI = $uri")
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            cursor = context.contentResolver.query(
                uri, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    fun readTextFromFile(file: File): String {
        val text = StringBuilder()

        try {
            val fis = FileInputStream(file)
            val isr = InputStreamReader(fis)
            val br = BufferedReader(isr)

            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line).append("\n")
            }

            br.close()
            isr.close()
            fis.close()
        } catch (e: Exception) {
            sLogger.error(e.message,e)
        }

        return text.toString()
    }

    fun getMiniAppPath(context: Context, appId: String, appVersion: String): String {
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(appVersion)) return ""
        val filePathBuilder = StringBuilder()
        filePathBuilder
            .append(context.getDir("miniApps", Context.MODE_PRIVATE))
            .append(File.separator)
            .append(appId)
            .append(File.separator)
            .append(appVersion)
        return filePathBuilder.toString()
    }

    fun readFileToByteArray(filePath: String): ByteArray? {
        val file = File(filePath)
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int

        try {
            FileInputStream(file).use { inputStream ->
                while (inputStream.read(buffer).also { length = it } != -1) {
                    outputStream.write(buffer, 0, length)
                }
                return outputStream.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }


    fun byteArrayToBase64(byteArray: ByteArray): String {
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP);
        return base64String
    }

    fun base64ToByteArray(base64String: String): ByteArray {
        val byteArray = Base64.decode(base64String, Base64.NO_WRAP);
        return byteArray
    }
    fun getFileSizeFromUri(context: Context, uri: Uri?): Long {
        var fileSize: Long = 0
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri!!, null, null, null, null)
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    fileSize = cursor.getLong(sizeIndex)
                }
            } finally {
                try {
                    cursor.close()
                } catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
        return fileSize
    }
    fun getFileLastModifiedFromUri(context: Context, uri: Uri?): Long {
        var lastModified: Long = 0
//        val contentResolver = context.contentResolver
//        val cursor = contentResolver.query(uri!!, null, null, null, null)
//        if (cursor != null) {
//            try {
//                if (cursor.moveToFirst()) {
//                    val dateModifiedIndex =
//                        cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
//                    lastModified =
//                        cursor.getLong(dateModifiedIndex) * 1000 // Convert seconds to milliseconds
//                }
//            } finally {
//                try {
//                    cursor.close()
//                } catch (e:Exception){
//                    e.printStackTrace()
//                }
//            }
//        }
        return lastModified
    }

    fun getFileNameFromUri(context: Context, uri: Uri?): String? {
        var fileName: String? = null
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri!!, null, null, null, null)
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    fileName = cursor.getString(nameIndex)
                }
            } finally {
                cursor.close()
            }
        }
        return fileName
    }
    fun isUri(input: String): Boolean {
        return input.startsWith("content://") || input.startsWith("file://")
    }
    fun deleteFileFromUri(context: Context, uri: Uri?): Boolean {
        val contentResolver = context.contentResolver
        val rowsDeleted = contentResolver.delete(uri!!, null, null)
        return rowsDeleted > 0
    }
}