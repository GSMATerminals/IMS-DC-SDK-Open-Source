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

package com.ct.ertclib.dc.core.miniapp.bridge

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.webkit.JavascriptInterface
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.constants.CommonConstants.DC_SEND_DATA_OK
import com.ct.ertclib.dc.core.constants.CommonConstants.MINI_APP_LIST_PAGE_SIZE
import com.ct.ertclib.dc.core.constants.MiniAppConstants
import com.ct.ertclib.dc.core.constants.MiniAppConstants.GET_CONTACT_NAME_NUMBER_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.IS_MUTED
import com.ct.ertclib.dc.core.constants.MiniAppConstants.IS_SPEAKERPHONE_ON
import com.ct.ertclib.dc.core.constants.MiniAppConstants.KEY_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.TTL
import com.ct.ertclib.dc.core.constants.MiniAppConstants.VALUE_PARAM
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.data.bridge.JSResponse
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.common.Reason
import com.ct.ertclib.dc.core.data.event.MiniAppListGetEvent
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.miniapp.MiniAppStartParam
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.manager.common.SPManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.port.miniapp.IStartAppCallback
import com.ct.ertclib.dc.core.utils.common.XmlUtils
import com.ct.ertclib.dc.core.data.miniapp.DataChannel
import com.ct.ertclib.dc.core.data.miniapp.DataChannelApp
import com.ct.ertclib.dc.core.data.miniapp.DataChannelAppInfo
import com.ct.ertclib.dc.core.manager.call.DCManager
import com.ct.ertclib.dc.core.utils.common.CallUtils
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.isWhitespace

class BootstrapJSApi(private val context: Context, private var miniAppListInfo: MiniAppList, private var callInfo: CallInfo) {

    companion object {
        private const val TAG = "BootstrapJSApi"
    }

    interface IBootstrapFinishCallback {
        fun onFinish()
    }

    private var finishCallback: IBootstrapFinishCallback? = null

    fun setFinishCallback(callback: IBootstrapFinishCallback) {
        this.finishCallback = callback
    }

    private val sLogger: Logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var loadListHandler: CompletionHandler<String?>? = null

    private var job: Job? = null


    fun init() {
        job = scope.launch {
            launch {
                NewCallAppSdkInterface.miniAppListEventFlow.distinctUntilChanged().collect { event ->
                    sLogger.info("collect miniAppListEventFlow event: ${event.message}")
                    if (MiniAppListGetEvent.ON_DOWNLOAD == event.message && event.callId == miniAppListInfo.callId) {
                        event.miniAppListInfoAll?.let {
                            miniAppListInfo = it
                        }
                        scope.launch(Dispatchers.Main){
                            val response = JSResponse("0", "success", event.miniAppListInfoNewGet)
                            scope.launch(Dispatchers.Main) {
                                loadListHandler?.complete(JsonUtil.toJson(response))
                                loadListHandler = null
                            }
                        }
                    }
                }
            }
            launch {
                NewCallAppSdkInterface.callInfoEventFlow.collect { newCallInfo ->
                    if (newCallInfo.telecomCallId == callInfo.telecomCallId){
                        callInfo = newCallInfo
                    }
                }
            }
        }
    }

    fun release(){
        job?.cancel()
        job = null
    }

