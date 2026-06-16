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

package com.ct.ertclib.dc.core.miniapp

import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.AS_MODULE_PLATFORM_INFO_EVENT
import com.ct.ertclib.dc.core.data.miniapp.Module
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback
import com.newcalllib.datachannel.V1_0.IImsDCObserver
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * 用于SDK与Root AS通信
 * 接口回调动作
 */

class MiniAppRootADCImpl(private val onADCParamsOk:OnADCParamsOk) {

    companion object {
        private const val TAG = "MiniAppRootADCImpl"
        private const val ROOT_DC_LABEL = "local_${CommonConstants.DC_APPID_ROOT}_0_${CommonConstants.DC_LABEL_ROOT}"
    }

    data class RootDataMsg(val module:String, val event: String, val dcLabel: String, val dataBase64:String)
    data class QueueData(val module:Module, val event: String, val originData:ByteArray, val onSendCallback:OnSendCallback)

    interface OnADCListener{
        fun onMessage(event: String, data: ByteArray, length: Int)
    }

    interface OnADCParamsOk{
        fun onCreateADCParams(appId: String, toTypedArray: Array<String>, description: String): Int
    }

    interface OnSendCallback{
        fun onSendDataResult(state: Int)
    }

    private val sLogger: Logger = Logger.getLogger(TAG)
    private var mAdc :IImsDataChannel? = null
    private val moduleListenerMap = ConcurrentHashMap<String, OnADCListener>()

    private var mDataQueue: ArrayBlockingQueue<QueueData> = ArrayBlockingQueue(10000)
    private var sendResultOK = true

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job:Job? = null

    fun onDCCreated(imsDataChannel: IImsDataChannel) {
        sLogger.info("MiniAppRootADCImpl onDCCreated dcLabel:${imsDataChannel.dcLabel}")
        mAdc = imsDataChannel
        imsDataChannel.registerObserver(object : IImsDCObserver.Stub() {
            override fun onDataChannelStateChange(status: ImsDCStatus?, errCode: Int) {
                sLogger.info("MiniAppRootADCImpl onDataChannelStateChange:${status},dcLabel:${imsDataChannel.dcLabel}")
                if (status == ImsDCStatus.DC_STATE_OPEN){
                    startSendQueue()
                } else {
                    stopSendQueue()
                }
            }

            override fun onMessage(data: ByteArray?, length: Int) {
                try {
                    data?.let {
                        val msg = String(data)
                        sLogger.info("MiniAppRootADCImpl onMessage:${msg}")
                        msg.let {
                            val rootDataMsg = JsonUtil.fromJson(it, RootDataMsg::class.java)
                            if (rootDataMsg != null) {
                                val bs = FileUtils.base64ToByteArray(rootDataMsg.dataBase64)
                                moduleListenerMap[rootDataMsg.module]?.onMessage(rootDataMsg.event, bs,bs.size)
                            }
                        }
                    }
                } catch (e:Exception){
                    e.printStackTrace()
                }
            }
        })
    }

    fun createDC() {
        val labels = mutableListOf<String>()
        labels.add(ROOT_DC_LABEL)
        val description =
            "<DataChannelAppInfo><DataChannelApp appId=\"${CommonConstants.DC_APPID_ROOT}\"><DataChannel dcId=\"${CommonConstants.DC_LABEL_ROOT}\"><StreamId></StreamId><DcLabel>${ROOT_DC_LABEL}</DcLabel><UseCase>0</UseCase><Subprotocol></Subprotocol><Ordered></Ordered><MaxRetr></MaxRetr><MaxTime></MaxTime><Priority></Priority><AutoAcceptDcSetup></AutoAcceptDcSetup><Bandwidth></Bandwidth><QosHint></QosHint></DataChannel></DataChannelApp></DataChannelAppInfo>"
        val result = onADCParamsOk.onCreateADCParams(
            CommonConstants.DC_APPID_ROOT,
            labels.toTypedArray(),
            description
        )
        sLogger.info("MiniAppRootADCImpl createDC appId:${CommonConstants.DC_APPID_ROOT} ,result:$result ,description:$description")
    }

    fun release(){
        stopSendQueue()
        mAdc?.unregisterObserver()
        moduleListenerMap.clear()
        job?.cancel()
        job = null
    }

    fun sendData(module: Module, event: String, originData:ByteArray, onSendCallback:OnSendCallback){
        mDataQueue.add(QueueData(module, event, originData, onSendCallback))
    }

    private fun stopSendQueue(){
        mDataQueue.clear()
    }
    private fun startSendQueue() {
        job = scope.launch {
            while (isActive) {

                if (!sendResultOK) {
                    delay(100)
                    continue
                }
                if (mAdc?.state != ImsDCStatus.DC_STATE_OPEN){
                    delay(1000)
                    continue
                }
                val queueData = mDataQueue.poll()
                if (queueData == null) {
                    delay(100)
                    continue
                }
                val ownDataJson = RootDataMsg(queueData.module.value, queueData.event, ROOT_DC_LABEL, FileUtils.byteArrayToBase64(queueData.originData))
                val data = JsonUtil.toJson(ownDataJson).toByteArray()
                mAdc?.send(data,data.size,object : IDCSendDataCallback.Stub() {
                    override fun onSendDataResult(state: Int) {
                        sLogger.info("MiniAppRootADCImpl onSendDataResult:${state}, data: ${String(data)}")
                        queueData.onSendCallback.onSendDataResult(state)
                        sendResultOK = state == CommonConstants.DC_SEND_DATA_OK
                    }
                })
                sendResultOK = false
            }
        }
    }

    fun registerListener(module: Module, listener: OnADCListener){
        moduleListenerMap[module.value] = listener
    }

    fun unRegisterListener(module: Module){
        moduleListenerMap.remove(module.value)
    }
}




