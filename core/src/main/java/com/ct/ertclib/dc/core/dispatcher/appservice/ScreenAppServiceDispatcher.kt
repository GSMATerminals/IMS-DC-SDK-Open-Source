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

import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_ADD_DRAWING_INFO
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_ADD_REMOTE_SIZE_INFO
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_ADD_REMOTE_WINDOW_SIZE_INFO
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_CLOSE_SKETCH_BOARD
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_OPEN_SKETCH_BOARD
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REQUEST_SCREEN_SHARE_ABILITY
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_SET_SCREEN_SHARE_PRIVACY_MODE
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_START_SCREEN_SHARE
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_STOP_SCREEN_SHARE
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_COLOR_PARAMS
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_DRAWING_INFO_PARAMS
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_IS_ENABLE
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_LICENSE_PARAM
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_REMOTE_HEIGHT_PARAM
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_REMOTE_WIDTH_PARAM
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_WIDTH_PARAMS
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.dispatcher.IAppServiceEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.port.usecase.main.ISketchBoardUseCase
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScreenAppServiceDispatcher : IAppServiceEventDispatcher, KoinComponent {

    companion object {
        private const val TAG = "ScreenShareAppServiceDispatcher"
    }

    private val screenShareUseCase: IScreenShareUseCase by inject()
    private val sketchBoardUseCase: ISketchBoardUseCase by inject()
    private val logger = Logger.getLogger(TAG)

    override fun dispatchEvent(telecomCallId: String, appId: String, appRequest: AppRequest, iMessageCallback: IMessageCallback?) {
        when (appRequest.actionName) {
            ACTION_START_SCREEN_SHARE -> {
                val license = appRequest.map[APP_LICENSE_PARAM]
                license?.let {
                    screenShareUseCase.startScreenShare(appId, license.toString(),iMessageCallback)
                }
            }
            ACTION_STOP_SCREEN_SHARE -> screenShareUseCase.stopScreenShare(needNotifyToMini = false)
            ACTION_REQUEST_SCREEN_SHARE_ABILITY -> screenShareUseCase.requestScreenShareAbility(iMessageCallback)
            ACTION_OPEN_SKETCH_BOARD -> {
                val paintColor = appRequest.map[APP_COLOR_PARAMS].toString()
                val paintWidth = appRequest.map[APP_WIDTH_PARAMS].toString().toFloat()
                sketchBoardUseCase.openSketchBoard(telecomCallId, appId, paintColor, paintWidth)
            }
            ACTION_CLOSE_SKETCH_BOARD -> sketchBoardUseCase.closeSketchBoard(needNotifyToMini = false)
            ACTION_ADD_DRAWING_INFO -> {
                val drawingInfo = appRequest.map[APP_DRAWING_INFO_PARAMS]
                drawingInfo?.let {
                    sketchBoardUseCase.addDrawingInfo(JsonUtil.toJson(it))
                }
            }
            ACTION_ADD_REMOTE_SIZE_INFO -> {
                kotlin.runCatching {
                    val width = appRequest.map[APP_REMOTE_WIDTH_PARAM].toString()
                    val height = appRequest.map[APP_REMOTE_HEIGHT_PARAM] .toString()
                    sketchBoardUseCase.addRemoteSizeInfo(width.toFloat().toInt(), height.toFloat().toInt())
                }.onFailure {
                    logger.error("remote size convert wrong")
                }
            }
            ACTION_SET_SCREEN_SHARE_PRIVACY_MODE -> {
                kotlin.runCatching {
                    val isEnable = appRequest.map[APP_IS_ENABLE].toString().toBoolean()
                    screenShareUseCase.setPrivacyModeEnabled(isEnable)
                }.onFailure {
                    logger.error("set privacy mode isEnable wrong")
                }
            }
            ACTION_ADD_REMOTE_WINDOW_SIZE_INFO -> {
                kotlin.runCatching {
                    val width = appRequest.map[APP_REMOTE_WIDTH_PARAM].toString()
                    val height = appRequest.map[APP_REMOTE_HEIGHT_PARAM] .toString()
                    sketchBoardUseCase.addRemoteWindowSizeInfo(width.toFloat().toInt(), height.toFloat().toInt())
                }.onFailure {
                    logger.error("remote size convert wrong")
                }
            }
        }
    }
}