    @JavascriptInterface
    fun async(msg: Any, handler: CompletionHandler<String?>) {
        try {
            sLogger.info("BootstrapJSApi async ,msg:$msg, handler:$handler")
            val jsRequest = JsonUtil.fromJson(msg.toString(), JSRequest::class.java)
            jsRequest?.let {
                when(it.function){
                    MiniAppConstants.FUNCTION_IS_MUTED -> {
                        val response = JSResponse("0", "success", mutableMapOf(IS_MUTED to NewCallsManager.instance.isMuted()))
                        handler.complete(JsonUtil.toJson(response))
                    }
                    MiniAppConstants.FUNCTION_IS_SPEAKERPHONE_ON -> {
                        val response = JSResponse("0", "success", mutableMapOf(IS_SPEAKERPHONE_ON to NewCallsManager.instance.isSpeakerphoneOn()))
                        handler.complete(JsonUtil.toJson(response))
                    }
                    MiniAppConstants.FUNCTION_HANG_UP -> {
                        NewCallsManager.instance.hangUp(callInfo.telecomCallId)
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", "")))
                    }
                    MiniAppConstants.FUNCTION_ANSWER -> {
                        NewCallsManager.instance.answer(callInfo.telecomCallId)
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", "")))
                    }
                    MiniAppConstants.FUNCTION_SET_MUTED -> {
                        NewCallsManager.instance.setMuted(jsRequest.params[MiniAppConstants.MUTED] as Boolean)
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", "")))
                    }
                    MiniAppConstants.FUNCTION_SET_SPEAKERPHONE -> {
                        NewCallsManager.instance.setSpeakerphone(jsRequest.params[MiniAppConstants.SPEAKERPHONE_ON] as Boolean)
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", "")))
                    }
                    MiniAppConstants.FUNCTION_GET_AUDIO_DEVICES -> {
                        val devices = NewCallsManager.instance.getAudioDevices()
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", devices)))
                    }
                    MiniAppConstants.FUNCTION_GET_CURRENT_AUDIO_DEVICE -> {
                        val device = NewCallsManager.instance.getCurrentAudioDevice()
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", device)))
                    }
                    MiniAppConstants.FUNCTION_GET_SYSTEM_THEME -> {
                        val currentNightMode = context.resources.configuration.uiMode and UI_MODE_NIGHT_MASK
                        val theme = if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
                        val response = JSResponse("0", "success", mutableMapOf("theme" to theme))
                        handler.complete(JsonUtil.toJson(response))
                    }
                    MiniAppConstants.FUNCTION_GET_SYSTEM_FONT_SCALE -> {
                        val fontScale = context.resources.configuration.fontScale
                        val response = JSResponse("0", "success", mutableMapOf("fontScale" to fontScale))
                        handler.complete(JsonUtil.toJson(response))
                    }
                    MiniAppConstants.FUNCTION_GET_LOCATION -> {
                        requestLocation(handler)
                    }
                    MiniAppConstants.FUNCTION_SET_AUDIO_DEVICE -> {
                        val name = jsRequest.params["name"] as? String
                        val type = jsRequest.params["type"] as? String
                        if (type != null || name != null) {
                            val result = NewCallsManager.instance.setAudioDevice(name, type)
                            handler.complete(JsonUtil.toJson(JSResponse("0", "success", result)))
                        } else {
                            handler.complete(JsonUtil.toJson(JSResponse("1", "name and type are null", "")))
                        }
                    }

                    MiniAppConstants.FUNCTION_GET_MINI_APP_LIST_INFO -> {
                        val index = (jsRequest.params["beginIndex"] as? Number)?.toInt() ?: 0
                        val num = (jsRequest.params["appNum"] as? Number)?.toInt() ?: 0
                        sLogger.info("getMiniAppListInfo index:$index, num:$num")
                        val localApps = miniAppListInfo.applications
                        val beginIndex = miniAppListInfo.beginIndex
                        sLogger.debug("getMiniAppListInfo cached beginIndex:$beginIndex, localApps size:${localApps?.size}, localApps:$localApps")
                        if (localApps != null && index >= beginIndex && index + num <= beginIndex + MINI_APP_LIST_PAGE_SIZE) {
                            sLogger.info("getMiniAppListInfo from local cache")
                            val relativeIndex = index - beginIndex
                            val endIndex = minOf(relativeIndex + num, localApps.size)
                            val subsetApps = ArrayList(localApps.subList(relativeIndex, endIndex))
                            val resultList = miniAppListInfo.copy(
                                beginIndex = index,
                                appNum = num,
                                applications = subsetApps
                            )
                            val response = JSResponse("0", "success", resultList)
                            handler.complete(JsonUtil.toJson(response))
                        } else {
                            NewCallAppSdkInterface.emitAppListEvent(
                                MiniAppListGetEvent(
                                    0,
                                    MiniAppListGetEvent.TO_GET, callInfo.telecomCallId, index, num, null, null
                                )
                            )
                            loadListHandler = handler
                        }
                    }
                    MiniAppConstants.FUNCTION_GET_RUNNING_APP_LIST -> {
                        val startingAppList = MiniAppManager.getAppPackageManager(callInfo.telecomCallId)?.getStartingAppList()
                        val runningAppList = startingAppList?.map {
                            mapOf("appid" to it.appId, "appName" to it.appName, "appIcon" to it.appIcon, "startTime" to it.startTime)
                        }
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", runningAppList)))
                    }
                    MiniAppConstants.FUNCTION_START_APP -> {
                        jsRequest.params["appType"]?.let { appType ->
                            jsRequest.params["extra"]?.let { extra ->
                                if (appType as String == MiniAppStartParam.MINIAPP_APPTYPE_MINIAPP) {
                                    MiniAppManager.getAppPackageManager( callInfo.telecomCallId)?.startMiniApp(
                                        extra.toString(),
                                        object :IStartAppCallback() {
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
                                        })
                                }
                            }
                        }
                    }
                    MiniAppConstants.FUNCTION_STOP_APP -> {
                        scope.launch(Dispatchers.Main) {
                            finishCallback?.onFinish()
                            handler.complete(JsonUtil.toJson(JSResponse("0", "success", "")))
                        }
                    }
                    MiniAppConstants.FUNCTION_GET_CALL_STATE -> {
                        val state =  callInfo.state
                        val callStateMap = mutableMapOf<String, Int>()
                        callStateMap["callState"] = state
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", callStateMap)))
                    }
                    MiniAppConstants.FUNCTION_GET_CALL_INFO -> {
                        val type = NewCallsManager.instance.getCallType(callInfo.telecomCallId)
                        val connectTime = NewCallsManager.instance.getCallConnectTime(callInfo.telecomCallId)
                        val callInfoMap = mutableMapOf<String, Any>()

                        callInfoMap["type"] = type
                        callInfoMap["connectTime"] = connectTime.toString()
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", callInfoMap)))
                    }
                    MiniAppConstants.FUNCTION_GET_REMOTE_NUMBER -> {
                        val jsResponse = JSResponse("0", "success", hashMapOf("remoteNumber" to callInfo.remoteNumber?.filter { !it.isWhitespace() }))
                        handler.complete(JsonUtil.toJson(jsResponse))
                    }
                    MiniAppConstants.FUNCTION_GET_CONTACT_NAME -> {
                        val number = jsRequest.params[GET_CONTACT_NAME_NUMBER_PARAM]?.toString()
                        sLogger.debug("getContactName, number: $number")
                        if (number.isNullOrEmpty()){
                            sLogger.warn("getContactName, param number is null, return")
                            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)))
                        } else {
                            val name = CallUtils.getContactName(context, number)
                            if (name.isNullOrEmpty()){
                                sLogger.warn("getContactName, failed, return")
                                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)))
                            } else {
                                val nameMap = mutableMapOf<String, String>()
                                nameMap["name"] = name
                                val response = JSResponse("0", "success", nameMap)
                                handler.complete(JsonUtil.toJson(response))
                            }
                        }
                    }
                    MiniAppConstants.FUNCTION_GET_CONTACT_PHOTO -> {
                        val number = jsRequest.params[GET_CONTACT_NAME_NUMBER_PARAM]?.toString()
                        sLogger.debug("getContactPhoto, number: $number")
                        if (number.isNullOrEmpty()){
                            sLogger.warn("getContactPhoto, param number is null, return")
                            handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)))
                        } else {
                            val photo = CallUtils.getContactPhoto(context, number)
                            if (photo.isNullOrEmpty()){
                                sLogger.warn("getContactPhoto, failed, return")
                                handler.complete(JsonUtil.toJson(JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)))
                            } else {
                                val photoMap = mutableMapOf<String, String>()
                                photoMap["photo"] = photo
                                val response = JSResponse("0", "success", photoMap)
                                handler.complete(JsonUtil.toJson(response))
                            }
                        }
                    }
                    MiniAppConstants.FUNCTION_GET_KEY_VALUE -> {
                        jsRequest.params["key"]?.let { key ->
                            sLogger.info("getKeyValue key:$key")
                            val value = SPManager.instance.getKeyValue(key as String)
                            val valueMap = mutableMapOf<String, String>()
                            valueMap["value"] = value
                            handler.complete(JsonUtil.toJson(JSResponse("0", "success", valueMap)))
                        }
                    }
                    MiniAppConstants.FUNCTION_SAVE_UPDATE_KEY_VALUE_WITH_EXPIRY -> {
                        val key = jsRequest.params[KEY_PARAM].toString()
                        val value = jsRequest.params[VALUE_PARAM].toString()
                        val ttl = jsRequest.params[TTL].toString()
                        SPManager.instance.saveUpdateKeyValueWithExpiry(key, value,ttl.toLong())
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", null)))
                    }
                    MiniAppConstants.FUNCTION_DELETE_KEY_VALUE -> {
                        val key = jsRequest.params[KEY_PARAM].toString()
                        scope.launch(Dispatchers.IO) {
                            SPManager.instance.deleteKeyValue(key)
                        }
                        handler.complete(JsonUtil.toJson(JSResponse("0", "success", null)))
                    }
                    MiniAppConstants.FUNCTION_CREATE_DATA_CHANNEL -> {
                        if (jsRequest.params["dcLabels"] == null || (jsRequest.params["DataChannelAppInfoXml"] == null && jsRequest.params["DataChannelAppInfoJson"] == null)) {
                            val response = JSResponse("1", "dcLabels or description is null", "")
                            handler.complete(JsonUtil.toJson(response))
                            sLogger.info("BootstrapJSApi dcLabels or description is null")
                            return
                        }
                        val dcLabels = jsRequest.params["dcLabels"] as List<String>
                        val xmlInfo = jsRequest.params["DataChannelAppInfoXml"] as? String
                        val jsonInfo = jsRequest.params["DataChannelAppInfoJson"] as? String

                        var description = ""

                        val classes = arrayOf<Class<*>>(
                            DataChannelAppInfo::class.java,
                            DataChannelApp::class.java,
                            DataChannel::class.java
                        )
                        var appId = ""

                        if (!jsonInfo.isNullOrEmpty()){
                            val dataChannelAppInfo = JsonUtil.fromJson(jsonInfo, DataChannelAppInfo::class.java)
                            appId = dataChannelAppInfo?.dataChannelApp?.appId ?: ""
                            if (dataChannelAppInfo != null) {
                                description = XmlUtils.toXml(dataChannelAppInfo, classes)
                            }
                        } else if (!xmlInfo.isNullOrEmpty()) {
                            val dataChannelAppInfo = XmlUtils.parseXml(xmlInfo, classes, DataChannelAppInfo::class.java)
                            appId = dataChannelAppInfo?.dataChannelApp?.appId ?: ""
                            description = xmlInfo
                        }

                        if (appId.isEmpty()) {
                            val response = JSResponse("1", "appId is empty", "")
                            handler.complete(JsonUtil.toJson(response))
                            sLogger.info("BootstrapJSApi async appId is empty")
                            return
                        }

                        dcLabels.forEach {
                            // 校验小程序ID
                            if (!it.contains("_" + appId + "_")) {
                                val response = JSResponse("1", "$it appId error", "")
                                handler.complete(JsonUtil.toJson(response))
                                sLogger.info("BootstrapJSApi async dcLabel$it appId error")
                                return
                            }
                            if (!DCManager.instance.isBootstrapAppId(appId)) {
                                val response = JSResponse("1", "$appId is not bootstrap appId", "")
                                handler.complete(JsonUtil.toJson(response))
                                sLogger.info("BootstrapJSApi async appId:$appId is not bootstrap appId")
                                return
                            }
                            if (MiniAppManager.getAppPackageManager(callInfo.telecomCallId)?.containsBootstrapDCLabel(it) == true) {
                                val response = JSResponse("1", "dcLabel:$it is already created", "")
                                handler.complete(JsonUtil.toJson(response))
                                sLogger.info("BootstrapJSApi dcLabel$it already created")
                                return
                            }
                        }

                        scope.launch {
                            withContext(Dispatchers.IO) {
                                // 补全streamId
                                if (!description.contains("StreamId")){
                                    description = description.replace("</DcLabel>","</DcLabel><StreamId></StreamId>")
                                }
                                val result = MiniAppManager.getAppPackageManager(callInfo.telecomCallId)
                                    ?.createApplicationDataChannelsInternal(appId, dcLabels.toTypedArray(), description)
                                sLogger.info("BootstrapJSApi async ,dcIds:$result")
                                val response = JSResponse("0", "${result?.toString()}", "")
                                handler.complete(JsonUtil.toJson(response))
                            }
                        }
                    }
                    MiniAppConstants.FUNCTION_CLOSE_DATA_CHANNEL -> {
                        val dcLabel = jsRequest.params["dcLabel"]
                        if (dcLabel == null) {
                            sLogger.info("BootstrapJSApi async closeDC dcLabel is null")
                            val response = JSResponse("1", "dcLabel is null", "")
                            handler.complete(JsonUtil.toJson(response))
                            return
                        }
                        val dcLabels = dcLabel as? ArrayList<*>
                        dcLabels?.forEach { dcLabelItem ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val dcLabelItemStr = dcLabelItem as String
                                    val dc = MiniAppManager.getAppPackageManager(callInfo.telecomCallId)?.getBootstrapDC(dcLabelItemStr)
                                    dc?.let {
                                        if (it.state != ImsDCStatus.DC_STATE_CLOSING && it.state != ImsDCStatus.DC_STATE_CLOSED) {
                                            it.unregisterObserver()
                                            it.close()
                                        }
                                    }
                                    MiniAppManager.getAppPackageManager(callInfo.telecomCallId)?.removeBootstrapDC(dcLabelItemStr)
                                    NewCallAppSdkInterface.emitBootstrapDataChannelState(
                                        dcLabelItemStr,
                                        ImsDCStatus.DC_STATE_CLOSED.ordinal
                                    )
                                }
                            }
                        }
                        val response = JSResponse("0", "success", "")
                        handler.complete(JsonUtil.toJson(response))
                    }
                    MiniAppConstants.FUNCTION_SEND_DATA -> {
                        val dcLabel = jsRequest.params["dcLabel"]
                        val data = jsRequest.params["data"]
                        if (dcLabel == null || data == null) {
                            val response = JSResponse("1", "dcLabel or data is null", "")
                            handler.complete(JsonUtil.toJson(response))
                            sLogger.info("BootstrapJSApi dcLabel or data is null")
                            return
                        }
                        val dataByteArray = FileUtils.base64ToByteArray(data as String)
                        val dcLabelStr = dcLabel as String
                        scope.launch(Dispatchers.IO){
                            sLogger.info("BootstrapJSApi,sendData dcLabel:$dcLabelStr, data:$data")
                            val dc = MiniAppManager.getAppPackageManager(callInfo.telecomCallId)?.getBootstrapDC(dcLabelStr)
                            dc?.send(dataByteArray, dataByteArray.size, object : IDCSendDataCallback.Stub() {
                                override fun onSendDataResult(state: Int) {
                                    sLogger.debug("onSendDataResult state:$state")
                                    val response = JSResponse(if(state == DC_SEND_DATA_OK) "0" else state.toString(), if(state == DC_SEND_DATA_OK) "success" else "fail", "")
                                    handler.complete(JsonUtil.toJson(response))
                                }
                            })
                        }
                    }
                    MiniAppConstants.FUNCTION_GET_BUFFER_AMOUNT -> {
                        val dcLabel = jsRequest.params["dcLabel"] as? String
                        if (dcLabel == null) {
                            sLogger.info("BootstrapJSApi getBufferedAmount dcLabel is null")
                            val response = JSResponse("1", "getBufferedAmount dcLabel is null", mutableMapOf<String, Long>())
                            handler.complete(JsonUtil.toJson(response))
                            return
                        }
                        val dc = MiniAppManager.getAppPackageManager(callInfo.telecomCallId)?.getBootstrapDC(dcLabel)
                        val bufferedAmount = dc?.let {
                            try {
                                it.bufferedAmount()
                            } catch (e: Exception) {
                                sLogger.error("getBufferedAmount dcLabel:$dcLabel failed, error:${e.message}")
                                -1L
                            }
                        } ?: run {
                            -1L
                        }
                        sLogger.info("BootstrapJSApi async ,getBufferedAmount dcLabel:$dcLabel bufferedAmount:$bufferedAmount")
                        if (bufferedAmount == -1L){
                            val response = JSResponse("1", "getBufferedAmount fail", mutableMapOf<String, Long>())
                            handler.complete(JsonUtil.toJson(response))
                            return
                        }
                        val bufferedAmountMap = mutableMapOf<String, Long>()
                        bufferedAmountMap["bufferedAmount"] = bufferedAmount
                        val response = JSResponse("0", "success", bufferedAmountMap)
                        handler.complete(JsonUtil.toJson(response))
                    }
                    else -> {
                        sLogger.error("BootstrapJSApi async ,msg:$msg, handler:$handler, function:${it.function} not support")
                    }
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun requestLocation(handler: CompletionHandler<String?>) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        if (locationManager == null) {
            handler.complete(JsonUtil.toJson(JSResponse("1", "LocationManager not available", null)))
            return
        }
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            && !locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        ) {
            com.ct.ertclib.dc.core.utils.common.ToastUtils.showShortToast(context, "正在打开位置设置")
            val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            handler.complete(JsonUtil.toJson(JSResponse("1", "location service disabled", null)))
            return
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            handler.complete(JsonUtil.toJson(JSResponse("1", "permission not granted", null)))
            return
        }
        var hasRemoveUpdates = false
        var locationListener: android.location.LocationListener? = null
        locationListener = android.location.LocationListener { location ->
            scope.launch(Dispatchers.Main) {
                val locationMap = mutableMapOf<String, String>()
                locationMap["lon"] = location.longitude.toString()
                locationMap["lat"] = location.latitude.toString()
                locationListener?.let {
                    hasRemoveUpdates = true
                    try {
                        locationManager.removeUpdates(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val response = JSResponse("0", "success", locationMap)
                handler.complete(JsonUtil.toJson(response))
            }
        }
        val criteria = android.location.Criteria()
        criteria.accuracy = android.location.Criteria.ACCURACY_FINE
        criteria.isAltitudeRequired = false
        criteria.isBearingRequired = false
        criteria.isCostAllowed = true
        criteria.powerRequirement = android.location.Criteria.POWER_LOW
        val bestProvider = locationManager.getBestProvider(criteria, false)

        if (bestProvider != null) {
            try {
                locationManager.requestLocationUpdates(bestProvider, 200, 5f, locationListener)
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    if (!hasRemoveUpdates) {
                        try {
                            locationManager.removeUpdates(locationListener)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        scope.launch(Dispatchers.Main) {
                            handler.complete(JsonUtil.toJson(JSResponse("1", "timeout", null)))
                        }
                    }
                }
            } catch (e: Exception) {
                handler.complete(JsonUtil.toJson(JSResponse("1", e.message ?: "error", null)))
            }
        } else {
            handler.complete(JsonUtil.toJson(JSResponse("1", "no provider available", null)))
        }
    }

}