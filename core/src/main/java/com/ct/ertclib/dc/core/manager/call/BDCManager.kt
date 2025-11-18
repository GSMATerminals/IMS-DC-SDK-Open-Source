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

package com.ct.ertclib.dc.core.manager.call

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.telecom.Call
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.utils.httpstack.HttpStackResponse
import com.ct.ertclib.dc.core.utils.httpstack.HttpStackHelper
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.FLOATING_DISMISS
import com.ct.ertclib.dc.core.constants.CommonConstants.FLOATING_DISPLAY
import com.ct.ertclib.dc.core.constants.CommonConstants.PERCENT_CONSTANTS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.STYLE_DEFAULT
import com.ct.ertclib.dc.core.data.common.CallStateData
import com.ct.ertclib.dc.core.data.common.FloatingBallData
import com.ct.ertclib.dc.core.data.event.MiniAppListGetEvent
import com.ct.ertclib.dc.core.port.miniapp.IDownloadMiniApp
import com.ct.ertclib.dc.core.port.miniapp.IMiniAppListLoadedCallback
import com.ct.ertclib.dc.core.data.miniapp.MiniAppDownloadResult
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.port.call.ICallInfoUpdateListener
import com.ct.ertclib.dc.core.port.dc.IDcCreateListener
import com.ct.ertclib.dc.core.utils.common.UsageStateUtils
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback
import com.newcalllib.datachannel.V1_0.IImsDCObserver
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import org.koin.core.component.KoinComponent
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
    private var mAskToUnlock = false

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job1 :Job ?= null
    private var job2 :Job ?= null

    init {
        mAskToUnlock = false
        mHandlerThread.start()
        mHandlerThreadQuited = false
        sLogger.info("init $mTag")
        job1 = scope.launch {
            NewCallAppSdkInterface.miniAppListEventFlow.distinctUntilChanged().collect { event ->
                sLogger.debug("collect miniAppListEventFlow event: ${event.message}")
                if (MiniAppListGetEvent.TO_REFRESH == event.message && callInfo.telecomCallId == event.miniAppListInfo?.callId){
                    getMiniAppList(0)
                } else if (MiniAppListGetEvent.TO_LOADMORE == event.message && callInfo.telecomCallId == event.miniAppListInfo?.callId){
                    getMiniAppList(event.miniAppListInfo.beginIndex+CommonConstants.MINI_APP_LIST_PAGE_SIZE)
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

    private fun checkTopTask() {
        if (job2 != null){// 防止任务重复启动
            return
        }
        sLogger.info("${mTag}checkTopActivityTask...")
        job2 = scope.launch {
            while (isActive && mDc?.state == ImsDCStatus.DC_STATE_OPEN && callInfo.state!=Call.STATE_DISCONNECTED) {
                // 利用定时任务顺便刷新一下通话信息
                StateFlowManager.emitCallInfoFlow(
                    CallStateData(
                        callInfo,
                        SystemClock.currentThreadTimeMillis()
                    )
                )
                updateMiniAppEntryHolder()
                delay(300)
            }
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
            if (MessageType.TYPE_GET_MINI_APP != requestMessage.messageType) {
                return
            }
            notifyDownloadFailed(appId = requestMessage.appId!!, null)
        } else if (!decodeHttpResponse.isSuccessful) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("${mTag}handleReceiveMsg decodeHttpResp code: ${decodeHttpResponse.code()}")
            }
            if (MessageType.TYPE_GET_MINI_APP != requestMessage.messageType) {
                return
            }
            notifyDownloadFailed(appId = requestMessage.appId!!, null)
        } else {
            if (MessageType.TYPE_GET_MINI_APP_LIST == requestMessage.messageType) {
                receiveMiniAppList(telecomCallId, decodeHttpResponse)
            } else {
                receiveMiniApp(telecomCallId, requestMessage.appId!!, decodeHttpResponse)
            }
        }
    }

    private fun notifyDownloadFailed(appId: String, errorMsg: String?) {

        val miniAppDownloadResult =
            MiniAppDownloadResult(appId = appId, isSuccessful = false, errorMessage = errorMsg)
        miniAppManager.onMiniAppDownloaded(miniAppDownloadResult, null)
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
        try {
            val bodyString = body.string() //待调试，是否是json格式String
            if (bodyString.isNullOrEmpty()) {
                sLogger.debug("$mTag receiveMiniAppList bodyString is null")
                return
            }

            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag receiveMiniAppList bodyString:$bodyString")
            }
            val miniAppList = JsonUtil.fromJson(bodyString, MiniAppList::class.java)
            miniAppList?.let {
                miniAppList.callId = telecomCallId
                if (miniAppList.applications == null) {
                    sLogger.info("$mTag receiveMiniAppList applications is null")
                } else {
                    miniAppManager.onMiniAppListLoaded(miniAppList)
                    checkTopTask()
                }
            }
        } catch (e: Exception) {
            if (sLogger.isDebugActivated) {
                sLogger.error("$mTag receiveMiniAppList error", e)
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
        firstRequestMessage.retryCount = firstRequestMessage.retryCount + 1
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
        TYPE_GET_MINI_APP
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
        getMiniAppList(0)
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
            mHandlerThreadQuited = mHandlerThread.quit()
        }
    }

    private fun getMiniAppList(index:Int) {
        val packageInfo = Utils.getApp().packageManager.getPackageInfo(Utils.getApp().packageName, 0)
        val versionName = packageInfo.versionName


        val request = Request.Builder()
            .url("http:/applicationlist?begin-index=$index&app-num=${CommonConstants.MINI_APP_LIST_PAGE_SIZE}&sdkVersion=$versionName")
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

        val builer = Request.Builder().url("http:/applications?appid=$appId&sdkVersion=$versionName")
            .method("GET", null)
        val cacheAppVersion = miniAppManager.getCacheAppVersion(miniAppInfo)
        if (!cacheAppVersion.isNullOrEmpty()) {
            builer.header("If-None-Match", cacheAppVersion)
        }

        val request = builer.build()

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
        checkTopTask()
        updateMiniAppEntryHolder()
    }

    override fun onCallAdded(context: Context, callInfo: CallInfo) {
    }

    override fun onCallRemoved(context: Context, callInfo: CallInfo) {
        sLogger.debug("$mTag NewCallGuideManager onCallRemoved, callInfo: $callInfo")
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
        } else if (!SDKPermissionUtils.isFellowDialer() && UsageStateUtils.isMiniAppExpandedActivityShow() && FlavorUtils.getChannelName() != FlavorUtils.CHANNEL_DIALER) {
            if (hideReason != 3){
                sLogger.info("$mTag updateMiniAppEntryHolder hide when expended list")
                hideReason = 3
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
                if (FlavorUtils.getChannelName() == FlavorUtils.CHANNEL_DIALER){
                    StateFlowManager.emitDialerEntryDataFlow(
                        FloatingBallData(
                            FLOATING_DISPLAY,
                            callInfo,
                            it,
                            NewCallAppSdkInterface.floatingBallStyle.value ?: STYLE_DEFAULT
                        )
                    )
                } else {
                    StateFlowManager.emitFloatingBallDataFlow(
                        FloatingBallData(
                            FLOATING_DISPLAY,
                            callInfo,
                            it,
                            NewCallAppSdkInterface.floatingBallStyle.value ?: STYLE_DEFAULT
                        )
                    )
                }
            }
        }
    }

    private fun hideMiniAppEntryHolder() {
        if (FlavorUtils.getChannelName() == FlavorUtils.CHANNEL_DIALER){
            StateFlowManager.emitDialerEntryDataFlow(
                FloatingBallData(
                    FLOATING_DISMISS,
                    callInfo,
                    null
                )
            )
        } else {
            StateFlowManager.emitFloatingBallDataFlow(
                FloatingBallData(
                    FLOATING_DISMISS,
                    callInfo,
                    null
                )
            )
        }
    }


}