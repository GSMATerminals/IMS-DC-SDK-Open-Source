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

package com.ct.ertclib.dc.core.service

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.text.TextUtils
import androidx.annotation.RequiresApi
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_AUDIO_DEVICE_CHANGE
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REGISTER_EC
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.core.data.common.SdkInfo
import com.ct.ertclib.dc.core.port.dc.IDcCreateListener
import com.ct.ertclib.dc.core.data.event.CloseAdcEvent
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_CALL_STATUS_CHANGE
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_CHECK_ALIVE
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_EC_CALLBACK
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_GET_SDK_VERSION
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REQUEST_EC
import com.ct.ertclib.dc.core.data.event.NotifyEvent
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.factory.AppServiceEventDispatcherFactory
import com.ct.ertclib.dc.core.manager.common.ExpandingCapacityManager
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.miniapp.MiniAppStartManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.miniapp.aidl.IDCCallback
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.miniapp.aidl.IMiniToParent
import com.ct.ertclib.dc.core.miniapp.aidl.IParentToMini
import com.ct.ertclib.dc.core.port.expandcapacity.IExpandingCapacityListener
import com.ct.ertclib.dc.core.port.manager.IAppServiceManager
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.EmptyCoroutineContext

class MiniAppService : Service(), CoroutineScope by MainScope(), KoinComponent  {

    companion object {
        private const val TAG = "MiniAppService"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    private val mDcCallBackMap = ConcurrentHashMap<String, IDCCallback>()

    val mParentToMiniCallbackMap = ConcurrentHashMap<String, IParentToMini>()

    private val mCallStatusListenerMap = ConcurrentHashMap<String, ICallStateListener>()
    private val appServiceManager: IAppServiceManager by inject()

    inner class CallStatusListener(private val appService: MiniAppService, val telecomCallId: String,val appId: String) :
        ICallStateListener {
        override fun onCallAdded(context: Context, callInfo: CallInfo) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("CallStatusListener onCallAdded")
            }
            notifyCallStateToMini(appId, "onCallAdded", callInfo)
        }

