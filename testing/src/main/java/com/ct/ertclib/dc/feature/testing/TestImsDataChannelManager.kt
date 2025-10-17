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

package com.ct.ertclib.dc.feature.testing

import android.annotation.SuppressLint
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.newcalllib.datachannel.V1_0.IImsDataChannelCallback
import com.newcalllib.datachannel.V1_0.IImsDataChannelServiceController
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import java.util.HashMap

@SuppressLint("StaticFieldLeak")
object TestImsDataChannelManager {
    private val TAG = "TestImsDataChannelManager"
    private val sLogger = Logger.getLogger(TAG)
    val mDCMaps: HashMap<String, TestImsDataChannelImpl> = HashMap()
    var mSoltId: Int? = null
    var mCallId: String? = null
    var mCallback: IImsDataChannelCallback? = null
    val mDcController: TestServiceController = TestServiceController()

    @SuppressLint("StaticFieldLeak")
    private fun openBdc(soltId: Int, callId: String) {
        if (mDCMaps["bdc"] != null) {
            sLogger.info("bdc is already open, need close first")
            return
        }
        mSoltId = soltId
        mCallId = callId

        var imsDataChannelImpl =
            TestImsDataChannelImpl()
        imsDataChannelImpl.setDcTyp(TestImsDataChannelImpl.DC_TYPE_BDC)
        imsDataChannelImpl.setSlotId(soltId)
        imsDataChannelImpl.setTelecomCallId(callId)
        imsDataChannelImpl.telephonyNumber = ""
        imsDataChannelImpl.setDcLabel("bdc")
        imsDataChannelImpl.setStreamId("0")
        imsDataChannelImpl.setDcStatus(ImsDCStatus.DC_STATE_OPEN)
        mDCMaps["bdc"] = imsDataChannelImpl
        notifyBDCResponse(mCallback)
    }

    private fun notifyBDCResponse(callback: IImsDataChannelCallback?) {
        if (mDCMaps["bdc"] == null) {
            sLogger.info("notifyBDCResponse BDC IS null")
            return
        }
        sLogger.info("callback:$callback")
        try {
            callback?.onBootstrapDataChannelResponse(mDCMaps["bdc"])
        } catch (e: Exception) {
            sLogger.error("notifyBDCResponse error", e)
        }
    }

    fun closeBdc(soltId: Int, callId: String) {
        val imsDataChannelImpl = mDCMaps["bdc"]
        if (imsDataChannelImpl == null) {
            sLogger.info("close bdc is null")
        } else if (imsDataChannelImpl.isClosed()) {
            sLogger.info("close bdc is already closed")
        } else {
            if (soltId == imsDataChannelImpl.slotId && callId == imsDataChannelImpl.telecomCallId) {
                imsDataChannelImpl.setDcStatus(ImsDCStatus.DC_STATE_CLOSED)
                mDCMaps.clear()
            }
        }

    }

    fun onUnbind() {
        mDCMaps.clear()
        mCallback = null
    }

    fun close(tag: String) {
        mDCMaps.remove(tag)
    }

    class TestServiceController : IImsDataChannelServiceController.Stub() {
        override fun createImsDataChannel(
            dcIds: Array<out String>?,
            appInfoXml: String?,
            slotId: Int,
            callId: String?,
            phoneNumber: String?
        ) {

            val list = ArrayList<String>()
            dcIds?.forEach {
                if (mDCMaps[it] == null) {
                    list.add(it)
                }
            }
            notifyADCResponse(dcIds, mCallback, ImsDCStatus.DC_STATE_CONNECTING)
        }

        override fun setImsDataChannelCallback(
            l: IImsDataChannelCallback?,
            slotId: Int,
            callId: String?
        ) {
            sLogger.info("setImsDataChannelCallback l: $l, slotId: $slotId, callId: $callId")
            if (l != null) {
                mCallback = l
                openBdc(slotId, callId!!)
            } else {
                sLogger.info("setImsDataChannelCallback l is null")
            }
        }

        override fun setModemCallId(slotId: Int, modemCallId: Int, telecomCallId: String?) {
            sLogger.info("setModemCallId slotId:$slotId, modemCallId:$modemCallId, telecomCallId:$telecomCallId")
        }

    }

    private fun sendData(data: TestData) {
        //通过socket发送数据
    }

    private fun notifyADCResponse(
        dcIds: Array<out String>?,
        callback: IImsDataChannelCallback?,
        dcStateConnecting: ImsDCStatus
    ) {
        sLogger.info("notifyADCResponse dcIds:$dcIds")
        if (dcIds == null) {
            return
        }
        dcIds.forEach {
            var imsDataChannelImpl = mDCMaps[it]
            if (imsDataChannelImpl == null) {
                sLogger.info("notifyADCResponse imsDataChannelImpl is null, create it")
                imsDataChannelImpl =
                    TestImsDataChannelImpl()
                imsDataChannelImpl.setDcTyp(TestImsDataChannelImpl.DC_TYPE_ADC)
                imsDataChannelImpl.setSlotId(mSoltId!!)
                imsDataChannelImpl.setTelecomCallId(mCallId!!)
                imsDataChannelImpl.setDcLabel(it)
                imsDataChannelImpl.setStreamId((1000+(mDCMaps.size)*2).toString())
                imsDataChannelImpl.setDcStatus(dcStateConnecting)
                mDCMaps[it] = imsDataChannelImpl
            } else if (ImsDCStatus.DC_STATE_CONNECTING != dcStateConnecting) {
                imsDataChannelImpl.setDcStatus(dcStateConnecting)
            }
            if (callback != null) {
                try {
                    callback.onApplicationDataChannelResponse(imsDataChannelImpl)
                } catch (e: Exception) {
                    sLogger.error("notifyADCResponse error", e)
                }
            } else {
                sLogger.info("notifyADCResponse callback is null")
            }
        }
    }
}