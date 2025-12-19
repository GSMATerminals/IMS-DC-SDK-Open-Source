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
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_ADD_DRAWING_INFO
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_ADD_REMOTE_SIZE_INFO
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_ADD_REMOTE_WINDOW_SIZE_INFO
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_CLOSE_SKETCH_BOARD
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_OPEN_SKETCH_BOARD
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REQUEST_SCREEN_SHARE_ABILITY
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_SET_SCREEN_SHARE_PRIVACY_MODE
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_START_SCREEN_SHARE
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_STOP_SCREEN_SHARE
import com.ct.ertclib.dc.core.constants.CommonConstants.SCREEN_SHARE_APP_EVENT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.REQUEST_ABILITY_PARAMS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.START_SHARE_PARAMS
import com.ct.ertclib.dc.core.data.bridge.JSResponse
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.manager.IMiniToParentManager
import com.ct.ertclib.dc.core.port.usecase.mini.IScreenShareMiniUseCase
import wendu.dsbridge.CompletionHandler

class ScreenShareMiniUseCase(private val miniToParentManager: IMiniToParentManager) :
    IScreenShareMiniUseCase {

    companion object {
        private const val TAG = "ScreenShareMiniUseCase"
    }

    private val logger = Logger.getLogger(TAG)

    override fun startScreenShare(context: Context, params: Map<String, Any>,handler: CompletionHandler<String?>) {
        val appRequestJson = AppRequest(SCREEN_SHARE_APP_EVENT, ACTION_START_SCREEN_SHARE, params).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, object : IMessageCallback.Stub() {
            override fun reply(message: String?) {
                message?.let {
                    logger.info("startScreenShare reply: $message")
                    val appResponse = JsonUtil.fromJson(message, AppResponse::class.java)
                    val map = appResponse?.data as? Map<*, *>
                    val response = map?.let {
                        JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, mutableMapOf(START_SHARE_PARAMS to map[START_SHARE_PARAMS]))
                    } ?: run {
                        JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)
                    }
                    handler.complete(JsonUtil.toJson(response))
                }
            }
        })
    }

    override fun stopScreenShare(context: Context): String {
        val appRequestJson = AppRequest(SCREEN_SHARE_APP_EVENT, ACTION_STOP_SCREEN_SHARE, mapOf()).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, null)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun requestScreenShareAbility(handler: CompletionHandler<String?>) {
        val appRequestJson = AppRequest(SCREEN_SHARE_APP_EVENT, ACTION_REQUEST_SCREEN_SHARE_ABILITY, mapOf()).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, object : IMessageCallback.Stub() {
            override fun reply(message: String?) {
                message?.let {
                    logger.info("requestScreenShareAbility reply: $message")
                    val appResponse = JsonUtil.fromJson(message, AppResponse::class.java)
                    val map = appResponse?.data as? Map<*, *>
                    val response = map?.let {
                        JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, mutableMapOf(REQUEST_ABILITY_PARAMS to map[REQUEST_ABILITY_PARAMS]))
                    } ?: run {
                        JSResponse(RESPONSE_FAILED_CODE, RESPONSE_FAILED_MESSAGE, null)
                    }
                    handler.complete(JsonUtil.toJson(response))
                }
            }
        })
    }

    override fun openSketchBoard(params: Map<String, Any>): String {
        val appRequestJson = AppRequest(SCREEN_SHARE_APP_EVENT, ACTION_OPEN_SKETCH_BOARD, params).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, null)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun closeSketchBoard(): String {
        val appRequestJson = AppRequest(SCREEN_SHARE_APP_EVENT, ACTION_CLOSE_SKETCH_BOARD, mapOf()).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, null)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun addDrawingInfo(params: Map<String, Any>): String {
        val appRequestJson = AppRequest(SCREEN_SHARE_APP_EVENT, ACTION_ADD_DRAWING_INFO, params).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, null)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun addRemoteSizeInfo(params: Map<String, Any>): String {
        val appRequestJson = AppRequest(SCREEN_SHARE_APP_EVENT, ACTION_ADD_REMOTE_SIZE_INFO, params).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, null)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun setPrivacyMode(params: Map<String, Any>): String {
        val appRequestJson = AppRequest(SCREEN_SHARE_APP_EVENT, ACTION_SET_SCREEN_SHARE_PRIVACY_MODE, params).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, null)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun addRemoteWindowSizeInfo(params: Map<String, Any>): String {
        val appRequestJson = AppRequest(SCREEN_SHARE_APP_EVENT, ACTION_ADD_REMOTE_WINDOW_SIZE_INFO, params).toJson()
        miniToParentManager.sendMessageToParent(appRequestJson, null)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }
}