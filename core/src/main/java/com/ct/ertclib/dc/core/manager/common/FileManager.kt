package com.ct.ertclib.dc.core.manager.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ct.ertclib.dc.core.common.FileScanner
import com.ct.ertclib.dc.core.common.NewCallDatabase
import com.ct.ertclib.dc.core.data.model.FileEntity
import com.ct.ertclib.dc.core.utils.logger.Logger

class FileManager {
    private val fileDao = NewCallDatabase.getInstance().FileDao()
    private var isScanning = false

    companion object {
        private const val TAG = "FileManager"
        val instance: FileManager by lazy {
            FileManager()
        }
    }
    private val sLogger: Logger = Logger.getLogger(TAG)

    fun hasFileIndex(): Boolean{
        return fileDao.queryFileCount() > 0
    }

    fun updateFiles(context: Context){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            sLogger.info("updateFiles no permission")
            return
        }
        if (isScanning){
            return
        }
        isScanning = true
        sLogger.info("updateFiles")
        val fileScanner = FileScanner(context)
        val list = fileScanner.scanAllFiles()
        fileDao.upsertFiles(list)
        isScanning = false
    }

    fun searchFilesByName(name: String): List<FileEntity> {
        return fileDao.queryFiles(name)
    }

    fun searchFilesByKeyWords(vararg keywords: String): List<FileEntity> {
        if (keywords.isEmpty()) return emptyList()
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        keywords.forEach { keyword ->
            clauses.add("name LIKE ?")
            args.add("%$keyword%")
        }
        val sql = "SELECT * FROM files WHERE ${clauses.joinToString(" OR ")}"
        return fileDao.queryFilesByKeywords(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }
}