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

package com.ct.ertclib.dc.core.manager.context

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.DCUtils
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_EC_CALLBACK
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REFRESH_MINI_PERMISSION
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_START_APP_RESPONSE
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_RESPONSE_CODE_SUCCESS
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS
import com.ct.ertclib.dc.core.constants.ContextConstants.INTENT_APP_SERVICE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_DRAWING_INO_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_EC_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_NOTIFY_DATA_CHANNEL
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_NOTIFY_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SCREEN_SIZE_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SCREEN_SHARE_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SKETCH_STATUS_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_START_ADVERSE_APP_RESPONSE_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_VIDEO_WINDOW_NOTIFY
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.event.NotifyEvent
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.miniapp.aidl.IDCCallback
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.miniapp.aidl.IMiniToParent
import com.ct.ertclib.dc.core.miniapp.aidl.IParentToMini
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.port.manager.IMiniToParentManager
import com.ct.ertclib.dc.core.port.miniapp.IMiniApp
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.newcalllib.datachannel.V1_0.IImsDCObserver
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
import java.util.Collections

class MiniToParentManager : IMiniToParentManager {

    companion object {
        private const val TAG = "MiniToParentManager"
    }

    override var miniAppInterface: IMiniApp? = null
    override val createDCLabelList: MutableList<String> = Collections.synchronizedList(ArrayList())
    override val openDCList: MutableList<IImsDataChannel> = Collections.synchronizedList(ArrayList())//可能会重复
    override val systemApiLicenseMap = mutableMapOf<String, String>()

    private val logger = Logger.getLogger(TAG)
    private var appServiceImpl: IMiniToParent? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isBind = false

    override fun bindService(context: Context) {
        if (isBind) {
            return
        }
        val intent = Intent()
        intent.action = INTENT_APP_SERVICE
        intent.setPackage(context.packageName)
        val bindResult = context.bindService(intent, appServiceConnection, Context.BIND_AUTO_CREATE)
        isBind = bindResult
        logger.info("bindMiniToParentService bindResult:$bindResult")
    }

    override fun unBindService(context: Context) {
        logger.info("unBindService")
        if (!isBind) {
            return
        }
        isBind = false
        openDCList.forEach {
            it.unregisterObserver()
        }
        appServiceImpl?.unregisterDCCallBack(getMiniAppInfo()?.callId, getMiniAppInfo()?.appId)
        appServiceImpl?.unregisterParentToMiniCallback(getMiniAppInfo()?.callId, getMiniAppInfo()?.appId)
        context.unbindService(appServiceConnection)
        miniAppInterface = null
        createDCLabelList.clear()
        openDCList.clear()
    }

    override fun createDC(dcLabels: List<String>, description: String): Int? {
        logger.info("createDC")
        return appServiceImpl?.createDC(
            getMiniAppInfo()?.callId,
            getMiniAppInfo()?.appId,
            dcLabels,
            description
        )
    }

    override fun closeDC(label: String) {
        logger.debug("closeDC label:$label")
        createDCLabelList.remove(label)
        openDCList.firstOrNull { DCUtils.compareDCLabel(it.dcLabel, label) }?.let {
            onDataChannelStateChanged(it, ImsDCStatus.DC_STATE_CLOSED,0)
            if (it.state != ImsDCStatus.DC_STATE_CLOSING && it.state != ImsDCStatus.DC_STATE_CLOSED){// closing或closed调用close会崩
                it.unregisterObserver()
                it.close()
            }
        }
        val map = mapOf("dcLabel" to label, "imsDCStatus" to ImsDCStatus.DC_STATE_CLOSED.ordinal)
        miniAppInterface?.callHandler(FUNCTION_NOTIFY_DATA_CHANNEL, arrayOf(JsonUtil.toJson(map)))
    }

    override fun callHandler(method: String, args: Array<Any>) {
        logger.info("callHandler, method: $method")
        miniAppInterface?.callHandler(method, args)
    }

    override fun onDataChannelStateChanged(dc: IImsDataChannel, status: ImsDCStatus?, errorCode: Int) {
        logger.info("onDataChannelStateChanged")
        appServiceImpl?.onDataChannelStateChange(
            getMiniAppInfo()?.callId,
            getMiniAppInfo()?.appId,
            dc,
            status,
            errorCode
        )
    }

