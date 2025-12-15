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

package com.ct.ertclib.dc.core.dispatcher.appservice


import android.content.Context
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_EC_CALLBACK
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_QUERY_EC
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REGISTER_EC
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REQUEST_EC
import com.ct.ertclib.dc.core.data.event.NotifyEvent
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.manager.common.ExpandingCapacityManager
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.common.IParentToMiniNotify
import com.ct.ertclib.dc.core.port.dispatcher.IAppServiceEventDispatcher
import com.ct.ertclib.dc.core.port.expandcapacity.IExpandingCapacityListener
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

class ECAppServiceDispatcher : IAppServiceEventDispatcher, KoinComponent {

    private val applicationContext: Context by inject()
    private val parentToMiniNotifier:IParentToMiniNotify by inject()

    override fun dispatchEvent(
        telecomCallId: String,
        appId: String,
        appRequest: AppRequest,
        iMessageCallback: IMessageCallback?
    ) {
        when (appRequest.actionName) {
            ACTION_QUERY_EC -> {
                val replayMessage = AppResponse(
                    CommonConstants.APP_RESPONSE_CODE_SUCCESS,
                    CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS,
                    ExpandingCapacityManager.instance.getProviderModulesMap()
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
                            parentToMiniNotifier.notifyEvent(telecomCallId,appId,event)
                        }
                    })
                }
            }
            ACTION_REQUEST_EC -> {
                val requestStr = JsonUtil.toJson(appRequest.map)
                ExpandingCapacityManager.instance.request(applicationContext,telecomCallId,appId,requestStr)
            }
        }
    }
}