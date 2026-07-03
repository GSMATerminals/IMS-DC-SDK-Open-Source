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

package com.ct.ertclib.dc.core.usecase.miniapp

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Criteria
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ZipUtils
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.common.PathManager
import com.ct.ertclib.dc.core.constants.MiniAppConstants.KEY_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_DOWNLOAD_EVENT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_DOWNLOAD_URL
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_EXTRA_INFO
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_FILE_NAME
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_FILE_PATH
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_MODEL
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.TTL
import com.ct.ertclib.dc.core.constants.MiniAppConstants.VALUE_PARAM
import com.ct.ertclib.dc.core.data.bridge.JSResponse
import com.ct.ertclib.dc.core.data.common.DownloadData
import com.ct.ertclib.dc.core.data.common.MediaInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppPermissions
import com.ct.ertclib.dc.core.data.miniapp.ModelInfo
import com.ct.ertclib.dc.core.data.model.ModelEntity
import com.ct.ertclib.dc.core.manager.common.FileManager
import com.ct.ertclib.dc.core.manager.common.SPManager
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.port.listener.IDownloadListener
import com.ct.ertclib.dc.core.port.manager.IFileDownloadManager
import com.ct.ertclib.dc.core.port.manager.IModelManager
import com.ct.ertclib.dc.core.port.usecase.mini.IFileMiniEventUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.SystemUtils
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections

class FileMiniUseCase(
    private val permissionMiniUseCase: IPermissionUseCase,
    private val modelManager: IModelManager,
    private val fileDownloadManager: IFileDownloadManager
) : IFileMiniEventUseCase {

    companion object {
        private const val TAG = "FileMiniUseCase"
        private const val FILE_MODEL_PATH = "model"
    }

    private val logger = Logger.getLogger(TAG)
    private var mFileInputStream: InputStream? = null
    private var mFileOutputStream: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())


    override fun getLocation(
        miniAppView: MiniAppView,
        handler: CompletionHandler<String?>
    ) {
        logger.info("getLocation")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_LOCATION))) {
                val response = JSResponse("1", "permission not granted", "")
                handler.complete(JsonUtil.toJson(response))
                logger.warn("getLocation, permission not granted, return")
                return
            }
        } ?: run {
            val response = JSResponse("1", "appInfo is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.warn("getLocation, appInfo is null, return")
            return
        }
        scope.launch {
            withContext(Dispatchers.Main) {
                requestLocation(miniAppView, handler)
            }
        }
    }

    override fun selectFile(miniAppView: MiniAppView, handler: CompletionHandler<String?>) {
        logger.info("selectFile")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                val response = JSResponse("1", "permission not granted", "")
                handler.complete(JsonUtil.toJson(response))
                logger.warn("selectFile, permission not granted, return")
                return
            }
        } ?: run {
            val response = JSResponse("1", "appInfo is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.warn("selectFile, appInfo is null, return")
            return
        }
        val fileTypes = null
        miniAppView.viewModel.selectFiles(1, fileTypes, object : OnPickMediaCallbackListener {
            override fun onCancel() {
                logger.info("selectFile, cancel")
            }

            override fun onResult(result: List<MediaInfo>) {
                if (result.isNotEmpty()){
                    val jsResponse = JSResponse(
                        "0",
                        "success",
                        mutableMapOf(
                            "path" to result[0].path,
                            "size" to result[0].size,
                            "lastModified" to result[0].lastModified,
                            "name" to result[0].displayName
                        )
                    )
                    handler.complete(JsonUtil.toJson(jsResponse))
                    return
                }
            }

        })
    }

    override fun selectFiles(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.info("selectFiles")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                val response = JSResponse("1", "permission not granted", "")
                handler.complete(JsonUtil.toJson(response))
                logger.warn("selectFiles, permission not granted, return")
                return
            }
        } ?: run {
            val response = JSResponse("1", "appInfo is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.warn("selectFiles, appInfo is null, return")
            return
        }
        val maxSelectable = (params["maxSelectable"] as? Number)?.toInt() ?: 9
        val fileTypes = params["fileTypes"] as List<String>?
        miniAppView.viewModel.selectFiles(maxSelectable, fileTypes, object : OnPickMediaCallbackListener {
            override fun onCancel() {
                logger.info("selectFiles, cancel")
            }

            override fun onResult(result: List<MediaInfo>) {
                logger.info("selectFiles success")
                val resultList = ArrayList<Map<String, Any?>>()
                for (mediaInfo in result) {
                    resultList.add(
                        mutableMapOf(
                            "path" to mediaInfo.path,
                            "size" to mediaInfo.size,
                            "lastModified" to mediaInfo.lastModified,
                            "name" to mediaInfo.displayName
                        )
                    )
                }
                val jsResponse = JSResponse(
                    "0",
                    "success",
                    resultList
                )
                handler.complete(JsonUtil.toJson(jsResponse))
            }

        })
    }

    override fun saveFile(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.info("saveFile")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                val response = JSResponse("1", "permission not granted", "")
                handler.complete(JsonUtil.toJson(response))
                logger.warn("saveFile, permission not granted, return")
                return
            }
        } ?: run {
            val response = JSResponse("1", "appInfo is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.warn("saveFile, appInfo is null, return")
            return
        }
        val data = params["data"]
        if (data == null) {
            val response = JSResponse("1", "saveFile data is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.info("JSApi asyn saveFile data is null")
            return
        }
        if (mFileOutputStream == null) {
            val response = JSResponse("1", "JSApi async saveFile did not call startSaveFile", "")
            handler.complete(JsonUtil.toJson(response))
            logger.info("JSApi sync saveFile did not call startSaveFile")
            return
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                val dataByteArray = FileUtils.base64ToByteArray(data as String)
                mFileOutputStream?.write(dataByteArray)
                val response = JSResponse("0", "success", "")
                handler.complete(JsonUtil.toJson(response))
            }
        }
    }

    override fun readFile(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.info("readFile")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                val response = JSResponse("1", "permission not granted", "")
                handler.complete(JsonUtil.toJson(response))
                logger.warn("readFile, permission not granted, return")
                return
            }
        } ?: run {
            val response = JSResponse("1", "appInfo is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.warn("readFile, appInfo is null, return")
            return
        }
        val length = params["length"]
        if (length == null) {
            val response = JSResponse("1", "readFile length is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.info("JSApi async readFile length is null")
            return
        }
        val lengthInt = (length as String).toInt()
        if (lengthInt >1000000) {
            val response = JSResponse("1", "readFile length is too long, should less then 100000", "")
            handler.complete(JsonUtil.toJson(response))
            logger.debug("JSApi async readFile length is too long,should less then 100000")
            return
        }
        if (mFileInputStream == null) {
            val response = JSResponse("1", "JSApi async readFile did not call startReadFile", "")
            handler.complete(JsonUtil.toJson(response))
            logger.debug("JSApi sync readFile did not call startReadFile")
            return
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                val buffer = ByteArray(lengthInt)
                val readBuffer: ByteArray
                val read: Int = mFileInputStream!!.read(buffer)
                if (read <= 0) {
                    val response = JSResponse("0", "success", hashMapOf("isEnd" to true,"base64Data" to ""))
                    handler.complete(JsonUtil.toJson(response))
                    return@withContext
                }
                if (read < lengthInt) {
                    readBuffer = ByteArray(read)
                    System.arraycopy(buffer, 0, readBuffer, 0, read)
                } else {
                    readBuffer = buffer
                }
                val base64 = FileUtils.byteArrayToBase64(readBuffer)
                scope.launch {
                    withContext(Dispatchers.Main) {
                        val response = JSResponse("0", "success", hashMapOf("isEnd" to false,"base64Data" to base64))
                        handler.complete(JsonUtil.toJson(response))
                    }
                }
            }
        }
    }

    override fun decompressFile(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.info("decompressFile")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                val response = JSResponse("1", "permission not granted", "")
                handler.complete(JsonUtil.toJson(response))
                logger.warn("decompressFile, permission not granted, return")
                return
            }
        } ?: run {
            val response = JSResponse("1", "appInfo is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.warn("decompressFile, appInfo is null, return")
            return
        }
        val srcPath = params["srcPath"]
        val desPath = params["desPath"]
        val compressType = params["compressType"]

        val response = JSResponse("0", "decompressFile", listOf<String>())
        if (srcPath == null || desPath == null || compressType == null) {
            logger.debug("JSApi sync decompressFile srcPath or desPath or compressType is null")
            response.code = "1"
            response.message = "srcPath or desPath or compressType is null"
            response.data = emptyList()
            handler.complete(JsonUtil.toJson(response))
            return
        }

        var srcPathStr = srcPath as String
        val desPathStr = desPath as String
        val compressTypeStr = compressType as String
        if (!isMiniAppPrivatePath(miniAppView, desPathStr)) {//严格判断只能是path，所以用startsWith
            logger.debug("JSApi sync decompressFile desPathStr path not in privateFolder")
            response.code = "1"
            response.message = "desPathStr not in privateFolder"
            response.data = emptyList()
            handler.complete(JsonUtil.toJson(response))
            return
        }
        if (FileUtils.isUri(srcPathStr)){
            val filePath = com.blankj.utilcode.util.UriUtils.uri2File(Uri.parse(srcPathStr)).absolutePath
            if (!TextUtils.isEmpty(filePath)){
                srcPathStr = filePath
            }
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                //根据压缩类型 compressType把压缩文件解压到对应路径
                try {
                    var ok = false
                    if (compressTypeStr == "zip") {
                        ZipUtils.unzipFile(srcPathStr, desPath)
                        ok = true
                    } else if (compressTypeStr == "tar" || compressTypeStr == "gz") {
                        ok = FileUtils.untarFile(srcPathStr, desPathStr)
                    }

                    scope.launch {
                        withContext(Dispatchers.Main) {
                            if (ok){
                                handler.complete(JsonUtil.toJson(JSResponse("0", "success",null)))
                            } else {
                                handler.complete(JsonUtil.toJson(JSResponse("0", "fail",null)))
                            }
                        }
                    }
                } catch (e:Exception){
                    scope.launch {
                        withContext(Dispatchers.Main) {
                            handler.complete(JsonUtil.toJson(JSResponse("0", "fail",null)))
                        }
                    }
                }
            }
        }
    }

    override fun getFileListAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(getFileList(miniAppView,params))
    }

    override fun getFileList(miniAppView: MiniAppView, params: Map<String, Any>): String? {
        logger.info("getFileList")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("getFileList, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("getFileList, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val path = params["folderPath"]//必填
        val offset = params["offset"]//必填
        val count = params["count"]//必填
        val fileType = params["fileType"]//可空，空或"0":文件和文件夹，"1":文件，"2":文件夹
        val suffix = params["suffix"]//对文件进行过滤，可空
        val sortType = params["sortType"]//可空，排序规则，"sizeSort"，"timeSort"
        val sortOrder = params["sortOrder"]//根据sortType字段确定，"sizeSort"("1":从大到小，"2":从小到大);"timeSort"("1":最新修改在前，"2":最新修改在后)
        val folderPosition = params["folderPosition"]//文件夹位置，可空，空或"0":默认，"1":文件夹放在前面，"2":文件夹放在后面

        val response = JSResponse("0", "getFileList", mutableListOf<HashMap<String,Any>>())
        // 必填参数校验
        var errMsg = ""
        if (path == null || TextUtils.isEmpty(path as String)) {
            errMsg = "JSApi sync getFileList path is null"
        } else if (offset == null || TextUtils.isEmpty(offset as String)) {
            errMsg = "JSApi sync getFileList offset is null"
        } else if (count == null || TextUtils.isEmpty(count as String)) {
            errMsg = "JSApi sync getFileList count is null"
        }
        if (!TextUtils.isEmpty(errMsg)){
            logger.error(errMsg)
            response.code = "1"
            response.message = errMsg
            response.data = mutableListOf()
            return JsonUtil.toJson(response)
        }

        val folderPath = path as String
        val offsetInt = (offset as String).toInt()
        val countInt = (count as String).toInt()
        val fileTypeInt = if (fileType == null) 0 else (fileType as String).toInt()
        val suffixStr = if (suffix == null) "" else (suffix as String)
        val sortTypeStr = if (sortType == null) "" else (sortType as String)
        val sortOrderInt = if (sortOrder == null) 0 else (sortOrder as String).toInt()
        val folderPositionInt = if (folderPosition == null) 0 else (folderPosition as String).toInt()

        //获取指定目录下的文件
        val suffixSplit = suffixStr.split("||")
        val suffixArray = mutableListOf<String>()
        for(su in suffixSplit){
            if (!TextUtils.isEmpty(su) ){
                suffixArray.add(su)
            }
        }
        //过滤规则
        val filter = object : FileFilter {
            override fun accept(file: File?): Boolean {
                file?.let {
                    val fileTypeBoolean = when(fileTypeInt){
                        0 ->  true
                        1 -> !it.isDirectory
                        2 -> it.isDirectory
                        else -> false
                    }
                    var suffixBoolean = true
                    if(suffixArray.isNotEmpty() && !it.isDirectory){//说明需要根据后缀筛选
                        suffixBoolean = false
                        for(su in suffixArray){
                            if (it.name.endsWith(su)){
                                suffixBoolean = true
                                break
                            }
                        }
                    }
                    return fileTypeBoolean && suffixBoolean
                }
                return false
            }
        }
        //排序
        val fileList = com.blankj.utilcode.util.FileUtils.listFilesInDirWithFilter(folderPath,filter)
        val sortComparator: java.util.Comparator<File>? = when(sortTypeStr){
            "sizeSort" -> when(sortOrderInt){//"1":从大到小，"2":从小到大
                1 -> Comparator { o1, o2 ->
                    (o2.length() - o1.length()).toInt()
                }
                2 -> Comparator { o1, o2 ->
                    (o1.length() - o2.length()).toInt()
                }
                else -> null
            }
            "timeSort" -> when(sortOrderInt){//"1":最新修改在前，"2":最新修改在后
                1 -> Comparator { o1, o2 ->
                    (o2.lastModified() - o1.lastModified()).toInt()
                }
                2 -> Comparator { o1, o2 ->
                    (o1.lastModified() - o2.lastModified()).toInt()
                }
                else -> null
            }
            else -> null
        }
        if (sortComparator!=null){
            Collections.sort(fileList, sortComparator)
        }
        //文件夹位置
        val folderComparator = when(folderPositionInt){
            1 -> Comparator<File> { o1, o2 ->
                var result = 0
                if (o1.isDirectory && !o2.isDirectory){
                    result = 1
                } else if (!o1.isDirectory && o2.isDirectory){
                    result = -1
                }
                result
            }
            2 -> Comparator { o1, o2 ->
                var result = 0
                if (o1.isDirectory && !o2.isDirectory){
                    result = -1
                } else if (!o1.isDirectory && o2.isDirectory){
                    result = 1
                }
                result
            }
            else -> null
        }
        if (folderComparator!=null){
            Collections.sort(fileList, folderComparator)
        }
        //分页
        val fileListSize = fileList.size
        if (offsetInt > fileListSize) {
            if (logger.isDebugActivated) {
                logger.debug("JSApi sync getFileList offsetInt > fileListSize")
            }
            response.code = "1"
            response.message = "offsetInt > fileListSize"
            response.data = mutableListOf()
            return JsonUtil.toJson(response)
        }
        val end =
            if (offsetInt + countInt > fileListSize) fileListSize else offsetInt + countInt
        val subList = fileList.subList(offsetInt, end)
        val result = mutableListOf<HashMap<String,Any>>()
        subList.forEach {
            result.add(hashMapOf(
                "path" to it.absolutePath,
                "size" to it.length(),
                "lastModified" to it.lastModified(),
                "isDirectory" to it.isDirectory(),
                "name" to it.name))
        }
        response.code = "0"
        response.message = "success"
        response.data = result
        return JsonUtil.toJson(response)
    }

    override fun getPrivateFolderAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(getPrivateFolder(miniAppView,params))
    }

    override fun getPrivateFolder(
        miniAppView: MiniAppView,
        params: Map<String, Any>
    ): String? {
        logger.info("getPrivateFolder")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("getPrivateFolder, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("getPrivateFolder, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val type = params["type"]//必填
        val response = JSResponse("0", "getPrivateFolder", mutableMapOf<String,Any>())
        if (type == null) {
            logger.debug("JSApi sync getPrivateFolder type is null")
            response.code = "1"
            response.message = "type is null"
            return JsonUtil.toJson(response)
        }
        val typeStr = type as String
        response.code = "0"
        response.message = "success"
        response.data = mutableMapOf("privateFolder" to miniAppFilePath(miniAppView, typeStr))
        return JsonUtil.toJson(response)
    }

    override fun startSaveFileAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(startSaveFile(miniAppView,params))
    }

    override fun startSaveFile(
        miniAppView: MiniAppView,
        params: Map<String, Any>
    ): String? {
        logger.debug("startSaveFile")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("startSaveFile, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("startSaveFile, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val path = params["path"]
        val append = params["append"]
        val response = JSResponse("0", "startSaveFile", "")
        if (path == null || append == null) {
            if (logger.isDebugActivated) {
                logger.debug("JSApi sync startSaveFile path or append is null")
            }
            response.code = "1"
            response.message = "path is or append null"
            response.data = ""
            return JsonUtil.toJson(response)
        }
        val pathStr = path as String
        val appendBoolean = append as Boolean
        if (!isMiniAppPrivatePath(miniAppView, pathStr)) {//严格判断只能是path，所以用startsWith
            if (logger.isDebugActivated) {
                logger.debug("JSApi sync startSaveFile path not in privateFolder")
            }
            response.code = "1"
            response.message = "path not in privateFolder"
            response.data = ""
            return JsonUtil.toJson(response)
        }
        if (pathStr.contains(":")) {//有些手机文件名不能有冒号
            if (logger.isDebugActivated) {
                logger.debug("JSApi sync startSaveFile path cannot contains :")
            }
            response.code = "1"
            response.message = "path cannot contains :"
            response.data = ""
            return JsonUtil.toJson(response)
        }
        try {
            mFileOutputStream?.close()
        }catch (e:Exception){
            e.printStackTrace()
        }
        try {
            val dirPath = pathStr.substring(0,pathStr.lastIndexOf(File.separator))
            val dir = File(dirPath)
            if (!dir.exists()){
                dir.mkdirs()
            }

            mFileOutputStream = FileOutputStream(File(pathStr),appendBoolean)
        } catch (e:Exception){
            response.code = "1"
            response.message = "startSaveFile err $e"
            response.data = ""
            return JsonUtil.toJson(response)
        }
        response.code = "0"
        response.message = "success"
        return JsonUtil.toJson(response)
    }

    override fun stopSaveFileAsync(miniAppView: MiniAppView, handler: CompletionHandler<String?>) {
        handler.complete(stopSaveFile(miniAppView))
    }

    override fun stopSaveFile(miniAppView: MiniAppView): String? {
        logger.debug("stopSaveFile")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("stopSaveFile, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("stopSaveFile, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val response = JSResponse("0", "stopSaveFile", "")
        try {
            mFileOutputStream?.close()
        }catch (e:Exception){
            e.printStackTrace()
        }
        mFileOutputStream = null
        response.code = "0"
        response.message = "success"
        return JsonUtil.toJson(response)
    }

    override fun startReadFileAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(startReadFile(miniAppView,params))
    }

    override fun startReadFile(
        miniAppView: MiniAppView,
        params: Map<String, Any>
    ): String? {
        logger.debug("startReadFile")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("startReadFile, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("startReadFile, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val path = params["path"]

        val response = JSResponse("0", "startReadFile", "")
        if (path == null) {
            if (logger.isDebugActivated) {
                logger.debug("JSApi sync startReadFile path is null")
            }
            response.code = "1"
            response.message = "path is null"
            response.data = ""
            return JsonUtil.toJson(response)
        }
        val pathStr = path as String
        try {
            mFileInputStream?.close()
        }catch (e:Exception){
            e.printStackTrace()
        }
        try {
            // 路径有可能是uri也可能是path
            mFileInputStream = if (FileUtils.isUri(pathStr)){
                miniAppView.context.contentResolver.openInputStream(Uri.parse(pathStr))
            } else {
                val file = File(pathStr)
                FileInputStream(file)
            }
        } catch (e:Exception){
            response.code = "1"
            response.message = "startReadFile err $e"
            response.data = ""
            return JsonUtil.toJson(response)
        }
        response.code = "0"
        response.message = "success"
        return JsonUtil.toJson(response)
    }

    override fun stopReadFileAsync(miniAppView: MiniAppView, handler: CompletionHandler<String?>) {
        handler.complete(stopReadFile(miniAppView))
    }

    override fun stopReadFile(miniAppView: MiniAppView): String? {
        logger.debug("stopReadFile")

        val response = JSResponse("0", "stopReadFile", "")
        try {
            mFileInputStream?.close()
        }catch (e:Exception){
            e.printStackTrace()
        }
        mFileInputStream = null
        response.code = "0"
        response.message = "success"
        return JsonUtil.toJson(response)
    }

    override fun checkFileOrFolderExistsAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(checkFileOrFolderExists(miniAppView,params))
    }

    override fun checkFileOrFolderExists(
        miniAppView: MiniAppView,
        params: Map<String, Any>
    ): String? {
        logger.debug("checkFileOrFolderExists")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("checkFileOrFolderExists, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("checkFileOrFolderExists, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val path = params["path"]
        //检测文件或文件夹是否存在
        val response = JSResponse("0", "checkFileOrFolderExists",mutableListOf<HashMap<String,Any>>())
        if (path == null) {
            logger.debug("JSApi sync checkFileOrFolderExists path is null")
            response.code = "1"
            response.message = "path is null"
            response.data = mutableListOf()
            return JsonUtil.toJson(response)
        }
        val pathArray = path as ArrayList<*>
        val dataList = mutableListOf<HashMap<String,Any>>()
        for (pathItem in pathArray){
            val pathStr = pathItem as String
            val exists = FileUtils.isFileExists(pathStr)
            dataList.add(hashMapOf("path" to pathStr,"exist" to exists))
        }
        response.code = "0"
        response.message = "success"
        response.data = dataList
        return JsonUtil.toJson(response)
    }

    @Deprecated(
        message = "This function is deprecated. Use getFileInfoAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>) instead.",
        replaceWith = ReplaceWith("getFileInfoAsync"),
        level = DeprecationLevel.WARNING
    )
    override fun getFileInfo(miniAppView: MiniAppView, params: Map<String, Any>): String? {
        logger.debug("getFileInfo")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("getFileInfo, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("getFileInfo, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val path = params["path"]
        //检测文件或文件夹是否存在
        val response = JSResponse("0", "getFileInfo", mutableMapOf<String,Any>())
        if (path == null) {
            if (logger.isDebugActivated) {
                logger.debug("JSApi sync getFileInfo path is null")
            }
            response.code = "1"
            response.message = "path is null"
            return JsonUtil.toJson(response)
        }
        val pathStr = path as String
        if (FileUtils.isUri(pathStr)){
            val uri = Uri.parse(pathStr)
            response.code = "0"
            response.message = "success"
            val name = FileUtils.getFileNameFromUri(miniAppView.context,uri)
            if (TextUtils.isEmpty(name)){
                response.code = "1"
                response.message = "fail"
                response.data = hashMapOf()
            } else {
                val isDirectory = !name!!.contains(".")//这个判断不够严谨
                response.data = hashMapOf(
                    "path" to pathStr,
                    "size" to FileUtils.getFileSizeFromUri(miniAppView.context,uri),
                    "lastModified" to FileUtils.getFileLastModifiedFromUri(miniAppView.context,uri),
                    "isDirectory" to isDirectory,
                    "name" to name)
            }
        } else {
            val file = com.blankj.utilcode.util.FileUtils.getFileByPath(pathStr)
            if (file == null){
                response.code = "1"
                response.message = "fail"
                response.data = hashMapOf()
            } else {
                response.code = "0"
                response.message = "success"
                response.data = hashMapOf(
                    "path" to file.absolutePath,
                    "size" to file.length(),
                    "lastModified" to file.lastModified(),
                    "isDirectory" to file.isDirectory(),
                    "name" to file.name)
            }
        }
        return JsonUtil.toJson(response)
    }

    override fun getFileInfoAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>) {
        logger.debug("getFileInfoAsync")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("getFileInfo, permission not granted, return")
                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)))
                return
            }
        } ?: run {
            logger.warn("getFileInfo, appInfo is null, return")
            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)))
            return
        }
        val path = params["path"]
        //检测文件或文件夹是否存在
        val response = JSResponse("0", "getFileInfo", mutableMapOf<String,Any>())
        if (path == null) {
            if (logger.isDebugActivated) {
                logger.debug("JSApi sync getFileInfo path is null")
            }
            response.code = "1"
            response.message = "path is null"
            handler.complete(JsonUtil.toJson(response))
            return
        }
        val pathStr = path as String
        scope.launch(Dispatchers.IO) {
            if (FileUtils.isUri(pathStr)){
                val uri = Uri.parse(pathStr)
                response.code = "0"
                response.message = "success"
                val name = FileUtils.getFileNameFromUri(miniAppView.context,uri)
                if (TextUtils.isEmpty(name)){
                    response.code = "1"
                    response.message = "fail"
                    response.data = hashMapOf()
                } else {
                    val isDirectory = !name!!.contains(".")//这个判断不够严谨
                    val md5 = if (FileUtils.isUriFolder(miniAppView.context,uri)){
                        ""
                    } else {
                        FileUtils.getFileMD5FromUri(miniAppView.context,uri)
                    }
                    response.data = hashMapOf(
                        "path" to pathStr,
                        "size" to FileUtils.getFileSizeFromUri(miniAppView.context,uri),
                        "lastModified" to FileUtils.getFileLastModifiedFromUri(miniAppView.context,uri),
                        "isDirectory" to isDirectory,
                        "name" to name,
                        "md5" to (md5 ?: "")
                    )
                }
            } else {
                val file = com.blankj.utilcode.util.FileUtils.getFileByPath(pathStr)
                if (file == null){
                    response.code = "1"
                    response.message = "fail"
                    response.data = hashMapOf()
                } else {
                    response.code = "0"
                    response.message = "success"
                    val md5 = if (file.isDirectory){
                        ""
                    } else {
                        com.blankj.utilcode.util.FileUtils.getFileMD5ToString(file)
                    }
                    response.data = hashMapOf(
                        "path" to file.absolutePath,
                        "size" to file.length(),
                        "lastModified" to file.lastModified(),
                        "isDirectory" to file.isDirectory(),
                        "name" to file.name,
                        "md5" to md5
                    )
                }
            }
            logger.debug("getFileInfoAsync, response: $response")
            scope.launch(Dispatchers.Main) {
                handler.complete(JsonUtil.toJson(response))
            }
        }
    }

    override fun deleteFileAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(deleteFile(miniAppView,params))
    }

    override fun deleteFile(miniAppView: MiniAppView, params: Map<String, Any>): String? {
        logger.debug("deleteFile")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("deleteFile, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("deleteFile, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val path = params["path"]
        val response = JSResponse("0", "deleteFile", listOf<String>())
        if (path == null) {
            if (logger.isDebugActivated) {
                logger.debug("JSApi sync deleteFile path is null")
            }
            response.code = "1"
            response.message = "path is null"
            response.data = emptyList()
            return JsonUtil.toJson(response)
        }
        val pathStr = path as String
        if (!isMiniAppPrivatePath(miniAppView, pathStr)) {// 有可能小程序拿到的是uri，所以用contains
            if (logger.isDebugActivated) {
                logger.debug("JSApi sync deleteFile path not in privateFolder")
            }
            response.code = "1"
            response.message = "path not in privateFolder"
            response.data = emptyList()
            return JsonUtil.toJson(response)
        }
        if (FileUtils.isUri(pathStr)){
            FileUtils.deleteFileFromUri(miniAppView.context, Uri.parse(pathStr))
        } else {
            FileUtils.deletePath(pathStr)
        }
        response.code = "0"
        response.message = "success"
        return JsonUtil.toJson(response)
    }

    override fun saveUpdateKeyValueAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(saveUpdateKeyValue(miniAppView,params))
    }

    @Deprecated(
        message = "This function is deprecated. Use saveUpdateKeyValueWithExpiry(miniAppView: MiniAppView, params: Map<String, Any>): String? instead.",
        replaceWith = ReplaceWith("saveUpdateKeyValueWithExpiry"),
        level = DeprecationLevel.WARNING
    )
    override fun saveUpdateKeyValue(miniAppView: MiniAppView, params: Map<String, Any>): String? {
        val key = params[KEY_PARAM]?.toString()
        val value = params[VALUE_PARAM]?.toString()
        if (key.isNullOrEmpty()){
            logger.warn("saveKeyValue, param key is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        if (value.isNullOrEmpty()){
            logger.warn("saveKeyValue, param value is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        miniAppView.viewModel.miniAppInfo?.let{
            SPUtils.getInstance().put(it.appId+key,value)
        }
        val response = JSResponse("0", "success", null)
        return JsonUtil.toJson(response)
    }

    override fun saveUpdateKeyValueWithExpiryAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(saveUpdateKeyValueWithExpiry(miniAppView,params))
    }

    override fun saveUpdateKeyValueWithExpiry(miniAppView: MiniAppView, params: Map<String, Any>): String? {
        val key = params[KEY_PARAM]?.toString()
        val value = params[VALUE_PARAM]?.toString()
        val ttl = params[TTL]?.toString()
        if (key.isNullOrEmpty()){
            logger.warn("saveKeyValue, param key is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        if (value.isNullOrEmpty()){
            logger.warn("saveKeyValue, param value is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        if (ttl.isNullOrEmpty()){
            logger.warn("saveKeyValue, param ttl is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }

        if (!ttl.matches(Regex("^[0-9]+$"))){
            logger.warn("saveKeyValue, param ttl is not a number, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }

        miniAppView.viewModel.miniAppInfo?.let{
            SPManager.instance.saveUpdateKeyValueWithExpiry(it.appId+key,value,ttl.toLong())
        }
        val response = JSResponse("0", "success", null)
        return JsonUtil.toJson(response)
    }

    override fun deleteKeyValueAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(deleteKeyValue(miniAppView,params))
    }

    override fun deleteKeyValue(miniAppView: MiniAppView, params: Map<String, Any>): String? {
        val key = params[KEY_PARAM]?.toString()
        if (key.isNullOrEmpty()){
            logger.warn("saveKeyValue, param key is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        miniAppView.viewModel.miniAppInfo?.let{ appInfo ->
            scope.launch(Dispatchers.IO) {
                SPManager.instance.deleteKeyValue(appInfo.appId+key)
            }
        }
        val response = JSResponse("0", "success", null)
        return JsonUtil.toJson(response)
    }

    override fun playVoiceAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(playVoice(miniAppView,params))
    }

    override fun playVoice(
        miniAppView: MiniAppView,
        params: Map<String, Any>
    ): String? {
        logger.info("playVoice")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("playVoice, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("playVoice, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val path = params["path"]
        val response = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, "")
        if (path == null) {
            logger.debug("JSApi sync playVoice path is null")
            response.code = RESPONSE_FAILED_CODE
            response.message = "path is null"
            return JsonUtil.toJson(response)
        }
        val pathStr = path as String

        if (!FileUtils.isFileExists(pathStr)){
            logger.debug("JSApi sync playVoice file not exists")
            response.code = RESPONSE_FAILED_CODE
            response.message = "file not exists"
            return JsonUtil.toJson(response)
        }
        miniAppView.viewModel.playVoice(pathStr)
        return JsonUtil.toJson(response)
    }

    override fun stopPlayVoiceAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(stopPlayVoice(miniAppView,params))
    }

    override fun stopPlayVoice(
        miniAppView: MiniAppView,
        params: Map<String, Any>
    ): String? {
        logger.info("stopPlayVoice")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                logger.warn("playVoice, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("stopPlayVoice, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        miniAppView.viewModel.stopPlayVoice()
        val response = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, "")
        return JsonUtil.toJson(response)
    }

    override fun getKeyValueAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(getKeyValue(miniAppView,params))
    }

    override fun getKeyValue(miniAppView: MiniAppView, params: Map<String, Any>): String? {
        val key = params[KEY_PARAM]?.toString()
        if (key.isNullOrEmpty()){
            logger.warn("getKeyValue, param key is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        miniAppView.viewModel.miniAppInfo?.let{
            val value = SPManager.instance.getKeyValue(it.appId+key)
            val valueMap = mutableMapOf<String, String>()
            valueMap["value"] = value
            val response = JSResponse("0", "success", valueMap)
            return JsonUtil.toJson(response)
        }
        logger.warn("getKeyValue, getMiniAppInfo is null, return")
        return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
    }

    override fun quickSearchFile(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.info("searchFile")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                val response = JSResponse("1", "permission not granted", "")
                handler.complete(JsonUtil.toJson(response))
                logger.warn("searchFile, permission not granted, return")
                return
            }
        } ?: run {
            val response = JSResponse("1", "appInfo is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.warn("searchFile, appInfo is null, return")
            return
        }
        val name = params["name"]
        if (name == null) {
            val response = JSResponse("1", "searchFile name is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.info("JSApi asyn searchFile name is null")
            return
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                if (!FileManager.instance.hasFileIndex()){// 没有扫描过，第一次扫描；后面将在InCallService中每次bind的时候更新，是在另一个进程中，不共享变量，但共享数据库。
                    FileManager.instance.updateFiles(miniAppView.context)
                }

                val list = FileManager.instance.searchFilesByName(name.toString())
                val result = mutableListOf<HashMap<String,Any>>()
                list.forEach {
                    result.add(
                        hashMapOf(
                            "name" to it.name,
                            "path" to it.path
                        )
                    )
                }
                scope.launch {
                    withContext(Dispatchers.Main) {
                        val response = JSResponse("0", "success", result)
                        handler.complete(JsonUtil.toJson(response))
                    }
                }
            }
        }
    }

    override fun quickSearchFileWithKeyWords(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                val response = JSResponse("1", "permission not granted", "")
                handler.complete(JsonUtil.toJson(response))
                logger.warn("quickSearchFileWithKeyWords, permission not granted, return")
                return
            }
        } ?: run {
            val response = JSResponse("1", "appInfo is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.warn("quickSearchFileWithKeyWords, appInfo is null, return")
            return
        }
        val keyWords = params["keywords"].toString()
        val keyWordsArray = JsonUtil.fromJson(keyWords, Array<String>::class.java)
        keyWordsArray ?: run {
            LogUtils.debug(TAG, "quickSearchFileWithKeyWords keywords is null")
            return
        }
        if (!FileManager.instance.hasFileIndex()) {// 没有扫描过，第一次扫描
            FileManager.instance.updateFiles(miniAppView.context)
        }
        val resultList = FileManager.instance.searchFilesByKeyWords(*keyWordsArray)

        scope.launch {
            withContext(Dispatchers.Main) {
                val response = JSResponse("0", "success", JsonUtil.toJson(resultList))
                handler.complete(JsonUtil.toJson(response))
            }
        }
    }

    override fun fileDownload(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                val response = JSResponse("1", "permission not granted", "")
                handler.complete(JsonUtil.toJson(response))
                logger.warn("fileDownload, permission not granted, return")
                return
            }
        } ?: run {
            val response = JSResponse("1", "appInfo is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.warn("fileDownload, appInfo is null, return")
            return
        }
        val downloadEvent = params[PARAMS_DOWNLOAD_EVENT] as? String
        val url = params[PARAMS_DOWNLOAD_URL] as? String
        val infoJson = params[PARAMS_EXTRA_INFO] as? String
        if (downloadEvent == null || url == null || infoJson == null) {
            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "empty params"))))
            return
        }
        if (!SystemUtils.isWiFiConnected(miniAppView.context)) {
            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "network not Wi-Fi"))))
        }
        when (downloadEvent) {
            PARAMS_MODEL -> {
                scope.launch {
                    val modelInfo = JsonUtil.fromJson(infoJson, ModelInfo::class.java)
                    val modelName = modelInfo?.modelName
                    val fileName = "$modelName.zip"
                    val downloadFileFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                    val downloadFilePath = "$downloadFileFolder${File.separator}$fileName"
                    val targetFileDir = "${miniAppView.context.filesDir}${File.separator}$FILE_MODEL_PATH${File.separator}$modelName${File.separator}"
                    val downloadListener = object : IDownloadListener {
                        override fun onDownloadProgress(progress: Int) {
                            LogUtils.debug(TAG, "onDownloadProgress progress: $progress")
                        }

                        override fun onDownloadSuccess() {
                            LogUtils.debug(TAG, "onDownloadSuccess")
                            kotlin.runCatching {
                                FileUtils.deletePath(targetFileDir)
                                ZipUtils.unzipFile(downloadFilePath, targetFileDir)
                            }.onFailure {
                                LogUtils.error(TAG, "onDownloadSuccess unzipFile failed: $this")
                            }
                            val zipFile = File(downloadFilePath)
                            if (zipFile.exists()) {
                                zipFile.delete()
                            }
                            val modelFilePath = "${targetFileDir}config.json"
                            modelInfo?.let {
                                val modelEntity = ModelEntity(modelId = modelInfo.modelId, modelName = modelInfo.modelName, modelPath = modelFilePath, modelVersion = modelInfo.modelVersion, modelType = modelInfo.modelType, "")
                                modelManager.insertOrUpdate(modelEntity)
                            }
                            val response = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, mapOf("result" to "true"))
                            handler.complete(JsonUtil.toJson(response))
                        }

                        override fun onDownloadFailed() {
                            LogUtils.debug(TAG, "onDownloadFailed")
                            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("result" to "false"))))
                        }
                    }
                    if (fileDownloadManager.isDownloading) {
                        handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "other task is running"))))
                    } else {
                        val downloadData = DownloadData(url, miniAppView.context.getString(R.string.download_model_title), miniAppView.context.getString(
                            R.string.download_model_description), fileName)
                        fileDownloadManager.startDownload(downloadData, downloadListener)
                    }
                }
            }
            PARAMS_FILE -> {
                val filePath = params[PARAMS_FILE_PATH] as? String
                val fileName = params[PARAMS_FILE_NAME] as? String
                if (filePath == null || fileName == null) {
                    handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "empty params"))))
                    return
                }
                val downloadListener = object : IDownloadListener {
                    override fun onDownloadProgress(progress: Int) {
                        LogUtils.debug(TAG, "onDownloadProgress progress: $progress")
                    }

                    override fun onDownloadSuccess() {
                        LogUtils.debug(TAG, "onDownloadSuccess")
                        val response = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, mapOf("result" to "true"))
                        handler.complete(JsonUtil.toJson(response))
                    }

                    override fun onDownloadFailed() {
                        LogUtils.debug(TAG, "onDownloadFailed")
                        handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("result" to "false"))))
                    }
                }
                if (fileDownloadManager.isDownloading) {
                    handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "other task is running"))))
                } else {
                    val downloadData = DownloadData(url, miniAppView.context.getString(R.string.download_file_title), miniAppView.context.getString(
                        R.string.download_file_description), "${filePath}${File.separator}${fileName}")
                    fileDownloadManager.startDownload(downloadData, downloadListener)
                }
            }
            else -> {
                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "invalid downloadEvent"))))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation(miniAppView: MiniAppView, handler: CompletionHandler<String?>){
        val locationMap = mutableMapOf<String, String>()
        val locationManager =
            miniAppView.context.getSystemService(LocationManager::class.java)
        var hasRemoveUpdates = false
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            askLocationSettings(miniAppView)
        } else {
            var locationListener: LocationListener? = null
            locationListener = LocationListener { location ->
                scope.launch {
                    withContext(Dispatchers.Main) {
                        logger.info("locationManager get location lat = ${location.latitude}, lon = ${location.longitude})")
                        locationMap["lon"] = location.longitude.toString()
                        locationMap["lat"] = location.latitude.toString()
                        locationListener?.let {
                            hasRemoveUpdates = true
                            try {
                                locationManager.removeUpdates(it)
                            } catch (e:Exception){
                                e.printStackTrace()
                            }
                        }
                        val response = JSResponse("0", "success", locationMap)
                        handler.complete(JsonUtil.toJson(response))
                    }
                }
            }
            val criteria = Criteria();
            criteria.accuracy = Criteria.ACCURACY_FINE
            criteria.isAltitudeRequired = false//不要求海拔
            criteria.isBearingRequired = false//不要求方位
            criteria.isCostAllowed = true//允许有花费
            criteria.powerRequirement = Criteria.POWER_LOW;//低功耗
            val bestProvider = locationManager.getBestProvider(criteria, false)
            logger.info("location provider = $bestProvider")

            bestProvider?.let{
                try {
                    locationManager.requestLocationUpdates(
                        bestProvider,
                        200,
                        5f,
                        locationListener
                    )
                    scope.launch {
                        delay(3000)
                        if (!hasRemoveUpdates){
                            try {
                                locationManager.removeUpdates(locationListener)
                            } catch (e:Exception){
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e.message, e)
                }
            }
        }
    }

    private fun askLocationSettings(miniAppView: MiniAppView) {
        ToastUtils.showShortToast(miniAppView.context, "正在打开位置设置")
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        miniAppView.context.startActivity(intent)
    }

    private fun isMiniAppPrivatePath(miniAppView: MiniAppView, path:String):Boolean{
        if (TextUtils.isEmpty(path)){
            return false
        }
        if (FileUtils.isUri(path)){//因为用户无法选择到inner中的文件，这种uri只可能来自用户选择的sdcard中的文件，所以只用判断outer。
            return path.contains(miniAppFilePath(miniAppView, "outer").replace("/sdcard",""))
        }
        return path.startsWith(miniAppFilePath(miniAppView, "inner")) || path.startsWith(miniAppFilePath(miniAppView,"outer"))
    }

    private fun miniAppFilePath(miniAppView: MiniAppView, type: String): String {
        return when (type) {
            "inner" -> miniAppView.viewModel.miniAppInfo?.appId?.let {
                PathManager().getMiniAppInnerSpace(miniAppView.context, it)
            } ?: ""
            "outer" -> miniAppView.viewModel.miniAppInfo?.appId?.let {
                PathManager().getMiniAppOuterSpace(it)
            } ?: ""
            else -> ""
        }
    }
}