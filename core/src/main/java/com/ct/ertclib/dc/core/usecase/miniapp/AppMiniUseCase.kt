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

import android.content.Intent
import android.net.Uri
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.manager.common.LicenseManager
import com.ct.ertclib.dc.core.common.NativeApp
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.utils.common.CallUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.MimeUtils
import com.ct.ertclib.dc.core.common.PathManager
import com.ct.ertclib.dc.core.utils.common.UriUtils
import com.ct.ertclib.dc.core.ui.activity.WebActivity
import com.ct.ertclib.dc.core.common.startImagePreview
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.AS_MODULE_PLATFORM_INFO_EVENT
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
import com.ct.ertclib.dc.core.data.common.ASPlatformInfo
import com.ct.ertclib.dc.core.data.common.MediaInfo
import com.ct.ertclib.dc.core.data.common.Reason
import com.ct.ertclib.dc.core.data.miniapp.MiniAppStartParam
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.data.miniapp.MiniAppPermissions
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.port.common.IMessageCallback
import com.ct.ertclib.dc.core.picker.pickCamera
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.port.usecase.mini.IAppMiniUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.utils.common.HttpUtils
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.PkgUtils
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.core.utils.extension.startAddContactActivity
import com.ct.ertclib.dc.core.utils.extension.startEditContactActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView
import com.ct.ertclib.dc.core.port.miniapp.IStartAppCallback
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.collections.get

class AppMiniUseCase(private val permissionMiniUseCase: IPermissionUseCase) : IAppMiniUseCase {

    companion object {
        private const val TAG = "AppMiniUseCase"
        private const val MAX_LINK_TIME = 15L
    }

    private val logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun hangupAsync(miniAppView: MiniAppView, handler: CompletionHandler<String?>) {
        handler.complete(hangup(miniAppView))
    }

