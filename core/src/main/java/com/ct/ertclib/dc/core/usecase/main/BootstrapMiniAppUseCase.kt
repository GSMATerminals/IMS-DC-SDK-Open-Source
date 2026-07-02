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

package com.ct.ertclib.dc.core.usecase.main

import android.util.Base64
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.port.usecase.main.IBootstrapMiniAppUseCase
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.newcalllib.datachannel.V1_0.IImsDCObserver
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 用于引导小程序（Bootstrap Mini-app）的数据通道控制实现
 */
class BootstrapMiniAppUseCase : IBootstrapMiniAppUseCase {
    companion object {
        private const val TAG = "BootstrapMiniAppUseCase"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val serialDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val bootstrapDCList = ConcurrentHashMap<String, IImsDataChannel>()

    override fun onDataChannelCreated(imsDataChannel: IImsDataChannel) {
        val dcLabel = imsDataChannel.dcLabel
        sLogger.info("onDataChannelCreated label: $dcLabel")
        bootstrapDCList[dcLabel] = imsDataChannel
        imsDataChannel.registerObserver(ImsDCObserverImpl(dcLabel))
        dataChannelNotify(dcLabel, imsDataChannel.state)
    }

    override fun containsDCLabel(label: String): Boolean {
        return bootstrapDCList.containsKey(label)
    }

    override fun getBootstrapDC(label: String): IImsDataChannel? {
        return bootstrapDCList[label]
    }

    override fun removeBootstrapDC(label: String) {
        bootstrapDCList.remove(label)
    }

    private fun dataChannelNotify(label: String, status: ImsDCStatus?) {
        if (status == null) return
        scope.launch(serialDispatcher) {
            sLogger.info("$TAG dataChannelNotify label:$label, status:${status.name}")
            NewCallAppSdkInterface.emitBootstrapDataChannelState(label, status.ordinal)
        }
    }

    inner class ImsDCObserverImpl(
        private val label: String,
    ) : IImsDCObserver.Stub() {

        override fun onDataChannelStateChange(status: ImsDCStatus?, errCode: Int) {
            sLogger.debug("$TAG onDataChannelStateChange status:$status, label:$label")
            dataChannelNotify(label, status)
        }

        override fun onMessage(data: ByteArray?, length: Int) {
            if (data == null) return
            scope.launch(serialDispatcher) {
                val messageBase64 = Base64.encodeToString(data, Base64.NO_WRAP)
                sLogger.info("$TAG onMessage label:$label, message length:$length")
                NewCallAppSdkInterface.emitBootstrapMessage(label, messageBase64)
            }
        }
    }

    override fun release() {
        sLogger.info("release")
        bootstrapDCList.forEach { (label, dc) ->
            sLogger.debug("unregisterObserver for label: $label")
            dc.unregisterObserver()
        }
        bootstrapDCList.clear()
    }
}
