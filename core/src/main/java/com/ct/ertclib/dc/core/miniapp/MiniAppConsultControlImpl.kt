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
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppConsultControlMsg
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback
import com.newcalllib.datachannel.V1_0.IImsDCObserver
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus

/**
 * 用于SDK与对端（SDK或AS）协商。目前用于同步小程序信息、拉起对端小程序等业务逻辑。借用小程序id
 * 由业务发起方打开小程序时发起协商ADC的建立（AS是a2p或SDK是p2p），SDK接收拦截这个协商ADC。SDK发起local_xxx_0_miniappcontrollll，拦截xxx_xxx_x_miniappcontrollll
 * 定义控制信令
 * 接口回调动作
 */

class MiniAppConsultControlImpl(private val appId: String,private val onOnControlListener:OnControlListener) {
    companion object {
        const val CMD_REQUEST_START_APP = "requestStartApp"
        const val CMD_RESPONSE_START_APP = "responseStartApp"

        const val START_APP_OPTION_AGREE = "1"
        const val START_APP_OPTION_REJECT = "2"
        private const val TAG = "MiniAppConsultControlImpl"
    }
    private val sLogger: Logger = Logger.getLogger(TAG)
    private var mDC :IImsDataChannel ?= null

    fun onDCCreated(imsDataChannel: IImsDataChannel) {
        sLogger.info("miniAppConsultControlImpl onDCCreated dcLabel:${imsDataChannel.dcLabel}")
        mDC = imsDataChannel
        onOnControlListener.onControlDCStateChange(mDC!!.state,0)
        imsDataChannel.registerObserver(object : IImsDCObserver.Stub() {
            override fun onDataChannelStateChange(status: ImsDCStatus?, errCode: Int) {
                sLogger.info("miniAppConsultControlImpl onDataChannelStateChange:${status},dcLabel:${imsDataChannel.dcLabel}")
                onOnControlListener.onControlDCStateChange(status,errCode)
            }

            override fun onMessage(data: ByteArray?, length: Int) {
                try {
                    data?.let {
                        val msg = String(data)
                        sLogger.info("miniAppConsultControlImpl onMessage:${msg}")
                        msg.let {
                            val request = JsonUtil.fromJson(it, MiniAppConsultControlMsg::class.java)
                            if (request != null) {
                                when(request.cmd){
                                    CMD_REQUEST_START_APP -> {
                                        val appInfoStr = request.params["appInfo"] as String
                                        val appInfo = JsonUtil.fromJson(appInfoStr, MiniAppInfo::class.java)
                                        if (appInfo != null) {
                                            onOnControlListener.onRequestStartApp(appInfo)
                                        }
                                    }
                                    CMD_RESPONSE_START_APP -> {
                                        val option = request.params["option"] as String
                                        onOnControlListener.onResponseStartApp(option)
                                    }
                                }
                            }
                        }
                    }
                } catch (e:Exception){
                    e.printStackTrace()
                }
            }
        })
    }

    fun createDC(miniAppInfo:MiniAppInfo) {
        val labels = mutableListOf<String>()
        val label = "local_${appId}_1_${CommonConstants.DC_LABEL_CONTROL}"
        labels.add(label)
        var firstQos = ""
        if (miniAppInfo.qosHint.split(",").isNotEmpty()){
            firstQos = miniAppInfo.qosHint.split(",")[0]
        }
        val description =
            "<DataChannelAppInfo><DataChannelApp appId=\"${appId}\"><DataChannel dcId=\"${CommonConstants.DC_LABEL_CONTROL}\"><StreamId></StreamId><DcLabel>${label}</DcLabel><UseCase>1</UseCase><Subprotocol></Subprotocol><Ordered></Ordered><MaxRetr></MaxRetr><MaxTime></MaxTime><Priority></Priority><AutoAcceptDcSetup></AutoAcceptDcSetup><Bandwidth></Bandwidth><QosHint>${firstQos}</QosHint></DataChannel></DataChannelApp></DataChannelAppInfo>"
        val result = onOnControlListener.onCreateADCParams(
            appId,
            labels.toTypedArray(),
            description
        )
        sLogger.info("MiniAppConsultControlImpl createDC appId:$appId ,result:$result,label:$label ,description:$description")
    }

    fun release(){
        mDC?.unregisterObserver()
    }

    fun requestStartAdverseApp(appInfo: MiniAppInfo) {
        val appInfoStr = JsonUtil.toJson(appInfo)
        val msg = MiniAppConsultControlMsg(CMD_REQUEST_START_APP, mapOf("appInfo" to appInfoStr))
        val byteArray = JsonUtil.toJson(msg).toByteArray()
        mDC?.let {
            if (it.state == ImsDCStatus.DC_STATE_OPEN) {
                it.send(byteArray, byteArray.size, object : IDCSendDataCallback.Stub() {
                    override fun onSendDataResult(state: Int) {
                        sLogger.info("miniAppConsultControlImpl requestStartApp onSendDataResult:${state}")
                    }
                })
            }
        }
    }

    fun responseStartAppResult(option: String){
        val msg = MiniAppConsultControlMsg(CMD_RESPONSE_START_APP, mapOf("option" to option))
        val byteArray = JsonUtil.toJson(msg).toByteArray()
        mDC?.let {
            if (it.state == ImsDCStatus.DC_STATE_OPEN) {
                it.send(byteArray, byteArray.size, object : IDCSendDataCallback.Stub() {
                    override fun onSendDataResult(state: Int) {
                        sLogger.info("miniAppConsultControlImpl responseStartAppResult onSendDataResult:${state}")
                    }
                })
            }
        }
    }

    interface OnControlListener{
        fun onCreateADCParams(appId: String, toTypedArray: Array<String>, description: String): Int
        fun onControlDCStateChange(status: ImsDCStatus?, errCode: Int)
        fun onRequestStartApp(appInfo: MiniAppInfo)
        fun onResponseStartApp(option: String)
    }


}