    override fun hangup(miniAppView: MiniAppView): String {
        //挂断电话
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("hangup, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("hangup, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        scope.launch {
             miniAppView.viewModel.callInfo?.telecomCallId?.let { NewCallsManager.instance.hangUp(it) }
            logger.debug("hangUp")
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun answerAsync(miniAppView: MiniAppView, handler: CompletionHandler<String?>) {
        handler.complete(answer(miniAppView))
    }

    override fun getPlatformInfo(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        LogUtils.debug(TAG, "getPlatformInfo")
        scope.launch {
            miniAppView.viewModel.callInfo?.telecomCallId?.let {
                MiniAppManager.getAppPackageManager(it)?.dispatchASEvent(AS_MODULE_PLATFORM_INFO_EVENT, mapOf(), object : IMessageCallback {
                    override fun reply(message: String?) {
                        message?.let {
                            LogUtils.debug(TAG, "getPlatformInfo reply: $message")
                            val appResponse = JsonUtil.fromJson(message, AppResponse::class.java)
                            val map = appResponse?.data as? Map<*, *>
                            val platFormInfoString = map?.get(AS_MODULE_PLATFORM_INFO_EVENT)?.toString()
                            platFormInfoString?.let {
                                val platformInfo = JsonUtil.fromJson(platFormInfoString, ASPlatformInfo::class.java)
                                val resultMap = mutableMapOf<String, Any?>()
                                for ((key, value) in params) {
                                    when (key) {
                                        "callId" -> {
                                            resultMap[key] = platformInfo?.callIdentifier
                                        }
                                    }
                                }
                                val response = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, resultMap)
                                handler.complete(JsonUtil.toJson(response))
                            } ?: run {
                                logger.warn("getPlatformInfo, info is null")
                                val response = JSResponse(RESPONSE_FAILED_CODE, "platformInfo is null", null)
                                handler.complete(JsonUtil.toJson(response))
                            }
                        }
                    }
                })

            }
        }
    }

    override fun answer(miniAppView: MiniAppView): String {
        //接听电话
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("answer, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("answer, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        scope.launch {
             miniAppView.viewModel.callInfo?.telecomCallId?.let { NewCallsManager.instance.answer(it) }
            logger.debug("answer")
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun playDtmfToneAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(playDtmfTone(miniAppView,params))
    }

    override fun playDtmfTone(miniAppView: MiniAppView,params: Map<String, Any>): String {
        logger.debug("playDtmfTone")
        val digit = params[DIGIT]
        val license = params[LICENSE]
        if (digit == null || license == null){
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "missing digit or license")))
        }
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
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
        scope.launch {
             miniAppView.viewModel.callInfo?.telecomCallId?.let { NewCallsManager.instance.playDtmfTone(it,digit.toString().first()) }
            logger.debug("playDtmfTone")
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun setSpeakerphoneAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(setSpeakerphone(miniAppView,params))
    }

    override fun setSpeakerphone(miniAppView: MiniAppView,params: Map<String, Any>): String {
        logger.debug("setSpeakerphone")
        val on = params[SPEAKERPHONE_ON]
        if (on == null || on !is Boolean){
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "missing on")))
        }
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("setSpeakerphone, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "permission not granted")))
            }
        } ?: run {
            logger.warn("setSpeakerphone, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "appInfo is null")))
        }
        scope.launch {
            NewCallsManager.instance.setSpeakerphone(on)
            logger.debug("setSpeakerphone")
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun isSpeakerphoneOn(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.debug("isSpeakerphoneOn")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("setMuted, permission not granted, return")
                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "permission not granted"))))
                return
            }
        } ?: run {
            logger.warn("isSpeakerphoneOn, appInfo is null, return")
            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "appInfo is null"))))
            return
        }

        val response = JSResponse("0", "success", mutableMapOf(IS_SPEAKERPHONE_ON to (NewCallsManager.instance.isSpeakerphoneOn())))
        handler.complete(JsonUtil.toJson(response))
    }

    override fun setMutedAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(setMuted(miniAppView,params))
    }

    override fun setMuted(miniAppView: MiniAppView,params: Map<String, Any>): String {
        logger.debug("setMuted")
        val muted = params[MUTED]
        if (muted == null || muted !is Boolean){
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "missing on")))
        }
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("setMuted, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "permission not granted")))
            }
        } ?: run {
            logger.warn("setMuted, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "appInfo is null")))
        }
        NewCallsManager.instance.setMuted(muted)
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun isMuted(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.debug("isMuted")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("setMuted, permission not granted, return")
                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "permission not granted"))))
                return
            }
        } ?: run {
            logger.warn("setMuted, appInfo is null, return")
            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, mapOf("reason" to "appInfo is null"))))
            return
        }
        val response = JSResponse("0", "success", mutableMapOf(IS_MUTED to NewCallsManager.instance.isMuted()))
        handler.complete(JsonUtil.toJson(response))
    }

    override fun getCallStateAsync(miniAppView: MiniAppView, handler: CompletionHandler<String?>) {
        handler.complete(getCallState(miniAppView))
    }

    override fun getCallState(miniAppView: MiniAppView): String {
        logger.debug("getCallState")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_GET_CALL_STATE))) {
                logger.warn("getCallState, permission not granted, return")
                return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
            }
        } ?: run {
            logger.warn("getCallState, appInfo is null, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val state =  miniAppView.viewModel.callInfo?.state
        val type = miniAppView.viewModel.callInfo?.telecomCallId?.let { NewCallsManager.instance.getCallType(it) }
        val callStateMap = mutableMapOf<String, Int>()
        callStateMap["callState"] = state ?: -1
        callStateMap["callType"] = type ?: 0
        val response = JSResponse("0", "success", callStateMap)
        return JsonUtil.toJson(response)
    }

    override fun getMiniAppInfo(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.debug("getMiniAppInfo")
        val miniAppInfo = miniAppView.viewModel.miniAppInfo
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
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.debug("startApp $params")
        params["appType"]?.let { appType ->
            params["extra"]?.let { extra ->
                when (appType as String) {
                    MiniAppStartParam.MINIAPP_APPTYPE_MINIAPP -> {
                        MiniAppManager.getAppPackageManager( miniAppView.viewModel.callInfo?.telecomCallId)
                            ?.startMiniApp(extra.toString(), object :IStartAppCallback() {
                                override fun onStartResult(
                                    appId: String,
                                    isSuccess: Boolean,
                                    reason: Reason?
                                ) {
                                    val response = JSResponse("0", "success", mutableMapOf(MiniAppConstants.IS_STARTED to isSuccess))
                                    scope.launch(Dispatchers.Main) {
                                        handler.complete(JsonUtil.toJson(response))
                                    }
                                }

                                override fun onDownloadProgressUpdated(appId: String, progress: Int) {

                                }
                            },isStartByOthers = true)
                    }

                    MiniAppStartParam.MINIAPP_APPTYPE_FILE -> {
                        miniAppView.viewModel.miniAppInfo?.let {
                            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(
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
                                    val contentResolver = miniAppView.context.contentResolver
                                    mimeType = contentResolver.getType(Uri.parse(path))
                                }

                                if (mimeType == null) {
                                    mimeType = "*/*"
                                }
                                var fileUri =
                                    UriUtils.file2Uri(miniAppView.context, File(path))
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
                                            UriUtils.file2Uri(miniAppView.context, cacheFile)
                                    }
                                }

                                if (extension!=null && (extension.lowercase() == "png" || extension.lowercase() == "jpg" || extension.lowercase() == "jpeg")){
                                    miniAppView.context.startImagePreview(path)
                                } else {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        addCategory(Intent.CATEGORY_DEFAULT)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        setDataAndType(fileUri, mimeType)
                                    }
                                    miniAppView.context.startActivity(intent)
                                }
                                val jsResponse = JSResponse("0", "open file success", null)
                                handler.complete(JsonUtil.toJson(jsResponse))
                            } else {
                                logger.warn("file not exist")
                            }
                        } catch (e: Exception) {
                            logger.error(e.message, e)
                            ToastUtils.showShortToast(miniAppView.context, "不支持打开此文件")
                            val jsResponse = JSResponse("1", "fail to open file", null)
                            scope.launch(Dispatchers.Main) {
                                handler.complete(JsonUtil.toJson(jsResponse))
                            }
                        }
                    }

                    MiniAppStartParam.MINIAPP_APPTYPE_CAMERA -> {
                        miniAppView.viewModel.miniAppInfo?.let {
                            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(
                                    MiniAppPermissions.MINIAPP_CAMERA))) {
                                logger.warn("startApp, MINIAPP_APPTYPE_CAMERA, permission not granted, return")
                                val jsResponse = JSResponse("1", "fail to open cemara", null)
                                handler.complete(JsonUtil.toJson(jsResponse))
                                return
                            }
                        }
                        val extraStr = extra as String
                        miniAppView.context.pickCamera(
                            extraStr == "picture",
                            miniAppFilePath(miniAppView, "outer") + "camera/",
                            object : OnPickMediaCallbackListener {
                                override fun onCancel() {

                                }

                                override fun onResult(result: List<MediaInfo>) {
                                    logger.debug("result:$result")
                                    result[0].let {
                                        val jsResponse =
                                            JSResponse("0", "success", it.absolutePath)
                                        scope.launch(Dispatchers.Main) {
                                            handler.complete(JsonUtil.toJson(jsResponse))
                                        }
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
                            val result = NativeApp.openMap(miniAppView.context, type,lat.toDouble(), lng.toDouble(), title)
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
                        val result = NativeApp.openBrowser(miniAppView.context,url)
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
                    MiniAppStartParam.MINIAPP_APPTYPE_MINIAPP_WITH_PARAMS -> {
                        val extraStr = extra as String // 示例："appId=xxx&params=xxx"
                        val pairs = extraStr.split("&",limit = 2)

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

                        MiniAppManager.getAppPackageManager( miniAppView.viewModel.callInfo?.telecomCallId)
                            ?.startMiniApp(extraMap["appId"].toString(), object :IStartAppCallback() {
                                override fun onStartResult(
                                    appId: String,
                                    isSuccess: Boolean,
                                    reason: Reason?
                                ) {
                                    val response = JSResponse("0", "success", mutableMapOf(MiniAppConstants.IS_STARTED to isSuccess))
                                    scope.launch(Dispatchers.Main) {
                                        handler.complete(JsonUtil.toJson(response))
                                    }
                                }

                                override fun onDownloadProgressUpdated(appId: String, progress: Int) {

                                }
                            },isStartByOthers = true, startByOthersParams = extraMap["params"])
                    }
                    else -> {}
                }
            }
        }
    }

    override fun setWindow(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.info("setWindow params:${params}")
        miniAppView.let {
            params["hidden"]?.let { hidden ->
                when (hidden) {
                    is Boolean -> {
                        logger.debug("setWindow hidden $hidden")
                        scope.launch(Dispatchers.Main) {
                            if (hidden) {
                                miniAppView.minimize()
                            } else {
                                miniAppView.show()
                            }
                        }
                    }
                    else -> logger.error("setWindow hidden param wrong")
                }
            }
            params["isFullScreen"]?.let { value ->
                when (value) {
                    is Boolean -> {
                        miniAppView.viewModel.miniAppInfo?.appProperties?.windowStyle?.isFullScreen = value
                        logger.debug("setWindow isFullScreen: $value")
                    }

                    else -> logger.error("setWindow isFullScreen param wrong: expected Boolean, got ${value::class.simpleName}")
                }
            }
            params["statusBarColor"]?.let { colorValue ->
                kotlin.runCatching {
                    val colorString = colorValue.toString()

                    // 使用正则表达式进行校验
                    if (Pattern.matches("^#[0-9a-fA-F]{6}$", colorString) || Pattern.matches("^#[0-9a-fA-F]{8}$", colorString)) {
                        // 格式正确，执行赋值操作
                        miniAppView.viewModel.miniAppInfo?.appProperties?.windowStyle?.statusBarColor = colorString
                        logger.debug("setWindowStyle statusBarColor:$colorString")
                    } else {
                        // 格式不正确，抛出异常或记录错误
                        val errorMsg = "Invalid statusBarColor format: $colorString. Must be #RRGGBB."
                        logger.error(errorMsg)
                    }

                }.onFailure { e ->
                    logger.error("set statusBarColor failed: ${e.message}", e)
                }
            }
            params["statusBarTitleColor"]?.let { value ->
                val color = when (value) {
                    is Int -> value
                    is Double -> value.toInt()
                    is Float -> value.toInt()
                    is String -> value.toIntOrNull()
                    else -> null
                }

                if (color != null) {
                    miniAppView.viewModel.miniAppInfo?.appProperties?.windowStyle?.statusBarTitleColor = color
                    logger.debug("setWindowStyle statusBarTitleColor: $color")
                } else {
                    logger.error("set statusBarTitleColor failed: invalid param type or value, got: $value (${value::class.simpleName})")
                }
            }
            params["navigationBarColor"]?.let {colorValue ->
                kotlin.runCatching {
                    val colorString = colorValue.toString()
                    // 使用正则表达式进行校验
                    if (Pattern.matches("^#[0-9a-fA-F]{6}$", colorString) || Pattern.matches("^#[0-9a-fA-F]{8}$", colorString)) {
                        // 格式正确，执行赋值操作
                        miniAppView.viewModel.miniAppInfo?.appProperties?.windowStyle?.navigationBarColor = colorString
                        logger.debug("setWindowStyle navigationBarColor:${colorString}")
                    } else {
                        // 格式不正确，抛出异常或记录错误
                        val errorMsg = "Invalid navigationBarColor format: $colorString. Must be #RRGGBB."
                        logger.error(errorMsg)
                    }

                }.onFailure {
                    logger.error("set navigationBarColor, param wrong")
                }
            }
            params["pageName"]?.let {
                kotlin.runCatching {
                    scope.launch {
                        withContext(Dispatchers.Main) {
                            miniAppView.viewModel.notifyPageNameChanged(it.toString())
                        }
                    }
                }.onFailure {
                    logger.error("set pageName, param wrong")
                }
            }
            logger.debug("setWindowStyle navigationBarColor:${miniAppView.viewModel.miniAppInfo?.appProperties?.windowStyle}")
            scope.launch {
                withContext(Dispatchers.Main) {
                    miniAppView.viewModel.setWindowStyle()
                }
            }

        }
        val jsResponse = JSResponse("0", "success", "")
        handler.complete(JsonUtil.toJson(jsResponse))
    }

    override fun getRemoteNumber(miniAppView: MiniAppView, handler: CompletionHandler<String?>) {
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
        val callInfo =  miniAppView.viewModel.callInfo
        val jsResponse = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, hashMapOf("remoteNumber" to callInfo?.remoteNumber?.filter { !it.isWhitespace() }))
        logger.debug("getRemoteNumber: ${callInfo?.remoteNumber}")
        handler.complete(JsonUtil.toJson(jsResponse))
    }

    override fun requestStartAdverseAppAsync(
        miniAppView: MiniAppView,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(requestStartAdverseApp(miniAppView))
    }

    override fun requestStartAdverseApp(miniAppView: MiniAppView): String {
        miniAppView.viewModel.miniAppInfo?.appId?.let { MiniAppManager.getAppPackageManager( miniAppView.viewModel.callInfo?.telecomCallId)?.requestStartAdverseApp(it) }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun addOrEditContactAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(addOrEditContact(miniAppView, params))
    }

    override fun addOrEditContact(miniAppView: MiniAppView, params: Map<String, Any>): String {
        val mode = params[ADD_CONTACT_MODE]?.toString()
        val name = params[ADD_CONTACT_NAME_PARAM]?.toString() ?: ""
        val number = params[ADD_CONTACT_NUMBER_PARAM]?.toString() ?: ""
        logger.debug("addOrEditContact, mode: $mode, name: $name, number: $number")
        scope.launch(Dispatchers.Main) {
            when (mode) {
                CONTACT_EDIT_MODE -> {
                    miniAppView.context.startEditContactActivity(number)
                }
                else -> {
                    miniAppView.context.startAddContactActivity(name, number)
                }
            }
        }
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun getContactNameAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(getContactName(miniAppView, params))
    }

    override fun getContactName(miniAppView: MiniAppView, params: Map<String, Any>): String {
        logger.info("getContactName")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_READ_CONTACTS))) {
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
        val name = CallUtils.getContactName(miniAppView.context,number)
        if (name.isNullOrEmpty()){
            logger.warn("getContactName, failed, return")
            return JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null))
        }
        val nameMap = mutableMapOf<String, String>()
        nameMap["name"] = name
        val response = JSResponse("0", "success", nameMap)
        return JsonUtil.toJson(response)
    }

    override fun getContactList(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>) {
        logger.info("getContactList")
        miniAppView.viewModel.miniAppInfo?.let {
            if (!permissionMiniUseCase.checkPermissionAndRecord(it.appId, listOf(MiniAppPermissions.MINIAPP_READ_CONTACTS))) {
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
            val list = CallUtils.getContactList(miniAppView.context,offset,limit)
            val total = CallUtils.getContactCount(miniAppView.context)
            val response = JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, hashMapOf("list" to list, "total" to total))
            scope.launch(Dispatchers.Main) {
                handler.complete(JsonUtil.toJson(response))
            }
        }
        return
    }

    override fun setSystemApiLicenseAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(setSystemApiLicense(miniAppView, params))
    }

    override fun setSystemApiLicense(miniAppView: MiniAppView, params: Map<String, Any>): String {
        val license = params[LICENSE]?.toString() ?: ""
        val api = params[API]?.toString() ?: ""
        miniAppView.viewModel.systemApiLicenseMap[api] = license
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun openWebAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(openWeb(miniAppView, params))
    }

    override fun openWeb(miniAppView: MiniAppView, params: Map<String, Any>): String {
        val url = params[URL]?.toString() ?: ""
        val title = params[TITLE]?.toString() ?: ""
        WebActivity.startActivity(miniAppView.context,url, title, miniAppView.viewModel.callInfo?.telecomCallId)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }
    
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

                val headMap = mutableMapOf<String, String>()
                decodeHeader?.let {
                    val headerMap = JsonUtil.fromJson(decodeHeader, Map::class.java)
                    headerMap?.forEach { (entry, value) ->
                        headMap[entry.toString()] = value.toString()
                    }
                }
                HttpUtils.sendPostRequest(url, headMap, decodeJson, mediaType) { result ->
                    if (result == HttpUtils.REQUEST_FAILED) {
                        val jsResponse = JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, "")
                        logger.debug("getHttpResult, failed")
                        handler.complete(JsonUtil.toJson(jsResponse))
                    } else {
                        val jsResponse = JSResponse(
                            RESPONSE_SUCCESS_CODE,
                            RESPONSE_SUCCESS_MESSAGE,
                            hashMapOf(
                                PARAMS_RESPONSE to com.ct.ertclib.dc.core.utils.common.FileUtils.byteArrayToBase64(
                                    result.toByteArray()
                                )
                            )
                        )
                        logger.debug("getHttpResult, get success result: $result")
                        handler.complete(JsonUtil.toJson(jsResponse))
                    }
                }
            } else {
                val headMap = mutableMapOf<String, String>()
                decodeHeader?.let {
                    val headerMap = JsonUtil.fromJson(decodeHeader, Map::class.java)
                    headerMap?.forEach { (entry, value) ->
                        headMap[entry.toString()] = value.toString()
                    }
                }
                HttpUtils.sendGetRequest(url, headMap) { result ->
                    if (result == HttpUtils.REQUEST_FAILED) {
                        val jsResponse = JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, "")
                        logger.debug("getHttpResult, failed")
                        handler.complete(JsonUtil.toJson(jsResponse))
                    } else {
                        val jsResponse = JSResponse(
                            RESPONSE_SUCCESS_CODE,
                            RESPONSE_SUCCESS_MESSAGE,
                            hashMapOf(
                                PARAMS_RESPONSE to com.ct.ertclib.dc.core.utils.common.FileUtils.byteArrayToBase64(
                                    result.toByteArray()
                                )
                            )
                        )
                        logger.debug("getHttpResult, get success result: $result")
                        handler.complete(JsonUtil.toJson(jsResponse))
                    }
                }
            }
        }
    }

    override fun moveToFrontAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>) {
        handler.complete(moveToFront(miniAppView))
    }

    override fun moveToFront(miniAppView: MiniAppView): String {
        miniAppView.show()
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun stopAppAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>) {
        handler.complete(stopApp(miniAppView))
    }

    override fun stopApp(miniAppView: MiniAppView): String {
        miniAppView.finishAndKillMiniApp()
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun getSDKInfoAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(getSDKInfo(miniAppView, params))
    }

    // 获取SDK版本号等信息
    override fun getSDKInfo(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
    ) :String{
        logger.debug("getSDKInfo")
        val response = JSResponse("0", "success", hashMapOf("version" to PkgUtils.getAppVersion(miniAppView.context)))
        return JsonUtil.toJson(response)
    }

    override fun getScreenInfoAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(getScreenInfo(miniAppView, params))
    }
    // 获取屏幕宽高px
    override fun getScreenInfo(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
    ): String {
        logger.debug("getScreenInfo")
        val response = JSResponse("0", "success", hashMapOf("width" to ScreenUtils.getScreenWidth(miniAppView.context),"height" to ScreenUtils.getScreenHeight(miniAppView.context)))
        return JsonUtil.toJson(response)
    }

    override fun getShareTypeNameAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(getShareTypeName(miniAppView, params))
    }

    // 获取用户点击的翼分享类型名称
    override fun getShareTypeName(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
    ): String {
        logger.debug("getShareTypeName")
        val name = NewCallAppSdkInterface.getShareType()
        val response = JSResponse("0", "success", hashMapOf("shareTypeName" to name))
        return JsonUtil.toJson(response)
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