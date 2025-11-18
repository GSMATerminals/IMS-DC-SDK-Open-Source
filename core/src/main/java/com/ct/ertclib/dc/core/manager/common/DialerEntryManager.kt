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

package com.ct.ertclib.dc.core.manager.common

import android.content.Context
import android.content.Intent
import android.graphics.Point
import com.ct.ertclib.dc.core.constants.CommonConstants.FLOATING_DISPLAY
import com.ct.ertclib.dc.core.constants.ContextConstants.INTENT_MINI_EXPANDED
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.common.ECBaseData
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.manager.common.StateFlowManager.dialerEntryStatusFlow
import com.ct.ertclib.dc.core.port.expandcapacity.IExpandingCapacityListener
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class DialerEntryManager {
    companion object {
        private const val TAG = "DialerEntryManager"
        val instance: DialerEntryManager by lazy {
            DialerEntryManager()
        }
    }
    private val sLogger: Logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job?= null


    fun init(context: Context){
        var enable = false
        var miniAppList:MiniAppList ?= null
        var callInfo:CallInfo ?= null
        job = scope.launch(Dispatchers.Main) {
            dialerEntryStatusFlow.distinctUntilChanged().collect { floatingBallData ->
                sLogger.info("floatingBallStatusFlow status: ${floatingBallData.showStatus}")
                floatingBallData.miniAppList?.let {
                    enable = floatingBallData.showStatus == FLOATING_DISPLAY
                    notifyEnable(context,enable)
                    miniAppList = floatingBallData.miniAppList
                    callInfo = floatingBallData.callInfo
                }
            }
        }
        val modules = ArrayList<String>()
        modules.add("NewCallSDK")
        val providerModules = ConcurrentHashMap<String, ArrayList<String>>()
        providerModules[ExpandingCapacityManager.OEM] = modules
        ExpandingCapacityManager.instance.registerECListener(
            "DialerEntryManager",
            "DialerEntryManager",
            providerModules,
            object : IExpandingCapacityListener {
                override fun onCallback(content: String?) {
                    sLogger.debug("DialerEntryManager onCallback content: $content")
                    try {
                        if (content != null) {
                            val ecResponse = JsonUtil.fromJson(content, ECBaseData::class.java)
                            when (ecResponse?.func) {
                                "queryExpandEnable" -> {
                                    notifyEnable(context, enable)
                                }

                                "expand" -> {
                                    if (callInfo?.isRinging() != true && callInfo?.isInCall() != true){
                                        sLogger.info("DialerEntryManager expand callInfo is not ringing or inCall")
                                        return
                                    }
                                    val intent = Intent(INTENT_MINI_EXPANDED)
                                    intent.putExtra("miniAppList", miniAppList)
                                    intent.putExtra("callInfo", callInfo)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    val point = Point(0, ScreenUtils.getScreenHeight(context) / 2)
//                                        floatLps?.let {
//                                            point.x = it.x
//                                            point.y = it.y
//                                        }
                                    intent.putExtra("point", point)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
    }

    fun release(context: Context){
        job?.cancel()
        ExpandingCapacityManager.instance.unregisterECListener(
            context,
            "DialerEntryManager",
            "DialerEntryManager"
        )
    }

    private fun notifyEnable(context: Context,enable:Boolean){
        ExpandingCapacityManager.instance.request(
            context,
            "DialerEntryManager",
            "DialerEntryManager",
            "{\"provider\":\"OEM\",\"module\":\"NewCallSDK\",\"func\":\"setExpandEnable\",\"data\":{\"isEnable\":$enable}}"
        )
    }
}