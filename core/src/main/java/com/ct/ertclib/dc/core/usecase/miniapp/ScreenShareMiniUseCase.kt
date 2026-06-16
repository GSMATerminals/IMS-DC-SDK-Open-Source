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

import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_COLOR_PARAMS
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_DRAWING_INFO_PARAMS
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_IS_ENABLE
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_LICENSE_PARAM
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_REMOTE_HEIGHT_PARAM
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_REMOTE_WIDTH_PARAM
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_WIDTH_PARAMS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.REQUEST_ABILITY_PARAMS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_FAILED_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.RESPONSE_SUCCESS_MESSAGE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.START_SHARE_PARAMS
import com.ct.ertclib.dc.core.data.bridge.JSResponse
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.port.common.IMessageCallback
import com.ct.ertclib.dc.core.port.usecase.mini.IScreenShareMiniUseCase
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView
import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.port.usecase.main.ISketchBoardUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class ScreenShareMiniUseCase() : IScreenShareMiniUseCase,KoinComponent {

    companion object {
        private const val TAG = "ScreenShareMiniUseCase"
    }

    private val logger = Logger.getLogger(TAG)

    private val screenShareUseCase: IScreenShareUseCase by inject()
    private val sketchBoardUseCase: ISketchBoardUseCase by inject()

    override fun startScreenShare(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>) {
        val license = params[APP_LICENSE_PARAM]
        miniAppView.viewModel.miniAppInfo?.appId?.let {
            screenShareUseCase.startScreenShare(it, license.toString(), object : IMessageCallback {
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
    }

    override fun stopScreenShareAsync(miniAppView: MiniAppView, handler: CompletionHandler<String?>) {
        handler.complete(stopScreenShare(miniAppView))
    }

    override fun stopScreenShare(miniAppView: MiniAppView): String {
        screenShareUseCase.stopScreenShare(needNotifyToMini = false)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun requestScreenShareAbility(miniAppView: MiniAppView,handler: CompletionHandler<String?>) {
        screenShareUseCase.requestScreenShareAbility(object : IMessageCallback{
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

    override fun openSketchBoardAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>) {
        handler.complete(openSketchBoard(miniAppView,params))
    }

    override fun openSketchBoard(miniAppView: MiniAppView,params: Map<String, Any>): String {
        val paintColor = params[APP_COLOR_PARAMS].toString()
        val paintWidth = params[APP_WIDTH_PARAMS].toString().toFloat()
        miniAppView.viewModel.callInfo?.telecomCallId?.let { miniAppView.viewModel.miniAppInfo?.appId?.let { appId -> sketchBoardUseCase.openSketchBoard(it, appId, paintColor, paintWidth) } }
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun closeSketchBoardAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>) {
        handler.complete(closeSketchBoard(miniAppView))
    }

    override fun closeSketchBoard(miniAppView: MiniAppView,): String {
        sketchBoardUseCase.closeSketchBoard(needNotifyToMini = false)
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun addDrawingInfoAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>
    ) {
        handler.complete(addDrawingInfo(miniAppView,params))
    }
    override fun addDrawingInfo(miniAppView: MiniAppView,params: Map<String, Any>): String {
        val drawingInfo = params[APP_DRAWING_INFO_PARAMS]
        drawingInfo?.let {
            sketchBoardUseCase.addDrawingInfo(JsonUtil.toJson(it))
        }
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun addRemoteSizeInfoAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>
    ) {
        handler.complete(addRemoteSizeInfo(miniAppView,params))
    }

    override fun addRemoteSizeInfo(miniAppView: MiniAppView,params: Map<String, Any>): String {
        kotlin.runCatching {
            val width = params[APP_REMOTE_WIDTH_PARAM].toString()
            val height = params[APP_REMOTE_HEIGHT_PARAM] .toString()
            sketchBoardUseCase.addRemoteSizeInfo(width.toFloat().toInt(), height.toFloat().toInt())
        }.onFailure {
            logger.error("remote size convert wrong")
        }
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun setPrivacyModeAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>
    ) {
        handler.complete(setPrivacyMode(miniAppView,params))
    }

    override fun setPrivacyMode(miniAppView: MiniAppView,params: Map<String, Any>): String {
        kotlin.runCatching {
            val isEnable = params[APP_IS_ENABLE].toString().toBoolean()
            screenShareUseCase.setPrivacyModeEnabled(isEnable)
        }.onFailure {
            logger.error("set privacy mode isEnable wrong")
        }
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }

    override fun addRemoteWindowSizeInfoAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>
    ) {
        handler.complete(addRemoteWindowSizeInfo(miniAppView,params))
    }

    override fun addRemoteWindowSizeInfo(miniAppView: MiniAppView,params: Map<String, Any>): String {
        kotlin.runCatching {
            val width = params[APP_REMOTE_WIDTH_PARAM].toString()
            val height = params[APP_REMOTE_HEIGHT_PARAM] .toString()
            sketchBoardUseCase.addRemoteWindowSizeInfo(width.toFloat().toInt(), height.toFloat().toInt())
        }.onFailure {
            logger.error("remote size convert wrong")
        }
        return JsonUtil.toJson(JSResponse(RESPONSE_SUCCESS_CODE, RESPONSE_SUCCESS_MESSAGE, null))
    }
}