    override fun sendMessageToParent(message: String, callback: IMessageCallback.Stub?) {
        appServiceImpl?.sendMessageToParent(
            getMiniAppInfo()?.callId,
            getMiniAppInfo()?.appId,
            message,
            callback
        )
    }

    override fun getCallInfo(): CallInfo? {
        return miniAppInterface?.callInfo
    }

    override fun getMiniAppInfo(): MiniAppInfo? {
        return miniAppInterface?.miniApp
    }

    override fun getMiniAppList(): MiniAppList? {
        return miniAppInterface?.miniAppListInfo
    }

    override fun selectFile(callback: OnPickMediaCallbackListener) {
        miniAppInterface?.selectFile(callback)
    }

    override fun stopApp(){
        miniAppInterface?.finishAndKillMiniAppActivity()
    }

    private val appServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            logger.info("onServiceConnected")
            appServiceImpl = IMiniToParent.Stub.asInterface(service)
            scope.launch {
                appServiceImpl?.registerParentToMiniCallback(
                    getMiniAppInfo()?.callId,
                    getMiniAppInfo()?.appId,
                    parentToMiniImpl
                )
                appServiceImpl?.registerDCCallBack(
                    getMiniAppInfo()?.callId,
                    getMiniAppInfo()?.appId,
                    dcCallbackImpl
                )
                withContext(Dispatchers.Main) {
                    miniAppInterface?.invokeOnServiceConnected()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            logger.debug("onServiceDisconnected, name: $name")
            appServiceImpl = null
        }
    }

    private val parentToMiniImpl = object : IParentToMini.Stub() {
        override fun finishMiniAppActivity() {
            logger.info("finishMiniAppActivity")
            miniAppInterface?.finishAndKillMiniAppActivity()
        }

        override fun sendMessageToMini(
            miniAppId: String?,
            message: String?,
            iMessageCallback: IMessageCallback?
        ) {
            logger.debug("sendMessageToMini miniAppId:$miniAppId, message:$message")
            if (miniAppId == null || message == null) {
                return
            }
            val notifyEvent = JsonUtil.fromJson(message, NotifyEvent::class.java)
            notifyEvent?.let {
                when (it.eventName) {
                    CommonConstants.ACTION_CALL_STATUS_CHANGE -> {
                        miniAppInterface?.invokeOnCallStateChange(it.map)
                    }
                    CommonConstants.ACTION_AUDIO_DEVICE_CHANGE -> {
                        miniAppInterface?.onAudioDeviceChange()
                    }
                    CommonConstants.ACTION_CHECK_ALIVE -> {
                        miniAppInterface?.invokeOnCheckAlive()
                        val response = AppResponse(
                            APP_RESPONSE_CODE_SUCCESS,
                            APP_RESPONSE_MESSAGE_SUCCESS,
                            "checkAlive has received, miniAppId:${getMiniAppInfo()?.appId}"
                        )
                        iMessageCallback?.reply(JsonUtil.toJson(response))
                    }
                    ACTION_REFRESH_MINI_PERMISSION -> {
                        miniAppInterface?.refreshPermission()
                    }
                    FUNCTION_DRAWING_INO_NOTIFY -> {
                        miniAppInterface?.callHandler(FUNCTION_DRAWING_INO_NOTIFY, arrayOf(JsonUtil.toJson(it.map)))
                    }
                    FUNCTION_SKETCH_STATUS_NOTIFY -> {
                        miniAppInterface?.callHandler(FUNCTION_SKETCH_STATUS_NOTIFY, arrayOf(JsonUtil.toJson(it.map)))
                    }
                    FUNCTION_SCREEN_SHARE_NOTIFY -> {
                        miniAppInterface?.callHandler(FUNCTION_SCREEN_SHARE_NOTIFY, arrayOf(JsonUtil.toJson(it.map)))
                    }
                    FUNCTION_SCREEN_SIZE_NOTIFY -> {
                        miniAppInterface?.callHandler(FUNCTION_SCREEN_SIZE_NOTIFY, arrayOf(JsonUtil.toJson(it.map)))
                    }
                    ACTION_EC_CALLBACK -> {
                        miniAppInterface?.callHandler(FUNCTION_EC_NOTIFY, arrayOf(it.map["msg"] as String))
                    }
                    ACTION_START_APP_RESPONSE -> {
                        miniAppInterface?.callHandler(FUNCTION_START_ADVERSE_APP_RESPONSE_NOTIFY, arrayOf(JsonUtil.toJson(it.map)))
                    }
                    FUNCTION_VIDEO_WINDOW_NOTIFY -> {
                        miniAppInterface?.callHandler(FUNCTION_VIDEO_WINDOW_NOTIFY, arrayOf(JsonUtil.toJson(it.map)))
                    }
                    else -> { }
                }
            }
        }
    }

