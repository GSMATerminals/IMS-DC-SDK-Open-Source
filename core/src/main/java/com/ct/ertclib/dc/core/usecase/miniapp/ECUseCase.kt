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

package com.ct.ertclib.dc.core.usecase.miniapp

import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_EC_NOTIFY
import com.ct.ertclib.dc.core.data.bridge.JSResponse
import com.ct.ertclib.dc.core.manager.common.ExpandingCapacityManager
import com.ct.ertclib.dc.core.port.usecase.mini.IECUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView
import com.ct.ertclib.dc.core.port.expandcapacity.IExpandingCapacityListener
import java.util.concurrent.ConcurrentHashMap

class ECUseCase() : IECUseCase {

    companion object {
        private const val TAG = "ECUseCase"
    }

    private val logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun queryEC(
        miniAppView: MiniAppView,
        handler: CompletionHandler<String?>
    ) {
        val response = JSResponse("0", "success", ExpandingCapacityManager.instance.getProviderModulesMap())
        scope.launch(Dispatchers.Main) {
            handler.complete(JsonUtil.toJson(response))
        }
    }

    override fun registerAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(register(miniAppView, params))
    }

    override fun register(
        miniAppView: MiniAppView,
        params: Map<String, Any>
    ) : String {
        if (params["providerModules"] != null){
            val list = params["providerModules"] as ArrayList<String>
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
            miniAppView.viewModel.callInfo?.telecomCallId?.let {
                miniAppView.viewModel.miniAppInfo?.appId?.let { appId ->
                    ExpandingCapacityManager.instance.registerECListener(it,appId,providerModules,object :IExpandingCapacityListener{
                        override fun onCallback(content: String?) {
                            miniAppView.viewModel.callHandler(FUNCTION_EC_NOTIFY, arrayOf(content as String))
                        }
                    })
                }
            }
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun requestAsync(
        miniAppView: MiniAppView,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(request(miniAppView, params))
    }

    override fun request(
        miniAppView: MiniAppView,
        params: Map<String, Any>
    ) : String {
        // params {"provider":"","module":"","func":"","data":T}
        val requestStr = JsonUtil.toJson(params)
        miniAppView.viewModel.callInfo?.telecomCallId?.let { miniAppView.viewModel.miniAppInfo?.appId?.let { appId -> ExpandingCapacityManager.instance.request(miniAppView.context,it,appId,requestStr) } }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }


}