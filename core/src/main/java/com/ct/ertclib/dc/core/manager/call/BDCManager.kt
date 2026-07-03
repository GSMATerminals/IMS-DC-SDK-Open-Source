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

package com.ct.ertclib.dc.core.manager.call

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.telecom.Call
import com.blankj.utilcode.util.Utils
import com.blankj.utilcode.util.ZipUtils
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.PathManager
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.utils.httpstack.HttpStackResponse
import com.ct.ertclib.dc.core.utils.httpstack.HttpStackHelper
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.AS_MODULE_ADLIST_INFO_EVENT
import com.ct.ertclib.dc.core.constants.CommonConstants.FLOATING_DISMISS
import com.ct.ertclib.dc.core.constants.CommonConstants.FLOATING_DISPLAY
import com.ct.ertclib.dc.core.constants.CommonConstants.PERCENT_CONSTANTS
import com.ct.ertclib.dc.core.data.bootstrap.BootstrapProperties
import com.ct.ertclib.dc.core.data.common.CallStateData
import com.ct.ertclib.dc.core.data.common.FloatingBallData
import com.ct.ertclib.dc.core.data.event.MiniAppListGetEvent
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.port.miniapp.IDownloadMiniApp
import com.ct.ertclib.dc.core.port.miniapp.IMiniAppListLoadedCallback
import com.ct.ertclib.dc.core.data.miniapp.MiniAppDownloadResult
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.model.AdItem
import com.ct.ertclib.dc.core.manager.common.LicenseManager
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.port.call.ICallInfoUpdateListener
import com.ct.ertclib.dc.core.port.common.IMessageCallback
import com.ct.ertclib.dc.core.port.dc.IDcCreateListener
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.utils.common.UsageStateUtils
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback
import com.newcalllib.datachannel.V1_0.IImsDCObserver
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import org.koin.core.component.KoinComponent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.LinkedBlockingDeque


