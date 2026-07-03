/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.core.utils.common

import android.annotation.SuppressLint
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
import androidx.documentfile.provider.DocumentFile
import com.blankj.utilcode.util.Utils
import com.blankj.utilcode.util.ZipUtils
import com.ct.ertclib.dc.core.common.PathManager
import com.ct.ertclib.dc.core.manager.common.LicenseManager
import com.ct.ertclib.dc.core.utils.logger.Logger
import okhttp3.internal.closeQuietly
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.FilenameUtils
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.MessageDigest
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
        // 如果是文件，直接删除
        if (file.isFile) {
            return file.delete()
        }
        // 如果是目录，递归删除
        if (file.isDirectory) {
            val listFiles = file.listFiles()
            if (!listFiles.isNullOrEmpty()) {
                listFiles.forEach {
                    if (!deleteFile(it)) {
                        return false
                    }
                }
            }
            return file.delete()
        }
        return false
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

    fun unzipFile(srcPathStr: String, desPathStr: String) {
        val srcFile = File(srcPathStr)
        if (!srcFile.exists()) {
            return
        }
        val desFile = File(desPathStr)
        if (!desFile.exists()) {
            val mkdirsResult = desFile.mkdirs()
            if (!mkdirsResult) {
                sLogger.warn("can not create ${desFile.path}")
                return
            }
        }
        val zipInputStream = ZipInputStream(srcFile.inputStream())
        try {
            while (true) {
                val nextEntry = zipInputStream.nextEntry
                if (nextEntry != null) {
                    if (nextEntry.isDirectory) {
                        val nextPath = FilenameUtils.normalize(desPathStr + "/" + nextEntry.name)
                        if (File(nextPath).mkdirs()) {
                            throw IOException("nextPath: $nextPath create dirs error")
                        }
                    } else {
                        val normalizePath = FilenameUtils.normalize(desPathStr + "/" + nextEntry.name)
                        val file = File(normalizePath)
                        val parentFile = file.parentFile
                        if (parentFile != null) {
                            if (!parentFile.exists() && !parentFile.mkdirs()) {
                                throw IOException("$parentFile create dirs error")
                            } else if (!file.createNewFile()) {
                                throw IOException("$file create file error")
                            } else {
                                var fileOutputStream: FileOutputStream? = null
                                try {
                                    fileOutputStream = FileOutputStream(file)
                                    val bufferArray = ByteArray(1024)
                                    while (true) {
                                        val read = zipInputStream.read(bufferArray)
                                        if (read == -1) {
                                            break
                                        }
                                        fileOutputStream.write(bufferArray, 0, read)
                                    }
                                } catch (e: Exception) {
                                    sLogger.warn(e.message)
                                } finally {
                                    try {
                                        fileOutputStream?.close()
                                    } catch (e: Exception) {
                                        sLogger.warn(e.message)
                                    }
                                }
                            }
                        }
                    }
                    zipInputStream.closeEntry()
                } else {
                    //跳出循环
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            zipInputStream.closeQuietly()
        }
    }

    fun untarFile(srcPathStr: String, desPathStr: String) :Boolean{
        val srcFile = File(srcPathStr)
        if (!srcFile.exists()) {
            return false
        }
        val desFile = File(desPathStr)
        if (!desFile.exists()) {
            val mkdirsResult = desFile.mkdirs()
            if (!mkdirsResult) {
                sLogger.warn("can not create ${desFile.path}")
                return false
            }
        }
        val tarInputStream = TarArchiveInputStream(srcFile.inputStream())
        try {
            while (true) {
                val nextEntry = tarInputStream.nextEntry
                if (nextEntry != null) {
                    if (nextEntry.isDirectory) {
                        val nextPath = desPathStr + "/" + nextEntry.name
                        if (File(nextPath).mkdirs()) {
                            throw IOException("nextPath: $nextPath create dirs error")
                        }
                    } else {
                        val file = File(desPathStr + "/" + nextEntry.name)
                        val parentFile = file.parentFile
                        if (parentFile != null) {
                            if (!parentFile.exists() && !parentFile.mkdirs()) {
                                throw IOException("$parentFile create dirs error")
                            } else if (!file.createNewFile()) {
                                throw IOException("$file create file error")
                            } else {
                                try {
                                    FileOutputStream(file).use { fileOutputStream ->
                                        val bufferArray = ByteArray(1024)
                                        while (true) {
                                            val read = tarInputStream.read(bufferArray)
                                            if (read == -1) break
                                            fileOutputStream.write(bufferArray, 0, read)
                                        }
                                    }
                                } catch (e: Exception) {
                                    sLogger.warn("Error during file extraction: ${e.message}")
                                }
                            }
                        }
                    }
                } else {
                    //跳出循环
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            tarInputStream.closeQuietly()
        }
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

    fun getFileMD5FromUri(context: Context, uri: Uri): String? {
        val digest = MessageDigest.getInstance("MD5")
        var inputStream: InputStream? = null

        return try {
            // 通过 Uri 获取 InputStream
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return null
            }

            // 读取文件并更新 MD5 计算
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }

            // 将计算结果转换为 32 位小写字符串
            val md5Bytes = digest.digest()
            BigInteger(1, md5Bytes).toString(16).padStart(32, '0')
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            // 关闭 InputStream
            inputStream?.close()
        }
    }

    fun isUriFolder(context: Context, uri: Uri): Boolean {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        return documentFile?.isDirectory == true
    }

    fun copyUriToCache(context: Context, uri: Uri, fileName: String?): File? {
        return try {
            val cacheDir = File("/sdcard/ctnewcall/cache/")

            if (!cacheDir.exists()) {
                val created = cacheDir.mkdirs()
                if (!created) {
                    sLogger.error("Failed to create cache directory: ${cacheDir.absolutePath}")
                    return null
                }
            }

            val cacheFile = File(cacheDir, "temp_${System.currentTimeMillis()}_$fileName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: Exception) {
            sLogger.error("Failed to copy URI to cache", e)
            null
        }
    }

    fun installBootstrapMiniApp(eTag: String, bytes: ByteArray){
        val filePathBuilder = StringBuilder()
        filePathBuilder
            .append(Utils.getApp().getDir("miniApps", Context.MODE_PRIVATE))
            .append(File.separator)
            .append("bootstrap")
            .append(File.separator)
            .append(eTag)
        val filePath = filePathBuilder.toString()
        if (!isFileExists(filePath)) {
            //不存在存储小程序
            try {
                //将数据写到沙盒cache里面
                val cacheFile = PathManager().createCacheFile(Utils.getApp(),fileName = "tmp_miniapp.zip")
                if (cacheFile == null) {
                    sLogger.debug("installBootstrapMiniApp Failed createCacheFile null")
                    return
                }
                val fileOutputStream = FileOutputStream(cacheFile)
                fileOutputStream.write(bytes)
                fileOutputStream.close()
                //校验小程序签名
                if(!LicenseManager.getInstance().verifyMiniAppPkg(cacheFile!!.absolutePath)){
                    deletePath(cacheFile.absolutePath)
                    sLogger.debug("installBootstrapMiniApp Failed verifyMiniAppPkg false")
                    return
                }
                //解压小程序
                ZipUtils.unzipFile(cacheFile.absolutePath, filePath)
                //删除cache
                deletePath(cacheFile.absolutePath)
                if (sLogger.isDebugActivated) {
                    sLogger.debug("installBootstrapMiniApp filePath:$filePath")
                }
            } catch (e: IOException) {
                if (sLogger.isDebugActivated) {
                    sLogger.error("installBootstrapMiniApp ", e)
                }
                return
            }
        }
    }

    /**
     * 获取已安装的最新版本路径
     * @return 最新版本的完整路径，如果没有则返回null
     */
    fun getLatestInstalledBootstrapPath(): String? {
        val bootstrapDir = File(Utils.getApp().getDir("miniApps", Context.MODE_PRIVATE), "bootstrap")

        if (!bootstrapDir.exists() || !bootstrapDir.isDirectory) {
            return null
        }

        val versionDirs = bootstrapDir.listFiles { file -> file.isDirectory }

        if (versionDirs.isNullOrEmpty()) {
            return null
        }

        // 按目录名（eTag）排序，找出最新的版本
        val latestDir = versionDirs.maxByOrNull { it.name }

        return latestDir?.absolutePath
    }

    /**
     * 获取已安装的最新版本号（ETag）
     * @return 最新版本的ETag，如果没有则返回null
     */
    fun getLatestInstalledBootstrapVersion(): String? {
        val bootstrapDir = File(Utils.getApp().getDir("miniApps", Context.MODE_PRIVATE), "bootstrap")

        if (!bootstrapDir.exists() || !bootstrapDir.isDirectory) {
            return null
        }

        val versionDirs = bootstrapDir.listFiles { file -> file.isDirectory }

        if (versionDirs.isNullOrEmpty()) {
            return null
        }

        // 按目录名（eTag）排序，找出最新的版本
        val latestDir = versionDirs.maxByOrNull { it.name }

        return latestDir?.name
    }


}