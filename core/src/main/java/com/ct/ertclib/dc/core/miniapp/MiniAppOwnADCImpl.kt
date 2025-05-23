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

package com.ct.ertclib.dc.core.miniapp

import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback
import com.newcalllib.datachannel.V1_0.IImsDCObserver
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * 用于SDK与SDK自己的AS通信
 * 接口回调动作
 */

class MiniAppOwnADCImpl(private val onADCParamsOk:OnADCParamsOk){
    data class OwnDataMsg(val model:String, val dataBase64:String)
    data class QueueData(val model:Model,val originData:ByteArray,val onSendCallback:OnSendCallback)

    interface OnADCListener{
        fun onMessage(data: ByteArray?, length: Int)
    }

    interface OnADCParamsOk{
        fun onCreateADCParams(appId: String, toTypedArray: Array<String>, description: String): Int
    }

    interface OnSendCallback{
        fun onSendDataResult(state: Int)
    }

    private val TAG = "MiniAppOwnADCImpl"
    private val sLogger: Logger = Logger.getLogger(TAG)
    private var mAdc :IImsDataChannel? = null
    private val mModelListenerMap = ConcurrentHashMap<String, OnADCListener>()

    private var mDataQueue: ArrayBlockingQueue<QueueData> = ArrayBlockingQueue(10000)
    private var canSend = false
    private var sendResultOK = true

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job:Job? = null

    fun onDCCreated(imsDataChannel: IImsDataChannel) {
        sLogger.info("MiniAppOwnADCImpl onDCCreated dcLabel:${imsDataChannel.dcLabel}")
        mAdc = imsDataChannel
        imsDataChannel.registerObserver(object : IImsDCObserver.Stub() {
            override fun onDataChannelStateChange(status: ImsDCStatus?, errCode: Int) {
                sLogger.info("MiniAppOwnADCImpl onDataChannelStateChange:${status},dcLabel:${imsDataChannel.dcLabel}")
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
                        sLogger.info("MiniAppOwnADCImpl onMessage:${msg}")
                        msg.let {
                            val ownDataMsg = JsonUtil.fromJson(it, OwnDataMsg::class.java)
                            if (ownDataMsg != null) {
                                val bs = FileUtils.base64ToByteArray(ownDataMsg.dataBase64)
                                mModelListenerMap[ownDataMsg.model]?.onMessage(bs,bs.size)
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
        val label = "local_${CommonConstants.DC_APPID_OWN}_1_${CommonConstants.DC_LABEL_OWN}"
        labels.add(label)
        val description =
            "<DataChannelAppInfo><DataChannelApp appId=\"${CommonConstants.DC_APPID_OWN}\"><DataChannel dcId=\"${CommonConstants.DC_LABEL_OWN}\"><StreamId></StreamId><DcLabel>${label}</DcLabel><UseCase>0</UseCase><Subprotocol></Subprotocol><Ordered></Ordered><MaxRetr></MaxRetr><MaxTime></MaxTime><Priority></Priority><AutoAcceptDcSetup></AutoAcceptDcSetup><Bandwidth></Bandwidth><QosHint></QosHint></DataChannel></DataChannelApp></DataChannelAppInfo>"
        val result = onADCParamsOk.onCreateADCParams(
            CommonConstants.DC_APPID_OWN,
            labels.toTypedArray(),
            description
        )
        sLogger.info("MiniAppOwnADCImpl createDC appId:${CommonConstants.DC_APPID_OWN} ,result:$result,label:$label ,description:$description")
    }

    fun release(){
        stopSendQueue()
        mAdc?.unregisterObserver()
        mModelListenerMap.clear()
        job?.cancel()
        job = null
    }

    fun sendData(model:Model,originData:ByteArray,onSendCallback:OnSendCallback){
        if (!canSend){
            return
        }
        mDataQueue.add(QueueData(model, originData, onSendCallback))
    }

    private fun stopSendQueue(){
        canSend = false
        mDataQueue.clear()
    }
    private fun startSendQueue(){
        canSend = true
        job = scope.launch {
            while (isActive && canSend) {

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
                val ownDataJson = OwnDataMsg(queueData.model.value,FileUtils.byteArrayToBase64(queueData.originData))
                val data = JsonUtil.toJson(ownDataJson).toByteArray()
                mAdc?.send(data,data.size,object : IDCSendDataCallback.Stub() {
                    override fun onSendDataResult(state: Int) {
                        sLogger.info("MiniAppOwnADCImpl onSendDataResult:${state}")
                        queueData.onSendCallback.onSendDataResult(state)
                        sendResultOK = state == CommonConstants.DC_SEND_DATA_OK
                    }
                })
                sendResultOK = false
            }
        }
    }

    fun registerListener(model:Model,listener:OnADCListener){
        mModelListenerMap[model.value] = listener
    }

    fun unRegisterListener(model:Model){
        mModelListenerMap.remove(model.value)
    }

    enum class Model(val value: String) {
        VPN("vpn")
    }
}