class BDCManager(
    private var callInfo: CallInfo,
    private val miniAppManager: MiniAppManager
) : IDcCreateListener, IDownloadMiniApp, IMiniAppListLoadedCallback, ICallStateListener,
    ICallInfoUpdateListener,KoinComponent {

    companion object {
        private const val TAG = "BDCManager"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    private val mTag: String = "BDCManager[${callInfo.telecomCallId}]"
    private val mHandlerThread = HandlerThread(mTag)
    private var mHandlerThreadQuited = false

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job1 :Job ?= null
    private var job2 :Job ?= null

    init {
        mHandlerThread.start()
        mHandlerThreadQuited = false
        sLogger.info("init $mTag")
        job1 = scope.launch {
            NewCallAppSdkInterface.miniAppListEventFlow.distinctUntilChanged().collect { event ->
                sLogger.debug("collect miniAppListEventFlow event: ${event.message}")
                if (MiniAppListGetEvent.TO_GET == event.message && callInfo.telecomCallId == event.callId){
                    getMiniAppList(event.index,event.num)
                }
            }
        }
    }

    private val mDcMessageHandler = DcMessageHandler(mHandlerThread.looper)
    private val mRequestMessageQueue = LinkedBlockingDeque<RequestMessage>()

    private val CONST_DC_CREATE: Int = 0
    private val CONST_DC_STATE_CHANGE: Int = 1
    private val CONST_SEND_REQUEST: Int = 2
    private val CONST_REQUEST_RESULT: Int = 3
    private val CONST_RECEIVE_MESSAGE: Int = 4
    private var mDc: IImsDataChannel? = null
    private var mDc100: IImsDataChannel? = null
    private var mLastDcStatus : ImsDCStatus ?= null

    var adList : ArrayList<AdItem> = arrayListOf()

    //防止重复打印日志
    private var hideReason = 0

    inner class DcMessageHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("handleMessage what:${msg.what}")
            }
            when (msg.what) {
                CONST_DC_CREATE -> {
                    handleDataChannelCreated(
                        msg.data.getString("telecomCallId")!!,
                        msg.data.getString("streamId"),
                        msg.obj as IImsDataChannel
                    )
                }

                CONST_DC_STATE_CHANGE -> handleDataChannelStateChanged()
                CONST_SEND_REQUEST -> handleSendRequest()
                CONST_REQUEST_RESULT -> handleSendDataResult(msg.arg1)
                CONST_RECEIVE_MESSAGE -> handleReceiveMsg(
                    msg.data.getString("telecomCallId")!!,
                    msg.obj as ByteArray
                )

                else -> sLogger.info("handleMessage not deal with what${msg.what}")
            }
        }
    }

    // 这个定时任务只能辅助，不能依赖这个定时任务实现必要的逻辑，目前只有isInCallOnTop依赖这个定时任务
    private fun startTimerTask() {
        if (job2 != null){// 防止任务重复启动
            return
        }
        sLogger.info("${mTag} startTimerTask...")
        job2 = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            sLogger.info("${mTag} job2 started")
            while (isActive) {
                if (mDc?.state == ImsDCStatus.DC_STATE_OPEN && callInfo.state != Call.STATE_DISCONNECTED) {
                    StateFlowManager.emitCallInfoFlow(
                        CallStateData(
                            callInfo,
                            SystemClock.currentThreadTimeMillis()
                        )
                    )
                    updateMiniAppEntryHolder()
                }
                delay(300)
            }
            sLogger.info("${mTag}job2 exited")
        }
    }

    private fun handleReceiveMsg(telecomCallId: String, data: ByteArray) {
        if (mRequestMessageQueue.isEmpty()) {
            sLogger.info("$mTag handleReceiveMsg-request is null, data:${data}")
            return
        }

        val requestMessage = mRequestMessageQueue.first
        val requestData = requestMessage.data
        if (requestData == null) {
            requestMessage.data = data
        } else {
            requestMessage.data = requestData.plus(data)
        }
        requestMessage.data?.let {
            val responseResult = HttpStackHelper.verify(it)
            if (!responseResult.isComplete) {
                val progressInt = (responseResult.downloadProgress * PERCENT_CONSTANTS).toInt()
                requestMessage?.appId?.let { appId ->
                    notifyDownloadProgress(appId, progressInt)
                }
                return
            }
        }

        sendNextRequest()
        val decodeHttpResponse = HttpStackHelper.decode(requestMessage.request, requestMessage.data!!)
        if (decodeHttpResponse == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("${mTag}handleReceiveMsg decodeHttpResp is null")
            }
            if (MessageType.TYPE_GET_MINI_APP == requestMessage.messageType) {
                notifyDownloadFailed(appId = requestMessage.appId!!, null)
                return
            }
        } else if (!decodeHttpResponse.isSuccessful) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("${mTag}handleReceiveMsg decodeHttpResp code: ${decodeHttpResponse.code()}")
            }
            if (MessageType.TYPE_GET_MINI_APP == requestMessage.messageType) {
                notifyDownloadFailed(appId = requestMessage.appId!!, null)
                return
            } else if (MessageType.TYPE_GET_BOOTSTRAP_MINI_APP == requestMessage.messageType && decodeHttpResponse.code() == 304) {
                setInnerADC()
                getMiniAppList(0)
                return
            }
        } else {
            when (requestMessage.messageType) {
                MessageType.TYPE_GET_MINI_APP_LIST -> {
                    receiveMiniAppList(telecomCallId, decodeHttpResponse)
                }
                MessageType.TYPE_GET_MINI_APP -> {
                    receiveMiniApp(telecomCallId, requestMessage.appId!!, decodeHttpResponse)
                }
                MessageType.TYPE_GET_BOOTSTRAP_MINI_APP -> {
                    receiveBootstrapMiniApp(telecomCallId, decodeHttpResponse)
                    setInnerADC()
                    getMiniAppList(0)
                }
                else -> {
                    sLogger.debug("${mTag}handleReceiveMsg not deal with messageType:${requestMessage.messageType}")
                }
            }
        }
    }

    private fun notifyDownloadFailed(appId: String, errorMsg: String?) {

        val miniAppDownloadResult =
            MiniAppDownloadResult(appId = appId, isSuccessful = false, errorMessage = errorMsg)
        miniAppManager.onMiniAppDownloaded(miniAppDownloadResult, null)
    }


    private fun receiveBootstrapMiniApp(
        telecomCallId: String?,
        decodeHttpResponse: HttpStackResponse
    ) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag receiveBootstrapMiniApp telecomCallId:$telecomCallId, httpResponse:$decodeHttpResponse")
        }
        val eTag = decodeHttpResponse.header("etag")
        if (eTag.isNullOrEmpty()) {
            sLogger.debug("$mTag receiveBootstrapMiniApp - etag is null")
            return
        }

        val body = decodeHttpResponse.body()
        if (body == null) {
            sLogger.debug("$mTag receiveBootstrapMiniApp - body is null")
            return
        }

        val bytes = body.bytes()
        FileUtils.installBootstrapMiniApp(eTag, bytes)
    }

    private fun receiveMiniApp(
        telecomCallId: String?,
        appId: String,
        decodeHttpResponse: HttpStackResponse
    ) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag receiveMiniApp appId:$appId, httpResponse:$decodeHttpResponse")
        }
        val eTag = decodeHttpResponse.header("etag")
        if (eTag.isNullOrEmpty()) {
            sLogger.debug("$mTag receiveMiniApp - appVersion is null")
            notifyDownloadFailed(appId, null)
            return
        }

        val body = decodeHttpResponse.body()
        if (body == null) {
            sLogger.debug("$mTag receiveMiniApp - body is null")
            return
        }

        val bytes = body.bytes()
        val downloadAppResult = MiniAppDownloadResult(
            telecomCallId = telecomCallId, appId = appId,
            appVersion = eTag, isSuccessful = true
        )
        miniAppManager.onMiniAppDownloaded(downloadAppResult, bytes)
    }

    private fun receiveMiniAppList(
        telecomCallId: String,
        decodeHttpResponse: HttpStackResponse
    ) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag receiveMiniAppList -httpResponse:$decodeHttpResponse")
        }

        val body = decodeHttpResponse.body()
        if (body == null) {
            sLogger.debug("$mTag receiveMiniAppList body is null")
            return
        }

        val bodyString = body.string()
        if (bodyString.isEmpty()) {
            sLogger.debug("$mTag receiveMiniAppList bodyString is null")
            return
        }

        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag receiveMiniAppList bodyString:$bodyString")
        }
        val miniAppList = try {
            JsonUtil.fromJson(bodyString, MiniAppList::class.java)?.let { list ->
                requireNotNull(list.applications) { "applications is required" }
                requireNotNull(list.totalAppNum) { "totalAppNum is required" }
                requireNotNull(list.beginIndex) { "beginIndex is required" }
                requireNotNull(list.appNum) { "appNum is required" }

                list.applications?.forEach { app ->
                    requireNotNull(app.appId) { "appId is required" }
                    requireNotNull(app.appName) { "appName is required" }
                    requireNotNull(app.eTag) { "eTag is required" }
                    requireNotNull(app.phase) { "phase is required" }
                    requireNotNull(app.qosHint) { "qosHint is required" }
                }
                list
            } ?: throw IllegalArgumentException("Parse failed: result is null")
        } catch (e: IllegalArgumentException) {
            sLogger.error("Required field missing: ${e.message}")
            null
        }
        miniAppList?.let {
            miniAppList.callId = telecomCallId
            if (miniAppList.applications == null) {
                sLogger.info("$mTag receiveMiniAppList applications is null")
            } else {
                miniAppManager.onMiniAppListLoaded(miniAppList)
            }
        }
    }

    private fun handleSendDataResult(state: Int) {
        sLogger.info("${mTag}handleSendDataResult - state: $state")
        if (state == 20000) {
            return
        }
        if (!isSendFailState(state)) {
            sendNextRequest()
            return
        }

        val firstRequestMessage = mRequestMessageQueue.first
        firstRequestMessage.retryCount += 1
        if (firstRequestMessage.retryCount > 3) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag requestMessage ${firstRequestMessage.messageType} retry too many")
            }
            sendNextRequest()
            return
        }
        firstRequestMessage.status = RequestMessageStatus.RETRY
        if (mHandlerThreadQuited) {
            sLogger.info("handleSendDataResult DcMessageHandler has been quitted.")
            return
        }
        mDcMessageHandler.sendMessageDelayed(
            mDcMessageHandler.obtainMessage(CONST_SEND_REQUEST),
            100L
        )
    }

    private fun sendNextRequest() {
        sLogger.info("$mTag sendNextRequest...size:${mRequestMessageQueue.size}")
        if (mRequestMessageQueue.isEmpty()) {
            return
        }
        mRequestMessageQueue.removeFirst()
        sendRequest()
    }

    private fun isSendFailState(state: Int): Boolean {
        return 20001 == state || 20004 == state
    }

    private fun handleSendRequest() {
        sLogger.info("handleSendRequest...")
        if (mRequestMessageQueue.isEmpty()) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag handleSendRequest-request queue is empty")
            }
            return
        }

        val first = mRequestMessageQueue.first()
        if (first.status == RequestMessageStatus.SENDING) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag handlerSendRequest-previous request not completed")
            }
            return
        }
        first.status = RequestMessageStatus.SENDING
        if (sLogger.isDebugActivated) {
            sLogger.debug("handleSendRequest-send $first")
        }
        val sendData = HttpStackHelper.getRequestData(first.request!!)
        sLogger.debug("handleSendRequest sendData:${String(sendData)}")
        try {
            sLogger.info("$mTag, handleSendRequest telecomCallId:${first.dc?.telecomCallId},streamId:${first.dc?.streamId}")
            first.dc?.send(sendData, sendData.size, IDCSendDataCallBackImpl())
                ?: sLogger.debug("handleSendRequest - send data dc is null")
        } catch (e: Exception) {
            sLogger.error("send data failed", e)
        }
    }

    private fun notifyDownloadProgress(appId: String, progress: Int) {
        sLogger.info("notifyDownloadProgress, appId: $appId, progress: $progress")
        miniAppManager.onMiniAppDownloadProgressUpdated(appId, progress)
    }

    inner class ImsDcObserverImpl() :
        IImsDCObserver.Stub() {
        private var telecomCallId: String? = null
        private var streamId: String? = null

        constructor(telecomCallId: String?, streamId: String?) : this() {
            this.telecomCallId = telecomCallId
            this.streamId = streamId
        }

        override fun onDataChannelStateChange(status: ImsDCStatus?, errorCode: Int) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag ImsDcObserverImpl onDataChannelStateChange, telecomCallId:$telecomCallId, streamId:$streamId, status:$status, current status:${mDc?.state}, lastDcStatus:$mLastDcStatus")
            }
            if ("100" == streamId){
                return
            }
            if (mHandlerThreadQuited) {
                sLogger.info("onDataChannelStateChange DcMessageHandler has been quitted.")
                return
            }
            val stateChangeMsg = mDcMessageHandler.obtainMessage(CONST_DC_STATE_CHANGE)
            stateChangeMsg.obj = status
            mDcMessageHandler.sendMessageAtFrontOfQueue(stateChangeMsg)
        }

        override fun onMessage(data: ByteArray?, length: Int) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag ImsDcObserverImpl onMessage, telecomCallId:$telecomCallId, streamId:$streamId, data length:$length")
            }
            if (data == null) {
                if (sLogger.isDebugActivated) {
                    sLogger.debug("$mTag ImsDcObserverImpl onMessage data is null")
                }
            } else {
                if (mHandlerThreadQuited) {
                    sLogger.info("onMessage DcMessageHandler has been quitted.")
                    return
                }
                val receiveMessage = mDcMessageHandler.obtainMessage(CONST_RECEIVE_MESSAGE)
                val bundle = Bundle()
                bundle.putString("telecomCallId", telecomCallId)
                receiveMessage.data = bundle
                receiveMessage.obj = data
                receiveMessage.sendToTarget()
            }
        }

    }

    inner class IDCSendDataCallBackImpl : IDCSendDataCallback.Stub() {
        override fun onSendDataResult(state: Int) {
            if (mHandlerThreadQuited) {
                sLogger.info("onSendDataResult DcMessageHandler has been quitted.")
                return
            }
            val resultMessage = mDcMessageHandler.obtainMessage(CONST_REQUEST_RESULT)
            resultMessage.arg1 = state
            resultMessage.sendToTarget()
        }
    }

    enum class MessageType {
        TYPE_GET_MINI_APP_LIST,
        TYPE_GET_MINI_APP,
        TYPE_GET_BOOTSTRAP_MINI_APP
    }

    enum class RequestMessageStatus {
        IDLE,
        SENDING,
        RETRY
    }

    inner class RequestMessage {
        var dc: IImsDataChannel? = null
        var messageType: MessageType? = null
        var appId: String? = null
        var request: Request? = null
        var data: ByteArray? = null
        var retryCount: Int = 0
        var status: RequestMessageStatus = RequestMessageStatus.IDLE

        override fun toString(): String {
            return "RequestMessage(dc=$dc, messageTye=$messageType, appId=$appId, request='$request', data=${data?.contentToString()}, retryCount=$retryCount, status=$status)"
        }

    }

    private fun handleDataChannelCreated(
        telecomCallId: String, streamId: String?,
        iImsDataChannel: IImsDataChannel
    ) {
        if (sLogger.isDebugActivated) {
            sLogger.info("${mTag}-handleDataChannelCreated telecomCallId:$telecomCallId, streamId:$streamId")
        }
        if ("100" == streamId) {
            mDc100 = iImsDataChannel
            mDc100?.let {
                try {
                    it.registerObserver(ImsDcObserverImpl(telecomCallId, streamId))
                } catch (e: Exception) {
                    sLogger.error("registerObserver failed", e)
                }
            }
        } else if ("0" == streamId){
            mDc = iImsDataChannel
            mDc?.let {
                try {
                    it.registerObserver(ImsDcObserverImpl(telecomCallId, streamId))
                } catch (e: Exception) {
                    sLogger.error("registerObserver failed", e)
                }
                handleDataChannelStateChanged()
            }
        }
    }

    private fun handleDataChannelStateChanged() {
        if (sLogger.isDebugActivated) {
            sLogger.debug("${mTag}-handleDataChannelStateChanged current state:${mDc?.state}")
        }
        if (mDc?.state == mLastDcStatus) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag dc status not change")
            }
            return
        }
        mLastDcStatus = mDc?.state

        when (mDc?.state) {
            ImsDCStatus.DC_STATE_CONNECTING -> sLogger.info("$mTag handleDataChannelStateChanged dc connecting")
            ImsDCStatus.DC_STATE_OPEN -> onImsBDCOpen()
            ImsDCStatus.DC_STATE_CLOSING, ImsDCStatus.DC_STATE_CLOSED -> {
                onImsCallRemovedBDCClose()
            }
            null -> {}
        }
    }

    private fun onImsBDCOpen() {
        miniAppManager.registerMiniAppListLoadedListener(this)
        miniAppManager.setDownloadAppListener(this)
        miniAppManager.onImsBDCOpen()
        getAdList()
        if (FlavorUtils.getChannelName() == FlavorUtils.CHANNEL_LOCAL){
            getMiniAppList(0)
        } else {
            getBootstrap()
        }
    }

    private fun getAdList(){
        sLogger.info("$mTag getAdList")
        miniAppManager.dispatchASEvent(AS_MODULE_ADLIST_INFO_EVENT, mapOf(), object : IMessageCallback {
            override fun reply(message: String?) {
                message?.let {
                    val appResponse = JsonUtil.fromJson(message, AppResponse::class.java)
                    val map = appResponse?.data as? Map<*, *>
                    val adListData = map?.get(AS_MODULE_ADLIST_INFO_EVENT)
                    if (adListData != null) {
                        val adListJsonString = JsonUtil.toJson(adListData)
                        val parsedArray = JsonUtil.fromJson(adListJsonString, Array<AdItem>::class.java)
                        parsedArray?.let { array ->
                            adList = ArrayList(array.toList())
                        }
                    }
                }
            }
        })
    }

    fun onImsCallRemovedBDCClose() {
        // 可能会执行两次
        sLogger.info("$mTag onImsCallRemovedBDCClose")
        job1?.cancel()
        job2?.cancel()
        job1 = null
        job2 = null
        mDc = null
        miniAppManager.onImsBDCClose()
        miniAppManager.unregisterMiniAppListLoadedCallback()
        updateMiniAppEntryHolder()
        mRequestMessageQueue.clear()
        if (!mHandlerThreadQuited){
            mHandlerThreadQuited = mHandlerThread.quitSafely()
        }
    }

    private fun getBootstrap() {
        val builder = Request.Builder()
            .url("http://bootstrap?Terminal_Vendor=${Build.MANUFACTURER}&Terminal_Model=${Build.MODEL}")
            .method("GET", null)
        if (FileUtils.getLatestInstalledBootstrapVersion() != null) {
            builder.header("If-None-Match", FileUtils.getLatestInstalledBootstrapVersion()!!)
        }
        val request = builder.build()

        val requestMessage = RequestMessage()
        requestMessage.dc = mDc
        requestMessage.messageType = MessageType.TYPE_GET_BOOTSTRAP_MINI_APP
        requestMessage.request = request
        sLogger.info("$mTag, getBootstrap telecomCallId:${mDc?.telecomCallId}")
        addRequestMessageToSend(requestMessage)
    }

    private fun setInnerADC(){
        scope.launch(Dispatchers.IO) {
            FileUtils.getLatestInstalledBootstrapPath()?.let {
                val deferred = async {
                    val file = File("$it/properties.json")
                    if (file.exists()) {
                        val propertiesString = file.readText()
                        JsonUtil.fromJson(propertiesString, BootstrapProperties::class.java)
                    } else {
                        null
                    }
                }
                val properties = deferred.await()
                if (sLogger.isDebugActivated) sLogger.debug("setInnerADC properties:$properties")
                if (properties?.supportInnerADCDevices?.contains("${Build.MANUFACTURER}&&${Build.MODEL}") == true || properties?.supportInnerADCDevices?.contains("all") == true){
                    DCManager.instance.setBootstrapAppIdList(properties.appId)
                }
            }
        }
    }

    private fun getMiniAppList(index:Int, pageSize:Int = CommonConstants.MINI_APP_LIST_PAGE_SIZE) {
        val packageInfo = Utils.getApp().packageManager.getPackageInfo(Utils.getApp().packageName, 0)
        val versionName = packageInfo.versionName


        val request = Request.Builder()
            .url("http://applicationlist?begin-index=$index&app-num=$pageSize&sdkVersion=$versionName")
            .method("GET", null).build()

        val requestMessage = RequestMessage()
        requestMessage.dc = mDc
        requestMessage.messageType = MessageType.TYPE_GET_MINI_APP_LIST
        requestMessage.request = request
        sLogger.info("$mTag, getMiniAppList telecomCallId:${mDc?.telecomCallId}")
        addRequestMessageToSend(requestMessage)
    }

    private fun addRequestMessageToSend(requestMessage: RequestMessage) {
        sLogger.info("$mTag, addRequestMessageToSend $requestMessage")
        mRequestMessageQueue.add(requestMessage)
        sendRequest()
    }

    private fun sendRequest() {
        if (mHandlerThreadQuited) {
            sLogger.info("sendRequest DcMessageHandler has been quitted.")
            return
        }
        mDcMessageHandler.obtainMessage(CONST_SEND_REQUEST).sendToTarget()
    }


    override fun onDataChannelCreated(
        telecomCallId: String,
        streamId: String,
        imsDataChannel: IImsDataChannel
    ) {
        sLogger.info("onDataChannelCreated telecomCallId: $telecomCallId, streamId:$streamId")
        if (mHandlerThreadQuited) {
            return
        }
        val bundler = Bundle()
        bundler.putString("telecomCallId", telecomCallId)
        bundler.putString("streamId", streamId)
        if (mHandlerThreadQuited) {
            sLogger.info("onDataChannelCreated DcMessageHandler has been quitted.")
            return
        }
        val dcCreateMessage = mDcMessageHandler.obtainMessage(CONST_DC_CREATE)
        dcCreateMessage.data = bundler
        dcCreateMessage.obj = imsDataChannel
        mDcMessageHandler.sendMessageAtFrontOfQueue(dcCreateMessage)
    }

    override fun downloadMiniApp(miniAppInfo: MiniAppInfo) {
        if (mDc == null) {
            sLogger.info("$mTag downloadMiniApp bdc is null")
            return
        }
        val appId = miniAppInfo.appId

        val packageInfo = Utils.getApp().packageManager.getPackageInfo(Utils.getApp().packageName, 0)
        val versionName = packageInfo.versionName

        val builder = Request.Builder().url("http://applications?appid=$appId&sdkVersion=$versionName")
            .method("GET", null)
        val cacheAppVersion = miniAppManager.getCacheAppVersion(miniAppInfo)
        if (cacheAppVersion.isNotEmpty()) {
            builder.header("If-None-Match", cacheAppVersion)
        }

        val request = builder.build()

        val requestMessage = RequestMessage()
        if (miniAppInfo.isFromBDC100){
            requestMessage.dc = mDc100
            sLogger.info("$mTag downloadMiniApp bdc 100")
        } else {
            requestMessage.dc = mDc
            sLogger.info("$mTag downloadMiniApp bdc 0")
        }
        requestMessage.messageType = MessageType.TYPE_GET_MINI_APP
        requestMessage.appId = appId
        requestMessage.request = request
        addRequestMessageToSend(requestMessage)
    }


    override fun onMiniAppListLoaded() {
        sLogger.debug("$mTag onMiniAppListLoaded")
        startTimerTask()
        updateMiniAppEntryHolder()
    }

    override fun onCallAdded(context: Context, callInfo: CallInfo) {
        DCManager.instance.registerBDCCallback(callInfo.telecomCallId, this)
        NewCallsManager.instance.addCallInfoUpdateListener(callInfo.telecomCallId, this)
    }

    override fun onCallRemoved(context: Context, callInfo: CallInfo) {
        sLogger.debug("$mTag onCallRemoved, callInfo: $callInfo")
        if (callInfo.telecomCallId != this.callInfo.telecomCallId){ // 加一下防护
            return
        }
        onImsCallRemovedBDCClose()
    }

    override fun onCallStateChanged(callInfo: CallInfo, state: Int) {
        sLogger.debug("$mTag onCallStateChanged state: $state, callInfo: $callInfo")
        if (callInfo.telecomCallId != this.callInfo.telecomCallId){ // 加一下防护
            return
        }
        this.callInfo.state = state
        updateMiniAppEntryHolder()
        NewCallAppSdkInterface.emitCallInfoEventFlow(callInfo)
    }

    override fun onAudioDeviceChange() {
        sLogger.debug("$mTag onAudioDeviceChange")
    }

    override fun onCallInfoUpdate(callInfo: CallInfo) {
        sLogger.debug("$mTag onCallInfoUpdate callInfo: $callInfo")
        if (callInfo.telecomCallId != this.callInfo.telecomCallId){ // 加一下防护
            return
        }
        this.callInfo.slotId = callInfo.slotId
        this.callInfo.isCtCall = callInfo.isCtCall
        updateMiniAppEntryHolder()
    }

    private fun updateMiniAppEntryHolder() {
//        if (!callInfo.isCtCall) {
//            sLogger.debug("updateMiniAppEntryHolder is not ct call")
//            hideMiniAppEntryHolder()
//        } else
        if (!callInfo.isInCall() && !callInfo.isRinging()) {
            if (hideReason != 1){
                sLogger.info("$mTag updateMiniAppEntryHolder hide callInfo not in call or ringing, state:${callInfo.state}")
                hideReason = 1
            }
            hideMiniAppEntryHolder()
        } else if (SDKPermissionUtils.isFellowDialer() && !UsageStateUtils.isInCallOnTop() &&  FlavorUtils.getChannelName() != FlavorUtils.CHANNEL_DIALER) {
            if (hideReason != 2){
                sLogger.info("$mTag updateMiniAppEntryHolder hide when in call not top")
                hideReason = 2
            }
            hideMiniAppEntryHolder()
        } else if (miniAppManager.getMiniAppInfoList() == null) {
            if (hideReason != 4){
                sLogger.info("$mTag updateMiniAppEntryHolder hide miniAppList is null")
                hideReason = 4
            }
            hideMiniAppEntryHolder()
        } else {
            hideReason = 0
            miniAppManager.getMiniAppList()?.let {
                FloatingBallDataManager.update(
                    FloatingBallData(
                    FLOATING_DISPLAY,
                    callInfo,
                    adList,
                    it,
                    NewCallAppSdkInterface.floatingBallStyle.value)
                )
            }
        }
    }

    private fun hideMiniAppEntryHolder() {
        FloatingBallDataManager.update(
            FloatingBallData(
                FLOATING_DISMISS,
                callInfo,
                adList,
                null
            )
        )
    }
}