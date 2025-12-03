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
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.IImsDataChannelCallback
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.core.port.dc.IDcCreateListener
import com.ct.ertclib.dc.core.port.dc.IControlDcCreateListener
import com.ct.ertclib.dc.core.port.dc.ImsDcServiceConnectionCallback
import com.ct.ertclib.dc.core.data.event.CloseAdcEvent
import com.ct.ertclib.dc.core.data.miniapp.CreateAdcParams
import com.ct.ertclib.dc.core.data.miniapp.DataChannel
import com.ct.ertclib.dc.core.data.miniapp.DataChannelApp
import com.ct.ertclib.dc.core.data.miniapp.DataChannelAppInfo
import com.ct.ertclib.dc.core.manager.common.DialerEntryManager
import com.ct.ertclib.dc.core.manager.common.ExpandingCapacityManager
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.port.dc.IAdverseDcCreateListener
import com.ct.ertclib.dc.core.utils.common.DCUtils
import com.ct.ertclib.dc.core.utils.common.XmlUtils
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.Class
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap

class DCManager() : ICallStateListener, ImsDcServiceConnectionCallback {

    companion object {
        private const val TAG = "DCManager"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)


    private val mBdcCreateListenerMap = ConcurrentHashMap<String, IDcCreateListener>()
    private val mAdcCreateListenerMap = ConcurrentHashMap<String, IDcCreateListener>()
    private val mControlAdcCreateListenerMap = ConcurrentHashMap<String, IControlDcCreateListener>()
    private val mAdverseAdcCreateListenerMap = ConcurrentHashMap<String, IAdverseDcCreateListener>()
    private val mImsDcCallbackMap = ConcurrentHashMap<String, ImsDcCallback>()
    // Adc缓存
    private val mAdcMap = ConcurrentHashMap<String, IImsDataChannel>()
    private val mDcListMap = ConcurrentHashMap<String, ArrayList<IImsDataChannel>>()
    private val mCallInfoList = ArrayList<CallInfo>()
    private var mIsDataChannelServiceConnected = false
    private var mIsNetworkManagerInit = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentCallId:String? = null
    private var job1:Job? = null
    private var job2:Job? = null

    // 请求芯片创建ADC时，先排队
    private var createAdcQueue: ArrayBlockingQueue<CreateAdcParams> = ArrayBlockingQueue(100)
    // 没有调用OEM的创建接口、最后一次调用创建接口且该次创建的ADC全部为open状态
    private var canCreateADC = true

