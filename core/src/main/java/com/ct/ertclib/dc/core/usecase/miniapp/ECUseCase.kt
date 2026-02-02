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

package com.ct.ertclib.dc.core.usecase.miniapp

import android.content.Context
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.MiniAppConstants.IS_PEER_SUPPORT_DC_PARAMS
import com.ct.ertclib.dc.core.data.bridge.JSResponse
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.manager.IMiniToParentManager
import com.ct.ertclib.dc.core.port.usecase.mini.IECUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wendu.dsbridge.CompletionHandler
import kotlin.collections.get

class ECUseCase(private val miniToParentManager: IMiniToParentManager) :
    IECUseCase {

    companion object {
        private const val TAG = "ECUseCase"
    }

    private val logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun queryEC(
        context: Context,
        handler: CompletionHandler<String?>
    ) {
        val request = AppRequest(
            CommonConstants.EC_EVENT,
            CommonConstants.ACTION_QUERY_EC,
            mapOf()
        )
        scope.launch {
            withContext(Dispatchers.IO) {
                miniToParentManager.sendMessageToParent(request.toJson(), object :  IMessageCallback.Stub(){
                    override fun reply(message: String?) {
                        try {
                            if (message != null) {
                                logger.info("queryEC reply${message}")
                                val appResponse = JsonUtil.fromJson(message, AppResponse::class.java)
                                val map = appResponse?.data as? Map<*, *>
                                map?.let {
                                    val response = JSResponse("0", "success", map)
                                    scope.launch(Dispatchers.Main) {
                                        handler.complete(JsonUtil.toJson(response))
                                    }
                                }
                            }
                        } catch (e:Exception){
                            e.printStackTrace()
                        }
                    }

                })
            }
        }
    }

    override fun registerAsync(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(register(context, params))
    }

    override fun register(
        context: Context,
        params: Map<String, Any>
    ) : String {
        val request = AppRequest(
            CommonConstants.EC_EVENT,
            CommonConstants.ACTION_REGISTER_EC,
            params
        )
        scope.launch {
            withContext(Dispatchers.IO) {
                miniToParentManager.sendMessageToParent(request.toJson(), null)
            }
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }

    override fun requestAsync(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        handler.complete(request(context, params))
    }

    override fun request(
        context: Context,
        params: Map<String, Any>
    ) : String {
        // params {"provider":"","module":"","func":"","data":T}
        val request = AppRequest(
            CommonConstants.EC_EVENT,
            CommonConstants.ACTION_REQUEST_EC,
            params
        )
        scope.launch {
            withContext(Dispatchers.IO) {
                miniToParentManager.sendMessageToParent(request.toJson(), null)
            }
        }
        val response = JSResponse("0", "success", "")
        return JsonUtil.toJson(response)
    }


}