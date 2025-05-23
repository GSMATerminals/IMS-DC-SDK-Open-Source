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

package com.ct.ertclib.dc.core.usecase.miniapp

import android.content.Context
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.DCUtils
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_IS_PEER_SUPPORT_DC
import com.ct.ertclib.dc.core.constants.CommonConstants.CALL_APP_EVENT
import com.ct.ertclib.dc.core.constants.CommonConstants.DC_SEND_DATA_OK
import com.ct.ertclib.dc.core.constants.MiniAppConstants.IS_PEER_SUPPORT_DC_PARAMS
import com.ct.ertclib.dc.core.data.bridge.JSResponse
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.manager.IMiniToParentManager
import com.ct.ertclib.dc.core.port.usecase.mini.IDCMiniEventUseCase
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wendu.dsbridge.CompletionHandler

class DCMiniUseCase(private val miniToParentManager: IMiniToParentManager) :
    IDCMiniEventUseCase {

    companion object {
        private const val TAG = "DCMiniUseCase"
    }

    private val logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun createAppDataChannel(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.info("createAppDataChannel")
        if (params["dcLabels"] == null || params["DataChannelAppInfoXml"] == null) {
            val response = JSResponse("1", "dcLabels or description is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.info("JSApi asyn dcLabels or description is null")
            return
        }
        val dcLabels = params["dcLabels"] as List<String>
        var description = params["DataChannelAppInfoXml"] as String
        dcLabels.forEach {
            // 校验小程序ID
            if (!miniToParentManager.getMiniAppInfo()?.let { it1 -> it.contains("_${it1.appId}_") }!!) {
                val response = JSResponse("1", "$it appId error", "")
                handler.complete(JsonUtil.toJson(response))
                logger.info("JSApi asyn dcLabel$it appId error")
                return
            }
            if (miniToParentManager.createDCLabelList?.contains(it) == true) {
                val response = JSResponse("1", "dcLabel:$it is already created", "")
                handler.complete(JsonUtil.toJson(response))
                logger.info("JSApi asyn dcLabel$it already created")
                return
            }
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                // 补全streamId
                if (!description.contains("StreamId")){
                    description = description.replace("</DcLabel>","</DcLabel><StreamId></StreamId>")
                }
                val result = miniToParentManager.createDC(dcLabels, description)
                logger.info("JSApi asyn ,dcIds:$result")
                val response = JSResponse("0", "${result?.toString()}", "")
                handler.complete(JsonUtil.toJson(response))
            }
        }
    }

    override fun sendData(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        val dcLabel = params["dcLabel"]
        val data = params["data"]
        if (dcLabel == null || data == null) {
            val response = JSResponse("1", "dcLabel or data is null", "")
            handler.complete(JsonUtil.toJson(response))
            logger.info("JSApi asyn dcLabel or data is null")
            return
        }
        val dataByteArray = FileUtils.base64ToByteArray(data as String)

        val dcLabelStr = dcLabel as String
        scope.launch(Dispatchers.IO) {
            logger.info("JSApi asyn ,sendData dcLabel:$dcLabelStr, data:$data")
            val dc = miniToParentManager.openDCList?.firstOrNull { DCUtils.compareDCLabel(it.dcLabel, dcLabelStr) }
            dc?.send(dataByteArray, dataByteArray.size, object : IDCSendDataCallback.Stub() {
                override fun onSendDataResult(state: Int) {
                    logger.debug("onSendDataResult state:$state")
                    val response = JSResponse(if(state == DC_SEND_DATA_OK) "0" else state.toString(), if(state == DC_SEND_DATA_OK) "success" else "fail", "")
                    handler.complete(JsonUtil.toJson(response))
                }
            })
        }
    }

    override fun closeAppDataChannel(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        val dcLabel = params["dcLabel"]
        if (dcLabel == null) {
            logger.info("JSApi asyn closeDC dcLabel is null")
            val response = JSResponse("1", "dcLabel is null", "")
            handler.complete(JsonUtil.toJson(response))
            return
        }
        val dcLabels = dcLabel as ArrayList<*>
        for (dcLabelItem in dcLabels){
            scope.launch {
                withContext(Dispatchers.IO) {
                    val dcLabelItemStr = dcLabelItem as String
                    miniToParentManager.closeDC(dcLabelItemStr)
                }
            }
        }
        val response = JSResponse("0", "success", "")
        handler.complete(JsonUtil.toJson(response))
    }

    override fun isPeerSupportDC(
        context: Context,
        params: Map<String, Any>,
        handler: CompletionHandler<String?>
    ) {
        logger.info("JSApi asyn ,isPeerSupportDC")
        val request = AppRequest(CALL_APP_EVENT, ACTION_IS_PEER_SUPPORT_DC, mapOf())
        scope.launch {
            withContext(Dispatchers.IO) {
                miniToParentManager.sendMessageToParent(request.toJson(),object : IMessageCallback.Stub(){
                    override fun reply(message: String?) {
                        try {
                            if (message != null) {
                                logger.info("isPeerSupportDC reply${message}")
                                val appResponse = JsonUtil.fromJson(message, AppResponse::class.java)
                                val map = appResponse?.data as? Map<*, *>
                                map?.let {
                                    val response = JSResponse("0", "success", mutableMapOf(IS_PEER_SUPPORT_DC_PARAMS to map[IS_PEER_SUPPORT_DC_PARAMS]))
                                    handler.complete(JsonUtil.toJson(response))
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

    override fun getBufferedAmount(
        context: Context,
        params: Map<String, Any>
    ) : String {
        val dcLabel = params["dcLabel"] as String?
        if (dcLabel == null) {
            logger.info("JSApi sync getBufferedAmount dcLabel is null")
            val response = JSResponse("1", "getBufferedAmount dcLabel is null", mutableMapOf<String, Long>())
            return JsonUtil.toJson(response)
        }
        val dc = miniToParentManager.openDCList?.firstOrNull { DCUtils.compareDCLabel(it.dcLabel, dcLabel) }
        val bufferedAmount = dc?.let {
            try {
                it.bufferedAmount()
            } catch (e: Exception) {
                logger.error("getBufferedAmount dcLabel:$dcLabel failed, error:${e.message}")
                -1
            }
        } ?: run {
            -1
        }
        logger.info("JSApi sync ,getBufferedAmount dcLabel:$dcLabel bufferedAmount:$bufferedAmount")
        if (bufferedAmount == -1L){
            val response = JSResponse("1", "getBufferedAmount fail", mutableMapOf<String, Long>())
            return JsonUtil.toJson(response)
        }
        val bufferedAmountMap = mutableMapOf<String, Long>()
        bufferedAmountMap["bufferedAmount"] = bufferedAmount
        val response = JSResponse("0", "success", bufferedAmountMap)
        return JsonUtil.toJson(response)
    }
}