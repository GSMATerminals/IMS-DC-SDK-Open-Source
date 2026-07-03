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

package com.ct.ertclib.dc.feature.testing.netdc.service

import android.annotation.SuppressLint
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.net.data.ADCState
import com.ct.ertclib.dc.net.websocket.InterfaceHandler
import com.newcalllib.datachannel.V1_0.IImsDataChannelCallback
import com.newcalllib.datachannel.V1_0.IImsDataChannelServiceController
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("StaticFieldLeak")
object NetImsDataChannelManager {
    private val TAG = "NetImsDataChannelManager"
    private val sLogger = Logger.getLogger(TAG)
    val mDCMaps: ConcurrentHashMap<String,ConcurrentHashMap<String, NetImsDataChannelImpl>> = ConcurrentHashMap()
    var mCallbackMap: ConcurrentHashMap<String,IImsDataChannelCallback> = ConcurrentHashMap()
    val mDcController: NetServiceController = NetServiceController()

    fun onBind(){
        InterfaceHandler.setDCCreateCallback(object : InterfaceHandler.DCCreateCallback {
            override fun onNotifyADC(callId: String, adcList: ArrayList<ADCState>) {
                sLogger.info("notifyADCResponse callId: $callId, adcList size: ${adcList.size}")

                mDCMaps[callId]?.let { channelMap ->
                    adcList.forEach { adcState ->
                        processADCState(callId, channelMap, adcState)
                    }
                } ?: sLogger.warn("notifyADCResponse channelMap is null for callId: $callId, no bdc")
            }

            private fun processADCState(
                callId: String,
                channelMap: ConcurrentHashMap<String, NetImsDataChannelImpl>,
                adcState: ADCState
            ) {
                channelMap[adcState.label]?.let { existingChannel ->
                    // 更新现有 channel
                    existingChannel.apply {
                        setDcStatus(mapToImsDCStatusSimple(adcState.state))
                    }
                    sLogger.debug("Updated ADC channel - label: ${adcState.label}, new state: ${adcState.state}")
                } ?: run {
                    // 创建新 channel
                    onCreateNewADCChannel(callId, channelMap, adcState)?.let { newChannel ->
                        mCallbackMap[callId]?.onApplicationDataChannelResponse(newChannel)
                    }
                }
            }

            private fun onCreateNewADCChannel(
                callId: String,
                channelMap: ConcurrentHashMap<String, NetImsDataChannelImpl>,
                adcState: ADCState
            ): NetImsDataChannelImpl? {
                val streamId = generateStreamId()

                return NetImsDataChannelImpl().apply {
                    setDcTyp(NetImsDataChannelImpl.DC_TYPE_ADC)
                    slotId = 0
                    telecomCallId = callId
                    dcLabel = adcState.label
                    this.streamId = streamId
                    setDcStatus(mapToImsDCStatusSimple(adcState.state))

                    channelMap[adcState.label] = this

                    sLogger.info("Created ADC channel - label: ${adcState.label}, streamId: $streamId, state: ${adcState.state}")

                }
            }

            private fun generateStreamId(): String {
                var count = 1
                mDCMaps.forEach{ map ->
                    map.value.forEach{ _ ->
                        count++
                    }
                }
                return (1000 + (count * 2)).toString()
            }
        })
    }

    fun onUnbind() {
        mDCMaps.clear()
        mCallbackMap.clear()
        InterfaceHandler.setDCCreateCallback(null)
        InterfaceHandler.clearDCDataCallback()
    }

    fun mapToImsDCStatusSimple(state: String): ImsDCStatus? {
        return when (state) {
            "DC_STATE_CONNECTING" -> ImsDCStatus.DC_STATE_CONNECTING
            "DC_STATE_OPEN" -> ImsDCStatus.DC_STATE_OPEN
            "DC_STATE_CLOSING" -> ImsDCStatus.DC_STATE_CLOSING
            "DC_STATE_CLOSED" -> ImsDCStatus.DC_STATE_CLOSED
            else -> null
        }
    }

    class NetServiceController : IImsDataChannelServiceController.Stub() {
        override fun createImsDataChannel(
            labels: Array<out String>?,
            appInfoXml: String?,
            slotId: Int,
            callId: String?,
            phoneNumber: String?
        ) {
            if (callId == null) return
            val list = ArrayList<String>()
            labels?.forEach {
                if (mDCMaps[callId]?.get(it) == null) {
                    list.add(it)
                }
            }
            // 通知平台建立ADC
            InterfaceHandler.createADC(callId,list)
        }

        companion object {
            private const val BDC_LABEL_0 = "bdc0"
            private const val BDC_LABEL_100 = "bdc100"
            private const val STREAM_ID_0 = "0"
            private const val STREAM_ID_100 = "100"
        }

        override fun setImsDataChannelCallback(
            l: IImsDataChannelCallback?,
            slotId: Int,
            callId: String?
        ) {
            sLogger.info("setImsDataChannelCallback l: $l, slotId: $slotId, callId: $callId")

            if (callId.isNullOrBlank()) {
                sLogger.warn("setImsDataChannelCallback callId is null or blank")
                return
            }

            if (l == null) {
                sLogger.info("setImsDataChannelCallback l is null, remove callId: $callId")
                mCallbackMap.remove(callId)
                return
            }

            // 原子性检查并插入
            val channelMap = mDCMaps.computeIfAbsent(callId) { ConcurrentHashMap() }

            // 检查 bdc0 是否已存在
            if (channelMap.containsKey(BDC_LABEL_0)) {
                sLogger.info("bdc is already open for callId: $callId, need close first")
                return
            }

            // 批量创建并添加 channel
            listOf(
                createBdcChannel(slotId, callId, BDC_LABEL_0, STREAM_ID_0),
                createBdcChannel(slotId, callId, BDC_LABEL_100, STREAM_ID_100)
            ).forEach { channel ->
                channelMap[channel.dcLabel] = channel
                try {
                    l.onBootstrapDataChannelResponse(channel)
                } catch (e: Exception) {
                    sLogger.error("notifyBDCResponse error", e)
                }
            }
            mCallbackMap[callId] = l
        }

        private fun createBdcChannel(
            slotId: Int,
            callId: String,
            dcLabel: String,
            streamId: String
        ): NetImsDataChannelImpl {
            return NetImsDataChannelImpl().apply {
                setDcTyp(NetImsDataChannelImpl.DC_TYPE_BDC)
                this.slotId = slotId
                this.telecomCallId = callId
                telephonyNumber = ""
                this.dcLabel = dcLabel
                this.streamId = streamId
                setDcStatus(ImsDCStatus.DC_STATE_OPEN)
            }
        }

        override fun setModemCallId(slotId: Int, modemCallId: Int, telecomCallId: String?) {
            sLogger.info("setModemCallId slotId:$slotId, modemCallId:$modemCallId, telecomCallId:$telecomCallId")
        }

    }
}