    private val dcCallbackImpl = object : IDCCallback.Stub() {
        override fun onDcCreated(
            callId: String?,
            streamId: String?,
            iImsDataChannel: IImsDataChannel?
        ) {
            logger.info("IDCCallback.Stub onDcCreated callId:$callId, streamId:$streamId, iImsDataChannel:${iImsDataChannel?.state}")
            if (callId == null || streamId == null || iImsDataChannel == null) {
                return
            }
            if (callId != getCallInfo()?.telecomCallId) {
                logger.warn("but mCallInfo?.telecomCallId is ${getCallInfo()?.telecomCallId}")
            }
            val dcLabel = iImsDataChannel.dcLabel
            if (!createDCLabelList.contains(dcLabel)) {
                createDCLabelList.add(dcLabel)
            }
            logger.debug("IDCCallback.Stub onDcCreated register observer dcLabel:$dcLabel")
            try {
                iImsDataChannel.registerObserver(
                    ImsDCObserverImpl(
                        dcLabel,
                        iImsDataChannel
                    )
                )
            } catch (e: RemoteException) {
                logger.error("IDCCallback.Stub onDcCreated register observer error:${e.message}")
            }
            onDataChannelStateChange(iImsDataChannel)
        }
    }

    private fun onDataChannelStateChange(dc: IImsDataChannel?) {
        logger.debug("DCCallbackImpl onDataChannelStateChange dc:${dc?.state}")
        if (dc == null) {
            return
        }
        val state = dc.state
        // 创建和state变化，可能会增加两个
        val exist = openDCList.firstOrNull { DCUtils.compareDCLabel(it.dcLabel, dc.dcLabel) }
        if (ImsDCStatus.DC_STATE_OPEN == state && exist == null) {
            openDCList.add(dc)
        }

        if (ImsDCStatus.DC_STATE_CLOSED == state && exist != null) {
            openDCList.remove(exist)
            createDCLabelList.remove(exist.dcLabel)
        }
        scope.launch(Dispatchers.Main) {
            logger.debug("DCCallbackImpl onDataChannelStateChange callHandler")
            val map = mapOf("dcLabel" to dc.dcLabel, "imsDCStatus" to state.ordinal)
            miniAppInterface?.callHandler(FUNCTION_NOTIFY_DATA_CHANNEL, arrayOf(JsonUtil.toJson(map)))
        }
        logger.debug("DCCallbackImpl onDataChannelStateChange now openDCList size: ${openDCList.size}")
    }

    inner class ImsDCObserverImpl(
        private val label: String,
        private val dc: IImsDataChannel
    ) : IImsDCObserver.Stub() {

        override fun onDataChannelStateChange(status: ImsDCStatus?, errCode: Int) {
            logger.debug("ImsDCObserverImpl onDataChannelStateChange status:$status")
            appServiceImpl?.onDataChannelStateChange(getMiniAppInfo()?.callId, getMiniAppInfo()?.appId, dc, status, errCode)
            onDataChannelStateChange(dc)
        }

        override fun onMessage(data: ByteArray?, length: Int) {
            logger.debug("AC ImsDCObserverImpl onMessage dcLabel:$label,bytes:${data?.toString()}, length:$length")
            if (data == null) {
                return
            }
            scope.launch(Dispatchers.Default) {
                val map = mapOf("dcLabel" to label, "message" to FileUtils.byteArrayToBase64(data))
                miniAppInterface?.callHandler(FUNCTION_NOTIFY_MESSAGE, arrayOf(JsonUtil.toJson(map)))
            }
        }
    }
}