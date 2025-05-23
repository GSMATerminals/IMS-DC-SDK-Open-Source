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

package com.ct.ertclib.dc.core.usecase.miniapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.common.LicenseManager
import com.ct.ertclib.dc.core.common.NativeApp
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.utils.common.CallUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.MimeUtils
import com.ct.ertclib.dc.core.common.PathManager
import com.ct.ertclib.dc.core.utils.common.UriUtils
import com.ct.ertclib.dc.core.common.WebActivity
import com.ct.ertclib.dc.core.common.startImagePreview
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_MOVE_TO_FRONT
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REQUEST_START_ADVERSE_APP
import com.ct.ertclib.dc.core.constants.CommonConstants.COMMON_APP_EVENT
import com.ct.ertclib.dc.core.constants.MiniAppConstants
import com.ct.ertclib.dc.core.constants.MiniAppConstants.ADD_CONTACT_MODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.ADD_CONTACT_NAME_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.ADD_CONTACT_NUMBER_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.API
import com.ct.ertclib.dc.core.constants.MiniAppConstants.CONTACT_EDIT_MODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.DIGIT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.GET_CONTACT_LIST_LIMIT_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.GET_CONTACT_LIST_OFFSET_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.GET_CONTACT_NAME_NUMBER_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.HTTP_POST_WAY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.HTTP_WAY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.IS_MUTED
import com.ct.ertclib.dc.core.constants.MiniAppConstants.IS_SPEAKERPHONE_ON
import com.ct.ertclib.dc.core.constants.MiniAppConstants.LICENSE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.MUTED
import com.ct.ertclib.dc.core.constants.MiniAppConstants.SPEAKERPHONE_ON
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_HEADER
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_JSON
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_MEDIA_TYPE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.PARAMS_RESPONSE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.TITLE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.URL
import com.ct.ertclib.dc.core.data.bridge.JSResponse
import com.ct.ertclib.dc.core.data.common.MediaInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppStartParam
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.data.miniapp.MiniAppPermissions
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.picker.pickCamera
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.port.manager.IMiniToParentManager
import com.ct.ertclib.dc.core.port.usecase.mini.IAppMiniUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.PkgUtils
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.core.utils.extension.startAddContactActivity
import com.ct.ertclib.dc.core.utils.extension.startEditContactActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import wendu.dsbridge.CompletionHandler
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.collections.get

