package com.ct.ertclib.dc.core.common

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.ct.ertclib.dc.core.data.model.FileEntity

class FileScanner(private val context: Context) {

    // 主扫描方法
    fun scanAllFiles(): List<FileEntity> {
        val results = mutableListOf<FileEntity>()

        // 1. 扫描常规存储目录
        scanNormalDirectories(results)

        // 2. 扫描Android/data和Android/obb目录
        scanRestrictedDirectories(results)

        return results
    }

    // 扫描普通目录
    private fun scanNormalDirectories(results: MutableList<FileEntity>) {
        val storageDirs = getStorageDirectories()
        storageDirs.forEach { dir ->
            if (dir.exists() && dir.canRead()) {
                scanDirectoryRecursive(dir, results)
            }
        }
    }

    // 扫描受限目录(Android/data, Android/obb),暂时不实现
    private fun scanRestrictedDirectories(results: MutableList<FileEntity>) {
//        val dataDir = File(Environment.getExternalStorageDirectory(), "Android/data")
//        val obbDir = File(Environment.getExternalStorageDirectory(), "Android/obb")

//        scanViaSaf(dataDir, results)
//        scanViaSaf(obbDir, results)

    }

    // 递归扫描普通目录
    private fun scanDirectoryRecursive(dir: File, results: MutableList<FileEntity>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (!shouldSkipDirectory(file)) {
                    scanDirectoryRecursive(file, results)
                }
            } else {
                results.add(createFileItem(file))
            }
        }
    }

    // 通过Storage Access Framework扫描受限目录
//    private suspend fun scanViaSaf(directory: File, results: MutableList<FileEntity>) {
//        val uri = getSafUriForDirectory(directory) ?: return
//
//        withContext(Dispatchers.IO) {
//            val treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(
//                uri,
//                DocumentsContract.getTreeDocumentId(uri)
//            )
//
//            context.contentResolver.query(
//                treeUri,
//                arrayOf(
//                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
//                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
//                    DocumentsContract.Document.COLUMN_SIZE,
//                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
//                    DocumentsContract.Document.COLUMN_MIME_TYPE
//                ),
//                null, null, null
//            )?.use { cursor ->
//                while (cursor.moveToNext()) {
//                    val docId = cursor.getString(0)
//                    val name = cursor.getString(1)
//                    val size = cursor.getLong(2)
//                    val lastModified = cursor.getLong(3)
//                    val mimeType = cursor.getString(4)
//
//                    val isDir = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
//                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
//
//                    results.add(FileEntity(
//                        name = name,
//                        path = getPathFromUri(fileUri),
//                    ))
//
//                    if (isDir) {
//                        scanSafDirectoryRecursive(fileUri, results)
//                    }
//                }
//            }
//        }
//    }

    // 递归扫描SAF目录
    private suspend fun scanSafDirectoryRecursive(uri: Uri, results: MutableList<FileEntity>) {
        withContext(Dispatchers.IO) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                uri,
                DocumentsContract.getDocumentId(uri)
            )

            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(0)
                    val name = cursor.getString(1)
                    val size = cursor.getLong(2)
                    val lastModified = cursor.getLong(3)
                    val mimeType = cursor.getString(4)

                    val isDir = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)

                    results.add(FileEntity(
                        name = name,
                        path = getPathFromUri(fileUri),
                    ))

                    if (isDir) {
                        scanSafDirectoryRecursive(fileUri, results)
                    }
                }
            }
        }
    }

    // 从URI获取路径
    private fun getPathFromUri(uri: Uri): String {
        return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.toString()
    }

    // 创建文件项
    private fun createFileItem(file: File): FileEntity {
        return FileEntity(
            name = file.name,
            path = file.absolutePath
        )
    }

    // 获取所有存储目录
    private fun getStorageDirectories(): List<File> {
        val dirs = mutableListOf<File>()

        // 添加主存储
        dirs.add(Environment.getExternalStorageDirectory())

        // 添加二级存储
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val storageVolumes = context.getSystemService(StorageManager::class.java)?.storageVolumes
            storageVolumes?.forEach { volume ->
                volume.directory?.let { dirs.add(it) }
            }
        }
        return dirs
    }

    // 应跳过的目录
    private fun shouldSkipDirectory(dir: File): Boolean {
        val skipList = listOf(
            ".",
            "lost+found",
            "cache",
            "temp"
        )
        return skipList.any { dir.absolutePath.contains(it) }
    }
}