        override fun onCallRemoved(context: Context, callInfo: CallInfo) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("CallStatusListener onCallRemoved")
            }
            notifyCallStateToMini(appId, "onCallRemoved", callInfo)
        }

        override fun onCallStateChanged(callInfo: CallInfo, state: Int) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("CallStatusListener onCallStateChanged")
            }
            notifyCallStateToMini(appId, "onCallStateChanged", callInfo)
        }

        override fun onAudioDeviceChange() {
            val event = NotifyEvent(
                ACTION_AUDIO_DEVICE_CHANGE,
                mutableMapOf()
            )
            sLogger.debug("CallStatusListener onAudioDeviceChange appId:$appId, request:$event")
            val iParentToMini = appService.mParentToMiniCallbackMap[getKey(telecomCallId,appId)]
            try {
                iParentToMini?.sendMessageToMini(appId, JsonUtil.toJson(event), null)
            } catch (e: RemoteException) {
                if (sLogger.isDebugActivated) {
                    sLogger.error("CallStatusListener onAudioDeviceChange ", e)
                }
            }

        }

        private fun notifyCallStateToMini(appId: String, tag: String, callInfo: CallInfo) {
            val event = NotifyEvent(
                ACTION_CALL_STATUS_CHANGE,
                mutableMapOf("callState" to callInfo.state)
            )
            sLogger.debug("CallStatusListener $tag appId:$appId, request:$event")
            val iParentToMini = appService.mParentToMiniCallbackMap[getKey(callInfo.telecomCallId,appId)]
            try {
                iParentToMini?.sendMessageToMini(appId, JsonUtil.toJson(event), null)
            } catch (e: RemoteException) {
                if (sLogger.isDebugActivated) {
                    sLogger.error("CallStatusListener $tag sendMessageToMini", e)
                }
            }
        }
    }

    inner class DcCreateListener(private val appService: MiniAppService, val appId: String) :
        IDcCreateListener {
        override fun onDataChannelCreated(
            telecomCallId: String,
            streamId: String,
            imsDataChannel: IImsDataChannel
        ) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("DcCreateListener onDataChannelCreated telecomCallId:$telecomCallId, streamId:$streamId")
            }

            try {
                mDcCallBackMap[appId]?.onDcCreated(telecomCallId, streamId, imsDataChannel)
            } catch (e: RemoteException) {
                if (sLogger.isDebugActivated) {
                    sLogger.error("DcCreateListener onDataChannelCreated", e)
                }
                appService.onRemoteError(telecomCallId,appId)
            }
        }
    }

    fun getKey(callId:String,appId: String):String{
        return "$callId++$appId"
    }

    fun getCallIdFromKey(key:String):String{
        if (TextUtils.isEmpty(key) || !key.contains("++")){
            return ""
        }
        val split = key.split("++")
        if(split.size!=2){
            return ""
        }
        return split[0]
    }

    fun getAppIdFromKey(key:String):String{
        if (TextUtils.isEmpty(key) || !key.contains("++")){
            return ""
        }
        val split = key.split("++")
        if(split.size!=2){
            return ""
        }
        return split[1]
    }

    fun onRemoteError(telecomCallId: String,appId: String) {
        mParentToMiniCallbackMap.remove(getKey(telecomCallId,appId))
        mDcCallBackMap.remove(appId)
    }

    inner class MiniToParentAidlImpl(private val appService: MiniAppService) : IMiniToParent.Stub() {

        override fun createDC(
            telecomCallId: String,
            appId: String,
            lables: MutableList<String>,
            descrption: String
        ): Int {
            if (sLogger.isDebugActivated) {
                sLogger.debug("MiniToParentImpl createDC, miniApId:$appId, labels:${lables},des:$descrption")
            }
            if (lables.isNullOrEmpty() || appId.isNullOrEmpty() || descrption.isNullOrEmpty()) {
                return 1
            }
            return MiniAppManager.getAppPackageManager(telecomCallId)
                ?.createApplicationDataChannelsInternal(
                    appId,
                    lables.toTypedArray(),
                    descrption
                ) ?: 1
        }

        override fun registerDCCallBack(telecomCallId: String,appId: String, idcCallback: IDCCallback) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("registerDCCallBack, appId:$appId, dcCallback:$idcCallback")
            }
            if (appId.isNullOrEmpty() || idcCallback == null) {
                return
            }
            mDcCallBackMap[appId] = idcCallback
            MiniAppManager.getAppPackageManager(telecomCallId)
                ?.registerAppDataChannelCallbackInternal(
                    appId,
                    DcCreateListener(appService, appId)
                )
        }

        override fun registerParentToMiniCallback(telecomCallId: String,appId: String, iParentToMini: IParentToMini) {
            sLogger.debug("registerParentToMiniCallback, appId:$appId, telecomCallId: $telecomCallId, dcCallback:$iParentToMini")
            if (appId.isEmpty()) {
                return
            }
            mParentToMiniCallbackMap[getKey(telecomCallId,appId)] = iParentToMini
            val listenerWrapper = CallStatusListener(appService, telecomCallId, appId)
            MiniAppManager.getAppPackageManager(telecomCallId)
                ?.registerCallStateChangeCallbackInternal(
                    appId,
                    listenerWrapper
                )
            mCallStatusListenerMap[appId] = listenerWrapper
        }

        override fun sendMessageToParent(
            telecomCallId: String,
            appId: String,
            message: String,
            iMessageCallback: IMessageCallback?
        ) {
            try {
                if (sLogger.isDebugActivated) {
                    sLogger.debug("sendMessageToParent, appId:$appId, message:$message")
                }
                if (appId.isEmpty() || message.isEmpty()) {
                    return
                }

                //后续appRequest尽量放到dispatchers类中进行处理，避免代码过于臃肿，不好扩展
                val appRequest = JsonUtil.fromJson(message, AppRequest::class.java)
                appRequest?.let {
                    val replayMessage: String
                    when (appRequest.actionName) {
                        ACTION_GET_SDK_VERSION -> {
                            val packageInfo = appService.applicationContext.packageManager.getPackageInfo(
                                appService.applicationContext.packageName,
                                0
                            )
                            val versionCode = packageInfo.versionCode
                            val versionName = packageInfo.versionName

                            replayMessage = AppResponse(
                                1,
                                "success",
                                mapOf("getSdkVersions" to SdkInfo(versionCode, versionName ?: ""))
                            ).toJson()
                            iMessageCallback?.reply(replayMessage)
                        }
                        ACTION_REGISTER_EC -> {
                            if (appRequest.map["providerModules"] != null){
                                val list = appRequest.map["providerModules"] as ArrayList<String>
                                val providerModules = ConcurrentHashMap<String, ArrayList<String>>()
                                list.forEach {
                                    if (it.contains("-")){
                                        val provider = it.split("-")[0]
                                        val module = it.split("-")[1]
                                        if (providerModules[provider] == null){
                                            providerModules[provider] = ArrayList<String>()
                                        }
                                        providerModules[provider]?.add(module)
                                    }
                                }
                                ExpandingCapacityManager.instance.registerECListener(telecomCallId,appId,providerModules,object :IExpandingCapacityListener{
                                    override fun onCallback(content: String?) {
                                        val event = NotifyEvent(
                                            ACTION_EC_CALLBACK,
                                            mutableMapOf("msg" to content)
                                        )
                                        if (sLogger.isDebugActivated) {
                                            sLogger.debug("ExpandingCapacityManager onCallback content: $content appId:$appId, request:$event")
                                        }
                                        mParentToMiniCallbackMap[getKey(telecomCallId,appId)]?.sendMessageToMini(appId,JsonUtil.toJson(event),null)
                                    }
                                })
                            }
                        }
                        ACTION_REQUEST_EC -> {
                            val requestStr = JsonUtil.toJson(appRequest.map)
                            ExpandingCapacityManager.instance.request(this@MiniAppService,telecomCallId,appId,requestStr)
                        }
                        else -> {
                            sLogger.info("other request...")
                        }
                    }
                    AppServiceEventDispatcherFactory.getDispatcher(appRequest.eventName).dispatchEvent(telecomCallId, appId, appRequest, iMessageCallback)
                }
            } catch (e:Exception){
                e.printStackTrace()
            }
        }

        override fun unregisterDCCallBack(telecomCallId: String?, appId: String?) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("unregisterDCCallBack, appId:$appId")
            }
            if (appId.isNullOrEmpty()) {
                return
            }
            mDcCallBackMap.remove(appId)
            MiniAppManager.getAppPackageManager(telecomCallId)
                ?.unregisterAppDataChannelCallbackInternal(appId)
        }

        override fun unregisterParentToMiniCallback(telecomCallId: String?, appId: String?) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("unregisterParentToMiniCallback, appId:$appId")
            }
            if (appId.isNullOrEmpty()) {
                return
            }
            telecomCallId?.let {
                mParentToMiniCallbackMap.remove(getKey(telecomCallId, appId))
            }
            val iCallStateListener = mCallStatusListenerMap[appId]
            if (iCallStateListener != null) {
                MiniAppManager.getAppPackageManager(telecomCallId)
                    ?.unregisterCallStateListenerInternal(appId, iCallStateListener)
                mCallStatusListenerMap.remove(appId)
            }
            telecomCallId?.let {
                ExpandingCapacityManager.instance.unregisterECListener(this@MiniAppService,telecomCallId,appId)
            }
        }

        override fun onDataChannelStateChange(
            telecomCallId: String?,
            appId: String?,
            iImsDataChannel: IImsDataChannel?,
            status: ImsDCStatus?,
            errCode: Int
        ) {
            if (status == ImsDCStatus.DC_STATE_CLOSED) {
                iImsDataChannel?.let {
                    StateFlowManager.emitCloseAdcEvent(CloseAdcEvent(0, CloseAdcEvent.CLOSE_ADC, appId, iImsDataChannel))
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onBind(intent: Intent?): IBinder {
        if (sLogger.isDebugActivated) sLogger.debug("onBind processName:${Application.getProcessName()}")

        MiniAppStartManager.setMiniAppAidlService(this)

        //使用kotlin协程调用sendMessageToMini 每隔5000毫秒发送一次消息，避免parent不知道miniapp是否存活
        val event = NotifyEvent(
            ACTION_CHECK_ALIVE,
            mutableMapOf()
        )
        launch(EmptyCoroutineContext) {
            while (true) {
                withContext(Dispatchers.IO) {
                    mParentToMiniCallbackMap.forEach {
                        val iParentToMini = it.value
                        try {
                            iParentToMini.sendMessageToMini(getAppIdFromKey(it.key), JsonUtil.toJson(event), object : IMessageCallback.Stub() {
                                override fun reply(message: String?) {
                                    if (sLogger.isDebugActivated) {
                                        sLogger.info("checkAlive reply $message")
                                    }
                                }
                            })
                        } catch (e: RemoteException) {
                            if (sLogger.isDebugActivated) {
                                sLogger.error("sendMessageToMini", e)
                            }
                            onRemoteError(getCallIdFromKey(it.key),getAppIdFromKey(it.key))
                        }
                    }
                }
                kotlinx.coroutines.delay(5000)
            }
        }

        return MiniToParentAidlImpl(this)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        if (sLogger.isDebugActivated) {
            sLogger.debug("onCreate, processName:${Application.getProcessName()}")
        }
        appServiceManager.initManager()
        super.onCreate()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onDestroy() {
        if (sLogger.isDebugActivated) {
            sLogger.debug("onDestroy, processName:${Application.getProcessName()}")
        }
        appServiceManager.release()
        cancel()
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onUnbind(intent: Intent?): Boolean {
        if (sLogger.isDebugActivated) {
            sLogger.debug("onDestroy, processName:${Application.getProcessName()}")
        }
        MiniAppStartManager.setMiniAppAidlService(null)
        return super.onUnbind(intent)
    }
}