    fun registerBDCCallback(
        telecomCallId: String,
        dcCreateListener: IDcCreateListener
    ) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("register BDC callback callId: $telecomCallId")
        }
        mBdcCreateListenerMap[telecomCallId] = dcCreateListener
    }

    override fun onCallAdded(context: Context, callInfo: CallInfo) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("onCallAdded callInfo: $callInfo")
        }
        mCallInfoList.add(callInfo)
        if (!mIsDataChannelServiceConnected) {
            init(context)
        } else {
            val imsDcCallback = ImsDcCallback()
            DCServiceManager.setDcCallback(
                imsDcCallback,
                callInfo.slotId,
                callInfo.telecomCallId
            )
            mImsDcCallbackMap[getImsDcCallbackKey(callInfo.slotId,callInfo.telecomCallId)] = imsDcCallback
        }
    }

    private fun init(context: Context) {
        sLogger.debug("init")
        if (mIsNetworkManagerInit) {
            sLogger.debug("DCManager already init")
            return
        }
        mIsNetworkManagerInit = true
        DCServiceManager.setImsDcServiceConnectionCallback(this)
        DCServiceManager.bindDcService(context)
        ExpandingCapacityManager.instance.init(context)
        DialerEntryManager.instance.init(context)
        job1 = scope.launch {
            StateFlowManager.closeAdcEventFlow.distinctUntilChanged().collect { event ->
                if (event.message == CloseAdcEvent.CLOSE_ADC) {
                    val adcKey = getAdcKey(event.iImsDataChannel.telecomCallId, event.iImsDataChannel.dcLabel)
                    mAdcMap.remove(adcKey)
                    sLogger.debug("CloseAdcEvent onEvent remove $adcKey")
                }
            }
        }
    }

    override fun onCallRemoved(context: Context, callInfo: CallInfo) {
        if (sLogger.isDebugActivated) sLogger.debug("onCallRemoved callInfo:$callInfo")
        mCallInfoList.remove(callInfo)
        val telecomCallId = callInfo.telecomCallId
        mBdcCreateListenerMap.remove(telecomCallId)
        mAdcCreateListenerMap.forEach{(key, _) ->
            if (key.startsWith(callInfo.telecomCallId)){
                mAdcCreateListenerMap.remove(key)
            }
        }
        mControlAdcCreateListenerMap.remove(telecomCallId)
        mAdverseAdcCreateListenerMap.remove(telecomCallId)
        mDcListMap.remove(telecomCallId)
        // 删除本次通话相关的Adc缓存
        mAdcMap.forEach{(key, _) ->
            if (key.startsWith(callInfo.telecomCallId)){
                mAdcMap.remove(key)
                sLogger.debug("onCallRemoved remove $key")
            }
        }
        if (mIsDataChannelServiceConnected) {
            DCServiceManager.setDcCallback(null, callInfo.slotId, telecomCallId)
            mImsDcCallbackMap.remove(getImsDcCallbackKey(callInfo.slotId,callInfo.telecomCallId))
        }
    }

    override fun onCallStateChanged(callInfo: CallInfo, state: Int) {
    }

    override fun onAudioDeviceChange() {
        sLogger.debug("onAudioDeviceChange")
    }

    override fun onServiceDisconnected() {
        sLogger.debug("onServiceDisconnected")
        mIsDataChannelServiceConnected = false
        mIsNetworkManagerInit = false
        canCreateADC = true
        createAdcQueue.clear()
    }

    override fun onServiceConnected() {
        sLogger.debug("onServiceConnected")
        mIsDataChannelServiceConnected = true
        mCallInfoList.forEach {
            val imsDcCallback = ImsDcCallback()
            DCServiceManager.setDcCallback(
                imsDcCallback,
                it.slotId,
                it.telecomCallId
            )
            mImsDcCallbackMap[getImsDcCallbackKey(it.slotId,it.telecomCallId)] = imsDcCallback
        }
        startCreateADCQueue()
    }

    inner class ImsDcCallback : IImsDataChannelCallback.Stub() {

        override fun onBootstrapDataChannelResponse(dc: IImsDataChannel?) {
            sLogger.info("onBootstrapDataChannelResponse")
            if (dc == null) {
                if (sLogger.isDebugActivated) {
                    sLogger.debug("onBootstrapDataChannelResponse bdc is null")
                }
                return
            }

            val callId = dc.telecomCallId
            val state = dc.state
            if (sLogger.isDebugActivated) {
                sLogger.debug("onBootstrapDataChannelResponse telecomCallId:$callId, DcStatus:$state")
            }

            sLogger.info("onBootstrapDataChannelResponse mBdcCreateListenerMap:$mBdcCreateListenerMap")
            val bdcCreateListener = mBdcCreateListenerMap[callId]
            sLogger.info("onBootstrapDataChannelResponse bdcCreateListener:$bdcCreateListener")
            if (bdcCreateListener == null) {
                sLogger.info("onBootstrapDataChannelResponse bdc createListener is null")
            } else {
                bdcCreateListener.onDataChannelCreated(callId, dc.streamId, dc)
            }

        }

        override fun onApplicationDataChannelResponse(dc: IImsDataChannel?) {
            canCreateADC = true
            sLogger.info("onApplicationDataChannelResponse")
            if (dc == null) {
                if (sLogger.isDebugActivated) {
                    sLogger.debug("onApplicationDataChannelResponse adc is null")
                }
                return
            }
            val callId = dc.telecomCallId
            val appIdFromDcLabel = getAppIdFromDcLabel(dc.dcLabel)
            val state = dc.state
            if (sLogger.isDebugActivated) {
                sLogger.debug("onApplicationDataChannelResponse telecomCallId:$callId, DcStatus:$state, appIdFromDcLabel:$appIdFromDcLabel, dcLabel:${dc.dcLabel},streamId:${dc.streamId}")
            }
            mAdcMap[getAdcKey(callId, dc.dcLabel)] = dc
            val adcListenerKey = getAdcListenerKey(callId, appIdFromDcLabel)

            // 有些ADC不用回调给小程序
            if (dc.dcLabel.contains("_${CommonConstants.DC_APPID_OWN}_")) {
                // SDK与AS自有adc
                val controlAdcCreateListener = mControlAdcCreateListenerMap[callId]
                if (controlAdcCreateListener != null && appIdFromDcLabel != null) {
                    controlAdcCreateListener.onOwnDataChannelCreated(
                        callId,
                        appIdFromDcLabel,
                        dc.streamId,
                        dc
                    )
                }
            } else if (dc.dcLabel.endsWith("_${CommonConstants.DC_LABEL_CONTROL}")) {
                // 借用adc
                val controlAdcCreateListener = mControlAdcCreateListenerMap[callId]
                if (controlAdcCreateListener != null && appIdFromDcLabel != null) {
                    controlAdcCreateListener.onControlDataChannelCreated(
                        callId,
                        appIdFromDcLabel,
                        dc.streamId,
                        dc
                    )
                }
            } else {
                val adcCreateListener = mAdcCreateListenerMap[adcListenerKey]
                if (adcCreateListener == null) {
                    sLogger.info("onApplicationDataChannelResponse adc createListener is null, adcListenerKey:$adcListenerKey dcLabel:${dc.dcLabel}")
                    addDataChannel(adcListenerKey, dc)
                    // 业务发起方创建ADC，触发被动拉起小程序等逻辑，与协商ADC机制并行
                    val adverseAdcCreateListener = mAdverseAdcCreateListenerMap[callId]
                    if (adverseAdcCreateListener != null
                        && appIdFromDcLabel != null
                        && dc.dcLabel.startsWith("remote_")
                    ) {
                        adverseAdcCreateListener.onAdverseDataChannelCreated(
                            callId,
                            appIdFromDcLabel,
                            dc.streamId,
                            dc
                        )
                    }
                } else {
                    adcCreateListener.onDataChannelCreated(callId, dc.streamId, dc)
                }
            }
        }

    }

    fun getAppIdFromDcLabel(label: String?): String? {
        label?.let {
            val splits = it.split("_")
            if (splits.size == 4) {
                return splits[1]
            }
            if (sLogger.isDebugActivated) {
                sLogger.debug("getAppIdFromDcLabel invalid dc label:$label")
            }
        }
        sLogger.info("getAppIdFromDcLabel label:$label")
        return null
    }

    fun getAdcListenerKey(telecomCallId: String, appId: String?): String {
        return "$telecomCallId" + "_$appId"
    }

    fun getAdcKey(telecomCallId: String, dcLabel: String?): String {
        return telecomCallId + "${dcLabel?.let { DCUtils.ignoreRole(it) }}"
    }

    fun getImsDcCallbackKey(slotId: Int, telecomCallId: String?): String {
        return "$slotId" + "_$telecomCallId"
    }

    fun addDataChannel(adcKey: String, dc: IImsDataChannel) {
        mDcListMap.computeIfAbsent(adcKey) {
            ArrayList()
        }.add(dc)
    }

    fun onCallServiceUnbind(context: Context) {
        sLogger.debug("onCallServiceUnbind")
        job1?.cancel()
        job2?.cancel()
        job1 = null
        job2 = null
        DCServiceManager.unbindDcService(context)
        ExpandingCapacityManager.instance.release(context)
        DialerEntryManager.instance.release(context)
        FloatingBallDataManager.release()
    }

    fun createApplicationDataChannels(
        slotId: Int,
        callId: String,
        remoteNumber: String?,
        labels: Array<String>,
        description: String
    ): Int {
        if (sLogger.isDebugActivated) {
            sLogger.debug("createApplicationDataChannels slotId$slotId, callId:$callId, remoteNumber:$remoteNumber, lables:$labels, des:$description")
        }

        if (mIsDataChannelServiceConnected) {
            createAdcQueue.add(CreateAdcParams(slotId, callId, remoteNumber,labels, description))
            return 0
        }
        return 1
    }

    fun registerAdverseAppDataChannelCallback(callId: String, dcCreateListener: IAdverseDcCreateListener) {
        mAdverseAdcCreateListenerMap[callId] = dcCreateListener
    }

    fun registerAppDataChannelCallback(callId: String, appId:String, dcCreateListener: IDcCreateListener) {
        mAdcCreateListenerMap[getAdcListenerKey(callId, appId)] = dcCreateListener
        // 这里要确保小程序已经加载完毕再通知，否则就会丢失，先简单等待500毫秒
        scope.launch {
            delay(500)
            notifyCachedAppDataChannels(callId, appId, dcCreateListener)
        }
    }

    fun registerControlAppDataChannelCallback(callId: String, dcCreateListener: IControlDcCreateListener) {
        mControlAdcCreateListenerMap[callId] = dcCreateListener
    }

    private fun notifyCachedAppDataChannels(
        callId: String,
        appId: String,
        dcCreateListener: IDcCreateListener
    ) {
        val adcListenerKey = getAdcListenerKey(callId, appId)
        val iImsDataChannels = mDcListMap[adcListenerKey]
        if (iImsDataChannels != null) {
            for (idc in iImsDataChannels) {
                val streamId = idc.streamId
                dcCreateListener.onDataChannelCreated(callId, streamId, idc)
            }
            iImsDataChannels.clear()
        }
        mDcListMap.remove(callId)
    }

    fun unregisterAdverseAppDataChannelCallback(callId: String) {
        mAdverseAdcCreateListenerMap.remove(callId)
    }

    fun unregisterAppDataChannelCallback(callId: String, appId: String) {
        mAdcCreateListenerMap.remove(getAdcListenerKey(callId, appId))
    }

    fun unregisterControlAppDataChannelCallback(callId: String) {
        mControlAdcCreateListenerMap.remove(callId)
    }

    fun unBindService(context: Context) {
        DCServiceManager.unbindDcService(context)
        ExpandingCapacityManager.instance.release(context)
        DialerEntryManager.instance.release(context)
    }

    private fun startCreateADCQueue(){
        job2 = scope.launch {
            while (isActive && mIsDataChannelServiceConnected) {
                if (!canCreateADC){
                    delay(1000)
                    continue
                }

                var isConnecting = false
                mAdcMap.forEach{(_, dc) ->
                    if (dc.state == ImsDCStatus.DC_STATE_CONNECTING){
                        isConnecting = true
                    }
                }
                if (isConnecting){
                    delay(1000)
                    continue
                }

                val data = createAdcQueue.poll()
                if (data == null) {
                    delay(1000)
                    continue
                }
                if (data.callId != currentCallId){
                    delay(200)
                    continue
                }
                createADC( data.slotId, data.callId, data.remoteNumber,data.labels, data.description)
                // 强制等待3秒
                delay(3000)
            }
        }
    }

    private fun createADC(
        slotId: Int,
        callId: String,
        remoteNumber: String?,
        labels: Array<String>,
        description: String
    ){
        canCreateADC = false
        val shouldCreateLabels = mutableListOf<String>()
        // 检查是否有adc缓存，有的话就直接返回缓存adc，不用再次创建
        val imsDcCallback = mImsDcCallbackMap[getImsDcCallbackKey(slotId,callId)]
        sLogger.debug("createApplicationDataChannels mAdcMap.size: ${mAdcMap.size}")
        labels.forEach {
            val adcKey = getAdcKey(callId,it)
            val dc = mAdcMap[adcKey]
            if (dc != null && dc.state != null){
                sLogger.debug("createApplicationDataChannels query in caches $adcKey state:${dc.state}")
            }
            if (DCServiceManager.bindDcServiceResult() && imsDcCallback!=null && dc != null && dc.state != null && dc.state != ImsDCStatus.DC_STATE_CLOSED){
                if (dc.state == ImsDCStatus.DC_STATE_CONNECTING || dc.state == ImsDCStatus.DC_STATE_OPEN){
                    sLogger.debug("createApplicationDataChannels from caches $adcKey")
                    imsDcCallback.onApplicationDataChannelResponse(dc)
                } else {
                    // 相同dcLabel的dc正在关闭
                    sLogger.debug("createApplicationDataChannels dc maybe closing,state:${dc.state}")
                    canCreateADC = true
                    return
                }
            } else {
                mAdcMap.remove(adcKey)
                sLogger.debug("createApplicationDataChannels from dcservice $adcKey")
                shouldCreateLabels.add(it)
                // todo 还要重新构建description
            }
        }
        if (shouldCreateLabels.size == 0){
            canCreateADC = true
            return
        }
        val shouldCreateDescription = createNewXml(shouldCreateLabels.toTypedArray(), description)
        sLogger.debug("createApplicationDataChannels shouldCreateLabels:$shouldCreateLabels, shouldCreateDescription:$shouldCreateDescription")
        val result = DCServiceManager.createImsDc(shouldCreateLabels.toTypedArray(), shouldCreateDescription, slotId, callId, remoteNumber)
        if (result == 1){
            canCreateADC = true
        }
    }

    fun setCurrentCallId(callId:String?){
        currentCallId = callId
    }

    private fun createNewXml(labels: Array<String>, description: String): String {
        return try {
            val classes = arrayOf<Class<*>>(
                DataChannelAppInfo::class.java,
                DataChannelApp::class.java,
                DataChannel::class.java
            )

            // 解析XML
            val dataChannelAppInfo = XmlUtils.parseXml(description, classes, DataChannelAppInfo::class.java)

            // 过滤DataChannel列表，只保留包含在labels中的dcLabel
            val filteredDataChannels = dataChannelAppInfo.dataChannelApp.dataChannelList
                .filter { dataChannel ->
                    labels.contains(dataChannel.dcLabel)
                }
                .toMutableList() // 转换为可变列表以便后续操作

            // 更新dataChannelList
            dataChannelAppInfo.dataChannelApp.dataChannelList = filteredDataChannels

            // 序列化回XML
            XmlUtils.toXml(dataChannelAppInfo, classes)

        } catch (e: Exception) {
            e.printStackTrace()
            // 发生异常时返回原始description
            description
        }
    }
}