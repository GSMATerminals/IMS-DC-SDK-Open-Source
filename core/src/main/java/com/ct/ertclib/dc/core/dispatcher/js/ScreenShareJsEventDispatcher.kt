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

package com.ct.ertclib.dc.core.dispatcher.js

import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_CLOSE_SKETCH_BOARD
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_DRAWING_INFO
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_OPEN_SKETCH_BOARD
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_REMOTE_SIZE_INFO
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_REMOTE_WINDOW_SIZE_INFO
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_REQUEST_SCREEN_SHARE_ABILITY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SET_PRIVACY_MODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_START_SCREEN_SHARE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_STOP_SCREEN_SHARE
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.port.dispatcher.IJsEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.mini.IScreenShareMiniUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView
import kotlin.math.min

class ScreenShareJsEventDispatcher : IJsEventDispatcher, KoinComponent {

    private val screenShareUseCase : IScreenShareMiniUseCase by inject()

    override fun dispatchAsyncMessage(miniAppView: MiniAppView, request: JSRequest, handler: CompletionHandler<String?>) {
        when (request.function) {
            FUNCTION_START_SCREEN_SHARE -> screenShareUseCase.startScreenShare(miniAppView, request.params,handler)
            FUNCTION_REQUEST_SCREEN_SHARE_ABILITY -> screenShareUseCase.requestScreenShareAbility(miniAppView,handler)

            FUNCTION_STOP_SCREEN_SHARE -> screenShareUseCase.stopScreenShareAsync(miniAppView,handler)
            FUNCTION_OPEN_SKETCH_BOARD -> screenShareUseCase.openSketchBoardAsync(miniAppView,request.params,handler)
            FUNCTION_CLOSE_SKETCH_BOARD -> screenShareUseCase.closeSketchBoardAsync(miniAppView,handler)
            FUNCTION_DRAWING_INFO -> screenShareUseCase.addDrawingInfoAsync(miniAppView,request.params,handler)
            FUNCTION_REMOTE_SIZE_INFO -> screenShareUseCase.addRemoteSizeInfoAsync(miniAppView,request.params,handler)
            FUNCTION_SET_PRIVACY_MODE -> screenShareUseCase.setPrivacyModeAsync(miniAppView,request.params,handler)
            FUNCTION_REMOTE_WINDOW_SIZE_INFO -> screenShareUseCase.addRemoteWindowSizeInfoAsync(miniAppView,request.params,handler)
        }
    }

    override fun dispatchSyncMessage(miniAppView: MiniAppView, request: JSRequest): String {
        return when (request.function) {
            FUNCTION_STOP_SCREEN_SHARE -> screenShareUseCase.stopScreenShare(miniAppView)
            FUNCTION_OPEN_SKETCH_BOARD -> screenShareUseCase.openSketchBoard(miniAppView,request.params)
            FUNCTION_CLOSE_SKETCH_BOARD -> screenShareUseCase.closeSketchBoard(miniAppView,)
            FUNCTION_DRAWING_INFO -> screenShareUseCase.addDrawingInfo(miniAppView,request.params)
            FUNCTION_REMOTE_SIZE_INFO -> screenShareUseCase.addRemoteSizeInfo(miniAppView,request.params)
            FUNCTION_SET_PRIVACY_MODE -> screenShareUseCase.setPrivacyMode(miniAppView,request.params)
            FUNCTION_REMOTE_WINDOW_SIZE_INFO -> screenShareUseCase.addRemoteWindowSizeInfo(miniAppView,request.params)
            else -> ""
        }

    }
}