class AppMiniUseCase(
    private val miniToParentManager: IMiniToParentManager,
    private val permissionMiniUseCase: IPermissionUseCase) : IAppMiniUseCase {

    companion object {
        private const val TAG = "AppMiniUseCase"
        private const val MAX_LINK_TIME = 15L
    }

    private val logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun hangup(context: Context): String {
        //挂断电话
        miniToParentManager.getMiniAppInfo()?.let {
            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("hangup, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("hangup, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val request = AppRequest(
            CommonConstants.CALL_APP_EVENT,
            CommonConstants.ACTION_HANGUP,
            mapOf("telecomCallId" to miniToParentManager.getCallInfo()?.telecomCallId)
        )
        scope.launch {
            miniToParentManager.sendMessageToParent(request.toJson(), null)
            logger.debug("hangUp")
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun playDtmfTone(context: Context,params: Map<String, Any>): String {
        logger.debug("playDtmfTone")
        val digit = params[DIGIT]
        val license = params[LICENSE]
        if (digit == null || license == null){
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "missing digit or license")))
        }
        miniToParentManager.getMiniAppInfo()?.let {
            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("playDtmfTone, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "permission not granted")))
            }
            val verify = LicenseManager.getInstance().verifyLicense(it.appId, LicenseManager.ApiCode.PLAY_DTMF_TONE.apiCode, license.toString())
            if (!verify){
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "license verify failed")))
            }
        } ?: run {
            logger.warn("playDtmfTone, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "appInfo is null")))
        }

        val request = AppRequest(
            CommonConstants.CALL_APP_EVENT,
            CommonConstants.ACTION_PLAY_DTMF_TONE,
            mapOf("telecomCallId" to miniToParentManager.getCallInfo()?.telecomCallId,DIGIT to digit.toString().first())
        )
        scope.launch {
            miniToParentManager.sendMessageToParent(request.toJson(), null)
            logger.debug("playDtmfTone")
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }
    override fun setSpeakerphone(context: Context,params: Map<String, Any>): String {
        logger.debug("setSpeakerphone")
        val on = params[SPEAKERPHONE_ON]
        if (on == null){
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "missing on")))
        }
        miniToParentManager.getMiniAppInfo()?.let {
            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("setSpeakerphone, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "permission not granted")))
            }
        } ?: run {
            logger.warn("setSpeakerphone, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "appInfo is null")))
        }

        val request = AppRequest(
            CommonConstants.CALL_APP_EVENT,
            CommonConstants.ACTION_SET_SPEAKERPHONE,
            mapOf(SPEAKERPHONE_ON to (on as Boolean))
        )
        scope.launch {
            miniToParentManager.sendMessageToParent(request.toJson(), null)
            logger.debug("setSpeakerphone")
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun isSpeakerphoneOn(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.debug("isSpeakerphoneOn")
        miniToParentManager.getMiniAppInfo()?.let {
            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("setMuted, permission not granted, return")
                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "permission not granted"))))
                return
            }
        } ?: run {
            logger.warn("isSpeakerphoneOn, appInfo is null, return")
            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "appInfo is null"))))
            return
        }

        val request = AppRequest(
            CommonConstants.CALL_APP_EVENT,
            CommonConstants.ACTION_IS_SPEAKERPHONE_ON,
            mapOf()
        )
        scope.launch {
            miniToParentManager.sendMessageToParent(request.toJson(), object : IMessageCallback.Stub(){
                override fun reply(message: String?) {
                    try {
                        if (message != null) {
                            logger.info("isSpeakerphoneOn reply${message}")
                            val appResponse = JsonUtil.fromJson(message, AppResponse::class.java)
                            val map = appResponse?.data as? Map<*, *>
                            map?.let {
                                val response = JSResponse("0", "success", mutableMapOf(IS_SPEAKERPHONE_ON to map[IS_SPEAKERPHONE_ON]))
                                handler.complete(JsonUtil.toJson(response))
                            }
                        }
                    } catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    override fun setMuted(context: Context,params: Map<String, Any>): String {
        logger.debug("setMuted")
        val muted = params[MUTED]
        if (muted == null){
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "missing on")))
        }
        miniToParentManager.getMiniAppInfo()?.let {
            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("setMuted, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "permission not granted")))
            }
        } ?: run {
            logger.warn("setMuted, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "appInfo is null")))
        }

        val request = AppRequest(
            CommonConstants.CALL_APP_EVENT,
            CommonConstants.ACTION_SET_MUTED,
            mapOf(MUTED to (muted as Boolean))
        )
        scope.launch {
            miniToParentManager.sendMessageToParent(request.toJson(), null)
            logger.debug("setMuted")
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun isMuted(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.debug("isMuted")
        miniToParentManager.getMiniAppInfo()?.let {
            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("setMuted, permission not granted, return")
                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "permission not granted"))))
                return
            }
        } ?: run {
            logger.warn("setMuted, appInfo is null, return")
            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "appInfo is null"))))
            return
        }

        val request = AppRequest(
            CommonConstants.CALL_APP_EVENT,
            CommonConstants.ACTION_IS_MUTED,
            mapOf()
        )
        scope.launch {
            miniToParentManager.sendMessageToParent(request.toJson(), object : IMessageCallback.Stub(){
                override fun reply(message: String?) {
                    try {
                        if (message != null) {
                            logger.info("isMuted reply${message}")
                            val appResponse = JsonUtil.fromJson(message, AppResponse::class.java)
                            val map = appResponse?.data as? Map<*, *>
                            map?.let {
                                val response = JSResponse("0", "success", mutableMapOf(IS_MUTED to map[IS_MUTED]))
                                handler.complete(JsonUtil.toJson(response))
                            }
                        }
                    } catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    override fun getCallState(context: Context): String {
        logger.debug("getCallState")
        miniToParentManager.getMiniAppInfo()?.let {
            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("getCallState, permission not granted, return")
                return JsonUtil.toJson(JSResponse(MiniAppConstants.RESPONSE_FAILED_CODE, MiniAppConstants.RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("getCallState, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(MiniAppConstants.RESPONSE_FAILED_CODE, MiniAppConstants.RESPONSE_FAILED_MESSAGE, null))
        }
        val state = miniToParentManager.getCallInfo()?.state
        val callStateMap = mutableMapOf<String, Int>()
        callStateMap["callState"] = state ?: -1
        val response = JSResponse("0", "success", callStateMap)
        return JsonUtil.toJson(response)
    }

    override fun getMiniAppInfo(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.debug("getMiniAppInfo")
        val miniAppInfo = miniToParentManager.getMiniAppInfo()
        val jsResponse = JSResponse("0", "success", mutableMapOf(
            "appId" to miniAppInfo?.appId,
            "appName" to miniAppInfo?.appName,
            "appIcon" to miniAppInfo?.appIcon,
            "callId" to miniAppInfo?.callId,
            "eTag" to miniAppInfo?.eTag,
            "ifWorkWithoutPeerDc" to miniAppInfo?.ifWorkWithoutPeerDc,
            "qosHint" to miniAppInfo?.qosHint,
            "supportScene" to miniAppInfo?.supportScene,
            "isActiveStart" to miniAppInfo?.isActiveStart,
            "path" to miniAppInfo?.path,
        ))
        handler.complete(JsonUtil.toJson(jsResponse))
    }

    override fun startApp(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.debug("startApp")
        params["appType"]?.let { appType ->
            params["extra"]?.let { extra ->
                when (appType as String) {
                    MiniAppStartParam.MINIAPP_APPTYPE_MINIAPP -> {
                        val request = AppRequest(
                            CommonConstants.COMMON_APP_EVENT,
                            CommonConstants.ACTION_START_APP,
                            mapOf(
                                "telecomCallId" to miniToParentManager.getCallInfo()?.telecomCallId,
                                "appId" to extra as String
                            )
                        )
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                miniToParentManager.sendMessageToParent(request.toJson(), null)
                            }
                        }
                        val response = JSResponse("0", "success", "")
                        handler.complete(JsonUtil.toJson(response))
                    }

                    MiniAppStartParam.MINIAPP_APPTYPE_FILE -> {
                        miniToParentManager.getMiniAppInfo()?.let {
                            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(
                                    MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE))) {
                                logger.warn("startApp, MINIAPP_APPTYPE_FILE, permission not granted, return")
                                val jsResponse = JSResponse("1", "fail to open file", null)
                                handler.complete(JsonUtil.toJson(jsResponse))
                                return
                            }
                        }
                        try {
                            val path = extra as String
                            if (FileUtils.isFileExists(path)) {
                                val extension =
                                    if (path.lastIndexOf(".") != -1) {
                                        path.substring(path.lastIndexOf(".") + 1)
                                    } else {
                                        null
                                    }
                                var mimeType =
                                    if (extension != null) {
                                        MimeUtils.guessMimeTypeFromExtension(extension)
                                    } else {
                                        null
                                    }

                                if (mimeType == null) {
                                    val contentResolver = context.contentResolver
                                    mimeType = contentResolver.getType(Uri.parse(path))
                                }

                                if (mimeType == null) {
                                    mimeType = "*/*"
                                }
                                var fileUri =
                                    UriUtils.file2Uri(context, File(path))
                                fileUri?.let {
                                    if (UriUtils.isFileContentUri(it)) {
                                        val filename =
                                            "${System.currentTimeMillis()}.$extension"
                                        val cacheFile =
                                            PathManager().createCacheFile(
                                                Utils.getApp(),
                                                fileName = filename
                                            )
                                        FileUtils.copy(
                                            it.path,
                                            cacheFile!!.absolutePath
                                        )
                                        fileUri =
                                            UriUtils.file2Uri(context, cacheFile)
                                    }
                                }

                                if (extension!=null && (extension.lowercase() == "png" || extension.lowercase() == "jpg" || extension.lowercase() == "jpeg")){
                                    context.startImagePreview(path)
                                } else {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        addCategory(Intent.CATEGORY_DEFAULT)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        setDataAndType(fileUri, mimeType)
                                    }
                                    context.startActivity(intent)
                                }
                                val jsResponse = JSResponse("0", "open file success", null)
                                handler.complete(JsonUtil.toJson(jsResponse))
                            } else {
                                logger.warn("file not exist")
                            }
                        } catch (e: Exception) {
                            logger.error(e.message, e)
                            ToastUtils.showShortToast(context, "不支持打开此文件")
                            val jsResponse = JSResponse("1", "fail to open file", null)
                            handler.complete(JsonUtil.toJson(jsResponse))
                        }
                    }

                    MiniAppStartParam.MINIAPP_APPTYPE_CAMERA -> {
                        miniToParentManager.getMiniAppInfo()?.let {
                            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(
                                    MiniAppPermissions.MINIAPP_CAMERA))) {
                                logger.warn("startApp, MINIAPP_APPTYPE_CAMERA, permission not granted, return")
                                val jsResponse = JSResponse("1", "fail to open cemara", null)
                                handler.complete(JsonUtil.toJson(jsResponse))
                                return
                            }
                        }
                        val extraStr = extra as String
                        context.pickCamera(
                            extraStr == "picture",
                            miniAppFilePath(context, "outer") + "camera/",
                            object : OnPickMediaCallbackListener {
                                override fun onCancel() {

                                }

                                override fun onResult(result: List<MediaInfo>) {
                                    logger.debug("result:$result")
                                    result[0].let {
                                        val jsResponse =
                                            JSResponse("0", "success", it.absolutePath)
                                        handler.complete(JsonUtil.toJson(jsResponse))
                                    }
                                }
                            })
                    }
                    MiniAppStartParam.MINIAPP_APPTYPE_MAP -> {
                        val extraStr = extra as String // 示例："type=1&lat=23.135146&lng=113.358444&title=广东电信科技大厦"
                        val pairs = extraStr.split("&")

                        // 创建一个可变Map来存储键值对
                        val extraMap = mutableMapOf<String, String>()

                        // 遍历每个参数
                        for (pair in pairs) {
                            // 使用=分割键和值
                            val keyValue = pair.split("=")
                            if (keyValue.size == 2) {
                                val key = keyValue[0]
                                val value = keyValue[1]
                                // 将键值对添加到Map中
                                extraMap[key] = value
                            }
                        }
                        val type = extraMap["type"]
                        val lat = extraMap["lat"]
                        val lng = extraMap["lng"]
                        val title = extraMap["title"]
                        if (type != null && lat != null && lng != null && title != null){
                            val result = NativeApp.openMap(context, type,lat.toDouble(), lng.toDouble(), title)
                            if (result == 0){
                                val jsResponse =
                                    JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, "")
                                handler.complete(JsonUtil.toJson(jsResponse))
                            } else {
                                val jsResponse =
                                    JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, "")
                                handler.complete(JsonUtil.toJson(jsResponse))
                            }
                        } else {
                            val jsResponse =
                                JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, "")
                            handler.complete(JsonUtil.toJson(jsResponse))
                        }
                    }
                    MiniAppStartParam.MINIAPP_APPTYPE_BROWSER -> {
                        val url = extra as String
                        val result = NativeApp.openBrowser(context,url)
                        if (result == 0){
                            val jsResponse =
                                JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, "")
                            handler.complete(JsonUtil.toJson(jsResponse))
                        } else {
                            val jsResponse =
                                JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, "")
                            handler.complete(JsonUtil.toJson(jsResponse))
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setWindow(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.info("setWindow")
        val activity = context as? Activity
        activity?.let { activity ->
            params["hidden"]?.let {
                if (it as Boolean) {
                    logger.debug("setWindow true")
                    activity.moveTaskToBack(true)
                }
            }
            params["isFullScreen"]?.let {
                kotlin.runCatching {
                    miniToParentManager.getMiniAppInfo()?.appProperties?.windowStyle?.isFullScreen = it as Boolean
                    logger.debug("setWindowStyle isFullScreen:${it}")
                }.onFailure {
                    logger.error("set isFullScreen, param wrong")
                }
            }
            params["statusBarColor"]?.let {
                kotlin.runCatching {
                    miniToParentManager.getMiniAppInfo()?.appProperties?.windowStyle?.statusBarColor = it.toString()
                    logger.debug("setWindowStyle statusBarColor:${it}")
                }.onFailure {
                    logger.error("set statusBarColor, param wrong")
                }
            }
            params["statusBarTitleColor"]?.let {
                kotlin.runCatching {
                    miniToParentManager.getMiniAppInfo()?.appProperties?.windowStyle?.statusBarTitleColor = (it as Double).toInt()
                    logger.debug("setWindowStyle statusBarTitleColor:${it}")
                }.onFailure {
                    logger.error("set statusBarTitleColor, param wrong,${it.toString()}")
                }
            }
            params["navigationBarColor"]?.let {
                kotlin.runCatching {
                    miniToParentManager.getMiniAppInfo()?.appProperties?.windowStyle?.navigationBarColor = it.toString()
                    logger.debug("setWindowStyle navigationBarColor:${it}")
                }.onFailure {
                    logger.error("set navigationBarColor, param wrong")
                }
            }
            params["pageName"]?.let {
                kotlin.runCatching {
                    scope.launch {
                        withContext(Dispatchers.Main) {
                            miniToParentManager.miniAppInterface?.setPageName(it.toString())
                        }
                    }
                }.onFailure {
                    logger.error("set pageName, param wrong")
                }
            }
            logger.debug("setWindowStyle navigationBarColor:${miniToParentManager.getMiniAppInfo()?.appProperties?.windowStyle}")
            scope.launch {
                withContext(Dispatchers.Main) {
                    miniToParentManager.miniAppInterface?.setWindowStyle()
                }
            }

        }
        val jsResponse = JSResponse("0", "success", "")
        handler.complete(JsonUtil.toJson(jsResponse))
    }

    override fun getRemoteNumber(context: Context, handler: CompletionHandler<String?>) {
//        scope.launch(Dispatchers.Main) {
//            val builder = AlertDialog.Builder(context)
//            builder.setTitle(context.getString(R.string.request_dialog_title))
//            builder.setMessage(context.getString(R.string.request_dialog_message))
//            builder.setPositiveButton(context.getString(R.string.btn_agree), DialogInterface.OnClickListener { arg0, arg1 ->
//                scope.launch(Dispatchers.Default) {
//                    val callInfo = miniToParentManager.getCallInfo()
//                    val jsResponse = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, hashMapOf("remoteNumber" to callInfo?.remoteNumber?.filter { !it.isWhitespace() }))
//                    logger.debug("getRemoteNumber: ${callInfo?.remoteNumber}")
//                    handler.complete(JsonUtil.toJson(jsResponse))
//                }
//            })
//            builder.setNegativeButton(context.getString(R.string.btn_refuse), DialogInterface.OnClickListener { arg0, arg1 ->
//                scope.launch(Dispatchers.Default) {
//                    val jsResponse = JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, "")
//                    logger.debug("getRemoteNumber, user not granted")
//                    handler.complete(JsonUtil.toJson(jsResponse))
//                }
//            })
//            val dialog = builder.create()
//            dialog.show()
//        }
        val callInfo = miniToParentManager.getCallInfo()
        val jsResponse = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, hashMapOf("remoteNumber" to callInfo?.remoteNumber?.filter { !it.isWhitespace() }))
        logger.debug("getRemoteNumber: ${callInfo?.remoteNumber}")
        handler.complete(JsonUtil.toJson(jsResponse))
    }

    override fun requestStartAdverseApp(context: Context): String {
        val appRequestJson = AppRequest(COMMON_APP_EVENT, ACTION_REQUEST_START_ADVERSE_APP, mapOf()).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, null)
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun addOrEditContact(context: Context, params: Map<String, Any>): String {
        val mode = params[ADD_CONTACT_MODE]?.toString()
        val name = params[ADD_CONTACT_NAME_PARAM]?.toString() ?: ""
        val number = params[ADD_CONTACT_NUMBER_PARAM]?.toString() ?: ""
        logger.debug("addOrEditContact, mode: $mode, name: $name, number: $number")
        scope.launch(Dispatchers.Main) {
            when (mode) {
                CONTACT_EDIT_MODE -> {
                    context.startEditContactActivity(number)
                }
                else -> {
                    context.startAddContactActivity(name, number)
                }
            }
        }
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun getContactName(context: Context, params: Map<String, Any>): String {
        logger.info("getContactName")
        miniToParentManager.getMiniAppInfo()?.let {
            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(MiniAppPermissions.MINIAPP_READ_CONTACTS))) {
                logger.warn("getContactName, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("getContactName, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val number = params[GET_CONTACT_NAME_NUMBER_PARAM]?.toString()
        logger.debug("getContactName, number: $number")
        if (number.isNullOrEmpty()){
            logger.warn("getContactName, param number is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val name = CallUtils.getContactName(context,number)
        if (name.isNullOrEmpty()){
            logger.warn("getContactName, failed, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val nameMap = mutableMapOf<String, String>()
        nameMap["name"] = name
        val response = JSResponse("0", "success", nameMap)
        return JsonUtil.toJson(response)
    }

    override fun getContactList(context: Context, params: Map<String, Any>, handler: CompletionHandler<String?>) {
        logger.info("getContactList")
        miniToParentManager.getMiniAppInfo()?.let {
            if (!permissionMiniUseCase.isPermissionGranted(it.appId, listOf(MiniAppPermissions.MINIAPP_READ_CONTACTS))) {
                logger.warn("getContactList, permission not granted, return")
                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)))
                return
            }
        } ?: run {
            logger.warn("getContactList, appInfo is null, return")
            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)))
            return
        }
        val offset = (params[GET_CONTACT_LIST_OFFSET_PARAM] as? String)?.toInt()
        val limit = (params[GET_CONTACT_LIST_LIMIT_PARAM] as? String)?.toInt()
        logger.debug("getContactList, offset: $offset limit: $limit")
        if (offset == null || limit == null){
            logger.warn("getContactList, param offset or limit is null, return")
            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)))
            return
        }
        scope.launch(Dispatchers.IO) {
            val list = CallUtils.getContactList(context,offset,limit)
            val total = CallUtils.getContactCount(context)
            val response = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, hashMapOf("list" to list, "total" to total))
            scope.launch(Dispatchers.Main) {
                handler.complete(JsonUtil.toJson(response))
            }
        }
        return
    }


    override fun setSystemApiLicense(context: Context, params: Map<String, Any>): String {
        val license = params[LICENSE]?.toString() ?: ""
        val api = params[API]?.toString() ?: ""
        miniToParentManager.systemApiLicenseMap[api] = license
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun openWeb(context: Context, params: Map<String, Any>): String {
        val url = params[URL]?.toString() ?: ""
        val title = params[TITLE]?.toString() ?: ""
        WebActivity.startActivity(context,url, title,miniToParentManager.getCallInfo()?.telecomCallId)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getHttpResult(params: Map<String, Any>, handler: CompletionHandler<String?>) {
        scope.launch {
            val url = params[URL] as? String
            val httpWay = params[HTTP_WAY] as? String
            val paramsJson = params[PARAMS_JSON] as? String
            val mediaType = params[PARAMS_MEDIA_TYPE] as? String
            val headers = params[PARAMS_HEADER] as? String
            val decodeHeader = headers?.let {
                String(com.ct.ertclib.dc.core.utils.common.FileUtils.base64ToByteArray(it))
            }
            LogUtils.debug(TAG, "getHttpResult url : $url, httpWay: $httpWay, paramsJson: $paramsJson, mediaType: $mediaType, decodeHeader: $decodeHeader")
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(MAX_LINK_TIME, TimeUnit.SECONDS)
                .readTimeout(MAX_LINK_TIME, TimeUnit.SECONDS)
                .writeTimeout(MAX_LINK_TIME, TimeUnit.SECONDS)
                .build()
            url ?: return@launch
            if (httpWay == HTTP_POST_WAY) {
                if (mediaType == null || paramsJson == null) {
                    return@launch
                }
                val decodeJson = String(com.ct.ertclib.dc.core.utils.common.FileUtils.base64ToByteArray(paramsJson))
                LogUtils.debug(TAG, "getHttpResult, decodeJson: $decodeJson")
                val requestBody = RequestBody.create(mediaType.toMediaTypeOrNull(), decodeJson)
                val builder = Request.Builder().url(url)
                decodeHeader?.let {
                    val headerMap = JsonUtil.fromJson(decodeHeader, Map::class.java)
                    headerMap?.forEach { (entry, value) ->
                        builder.addHeader(entry.toString(), value.toString())
                    }
                }
                val request = builder.post(requestBody).build()
                scope.launch(Dispatchers.IO) {
                    kotlin.runCatching {
                        val call = okHttpClient.newCall(request)
                        call.enqueue(object: Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                val jsResponse = JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, "")
                                logger.debug("getHttpResult, failed")
                                handler.complete(JsonUtil.toJson(jsResponse))
                            }

                            override fun onResponse(call: Call, response: Response) {
                                val responseBody = response.body?.string()
                                responseBody?.let {
                                    val jsResponse = JSResponse(
                                        RESPONSE_SUCCESS_CODE,
                                        RESPONSE_SUCCESS_MESSAGE,
                                        hashMapOf(
                                            PARAMS_RESPONSE to com.ct.ertclib.dc.core.utils.common.FileUtils.byteArrayToBase64(
                                                it.toByteArray()
                                            )
                                        )
                                    )
                                    logger.debug("getHttpResult, get success response: $response responseBody: $it")
                                    handler.complete(JsonUtil.toJson(jsResponse))
                                } ?: run {
                                    val jsResponse =
                                        JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, "")
                                    logger.debug("getHttpResult, post success  response null")
                                    handler.complete(JsonUtil.toJson(jsResponse))
                                }
                            }
                        })
                    }.onFailure {
                        LogUtils.error(TAG, "getHttpResult error: ${it.message}")
                    }
                }
            } else {
                val builder = Request.Builder().url(url)
                decodeHeader?.let {
                    val headerMap = JsonUtil.fromJson(decodeHeader, Map::class.java)
                    headerMap?.forEach { (entry, value) ->
                        builder.addHeader(entry.toString(), value.toString())
                    }
                }
                val request = builder.get().build()
                val call = okHttpClient.newCall(request)
                scope.launch(Dispatchers.IO) {
                    kotlin.runCatching {
                        call.enqueue(object: Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                val jsResponse = JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, "")
                                logger.debug("getHttpResult, failed")
                                handler.complete(JsonUtil.toJson(jsResponse))
                            }


                            override fun onResponse(call: Call, response: Response) {
                                val responseBody = response.body?.string()
                                responseBody?.let {
                                    val jsResponse = JSResponse(
                                        RESPONSE_SUCCESS_CODE,
                                        RESPONSE_SUCCESS_MESSAGE,
                                        hashMapOf(
                                            PARAMS_RESPONSE to com.ct.ertclib.dc.core.utils.common.FileUtils.byteArrayToBase64(
                                                it.toByteArray()
                                            )
                                        )
                                    )
                                    logger.debug("getHttpResult, get success response: $response responseBody: $it")
                                    handler.complete(JsonUtil.toJson(jsResponse))
                                } ?: run {
                                    val jsResponse =
                                        JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, "")
                                    logger.debug("getHttpResult, get success  response null")
                                    handler.complete(JsonUtil.toJson(jsResponse))
                                }
                            }
                        })
                    }.onFailure {
                        LogUtils.error(TAG, "getHttpResult error: ${it.message}")
                    }
                }
            }
        }
    }

    override fun moveToFront(): String {
        val appRequestJson = AppRequest(COMMON_APP_EVENT, ACTION_MOVE_TO_FRONT, mapOf()).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, null)
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun stopApp(): String {
        miniToParentManager.stopApp()
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    // 获取SDK版本号等信息
    override fun getSDKInfo(
        context: Context,
        params: Map<String, Any>,
    ) :String{
        logger.debug("getSDKInfo")
        val response = JSResponse("0", "success", hashMapOf("version" to PkgUtils.getAppVersion(context)))
        return JsonUtil.toJson(response)
    }

    // 获取屏幕宽高px
    override fun getScreenInfo(
        context: Context,
        params: Map<String, Any>,
    ): String {
        logger.debug("getScreenInfo")
        val response = JSResponse("0", "success", hashMapOf("width" to ScreenUtils.getScreenWidth(context),"height" to ScreenUtils.getScreenHeight(context)))
        return JsonUtil.toJson(response)
    }

    // 获取用户点击的翼分享类型名称
    override fun getShareTypeName(
        context: Context,
        params: Map<String, Any>,
    ): String {
        logger.debug("getShareTypeName")
        val name = NewCallAppSdkInterface.getShareType()
        val response = JSResponse("0", "success", hashMapOf("shareTypeName" to name))
        return JsonUtil.toJson(response)
    }

    private fun miniAppFilePath(context: Context, type:String):String{
        when(type){
            "inner" -> miniToParentManager.let {
                return it.getMiniAppInfo()?.appId?.let { it1 ->
                    PathManager().getMiniAppInnerSpace(context, it1)
                } ?: ""
            }
            "outer" -> miniToParentManager.let {
                return it.getMiniAppInfo()?.appId?.let { it1 ->
                    PathManager().getMiniAppOuterSpace(it1)
                } ?: ""
            }
        }
        return ""
    }
}