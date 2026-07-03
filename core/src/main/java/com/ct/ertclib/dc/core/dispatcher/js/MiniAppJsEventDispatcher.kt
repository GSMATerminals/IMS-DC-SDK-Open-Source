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

package com.ct.ertclib.dc.core.dispatcher.js

import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_ADD_CONTACT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_ANSWER
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_CALL_STATE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_CONTACT_LIST
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_CONTACT_NAME
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_HTTP_RESULT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_MINI_APP_INFO
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_PLATFORM_INFO
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_REMOTE_NUMBER
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_SCREEN_INFO
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_SDK_INFO
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_SHARE_TYPE_NAME
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_HANG_UP
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_IS_MUTED
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_IS_SPEAKERPHONE_ON
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_MOVE_TO_FRONT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_OPEN_WEB
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_PLAY_DTMF_TONE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_REQUEST_START_ADVERSE_APP
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SET_MUTED
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SET_SPEAKERPHONE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SET_SYSTEM_API_LICENSE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SET_WINDOW
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_START_APP
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_STOP_APP
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.port.dispatcher.IJsEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.mini.IAppMiniUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

class MiniAppJsEventDispatcher : IJsEventDispatcher, KoinComponent {

    private val miniAppEventUseCase : IAppMiniUseCase by inject()

    override fun dispatchAsyncMessage(miniAppView: MiniAppView, request: JSRequest, handler: CompletionHandler<String?>) {
        when (request.function) {
            FUNCTION_GET_MINI_APP_INFO -> miniAppEventUseCase.getMiniAppInfo(miniAppView, request.params, handler)
            FUNCTION_START_APP -> miniAppEventUseCase.startApp(miniAppView, request.params, handler)
            FUNCTION_SET_WINDOW -> miniAppEventUseCase.setWindow(miniAppView, request.params, handler)
            FUNCTION_GET_REMOTE_NUMBER -> miniAppEventUseCase.getRemoteNumber(miniAppView, handler)
            FUNCTION_GET_HTTP_RESULT -> miniAppEventUseCase.getHttpResult(request.params, handler)
            FUNCTION_GET_CONTACT_LIST -> miniAppEventUseCase.getContactList(miniAppView, request.params,handler)
            FUNCTION_IS_SPEAKERPHONE_ON -> miniAppEventUseCase.isSpeakerphoneOn(miniAppView, request.params,handler)
            FUNCTION_IS_MUTED -> miniAppEventUseCase.isMuted(miniAppView, request.params,handler)

            FUNCTION_ADD_CONTACT -> miniAppEventUseCase.addOrEditContactAsync(miniAppView, request.params,handler)
            FUNCTION_GET_CONTACT_NAME -> miniAppEventUseCase.getContactNameAsync(miniAppView, request.params,handler)
            FUNCTION_GET_SDK_INFO -> miniAppEventUseCase.getSDKInfoAsync(miniAppView, request.params,handler)
            FUNCTION_GET_SCREEN_INFO -> miniAppEventUseCase.getScreenInfoAsync(miniAppView, request.params,handler)
            FUNCTION_HANG_UP -> miniAppEventUseCase.hangupAsync(miniAppView,handler)
            FUNCTION_GET_CALL_STATE -> miniAppEventUseCase.getCallStateAsync(miniAppView,handler)
            FUNCTION_REQUEST_START_ADVERSE_APP -> miniAppEventUseCase.requestStartAdverseAppAsync(miniAppView,handler)
            FUNCTION_SET_SYSTEM_API_LICENSE -> miniAppEventUseCase.setSystemApiLicenseAsync(miniAppView, request.params,handler)
            FUNCTION_OPEN_WEB ->  miniAppEventUseCase.openWebAsync(miniAppView, request.params,handler)
            FUNCTION_MOVE_TO_FRONT -> miniAppEventUseCase.moveToFrontAsync(miniAppView,handler)
            FUNCTION_STOP_APP -> miniAppEventUseCase.stopAppAsync(miniAppView,handler)
            FUNCTION_GET_SHARE_TYPE_NAME -> miniAppEventUseCase.getShareTypeNameAsync(miniAppView, request.params,handler)
            FUNCTION_PLAY_DTMF_TONE -> miniAppEventUseCase.playDtmfToneAsync(miniAppView, request.params,handler)
            FUNCTION_SET_SPEAKERPHONE -> miniAppEventUseCase.setSpeakerphoneAsync(miniAppView, request.params,handler)
            FUNCTION_SET_MUTED -> miniAppEventUseCase.setMutedAsync(miniAppView, request.params,handler)
            FUNCTION_ANSWER ->  miniAppEventUseCase.answerAsync(miniAppView,handler)
            FUNCTION_GET_PLATFORM_INFO -> miniAppEventUseCase.getPlatformInfo(miniAppView, request.params, handler)
        }
    }

    override fun dispatchSyncMessage(miniAppView: MiniAppView, request: JSRequest): String? {
        when (request.function) {
            FUNCTION_ADD_CONTACT -> return miniAppEventUseCase.addOrEditContact(miniAppView, request.params)
            FUNCTION_GET_CONTACT_NAME -> return miniAppEventUseCase.getContactName(miniAppView, request.params)
            FUNCTION_GET_SDK_INFO -> miniAppEventUseCase.getSDKInfo(miniAppView, request.params)
            FUNCTION_GET_SCREEN_INFO -> miniAppEventUseCase.getScreenInfo(miniAppView, request.params)
            FUNCTION_HANG_UP -> return miniAppEventUseCase.hangup(miniAppView)
            FUNCTION_GET_CALL_STATE -> return miniAppEventUseCase.getCallState(miniAppView)
            FUNCTION_REQUEST_START_ADVERSE_APP -> return miniAppEventUseCase.requestStartAdverseApp(miniAppView)
            FUNCTION_SET_SYSTEM_API_LICENSE -> return miniAppEventUseCase.setSystemApiLicense(miniAppView, request.params)
            FUNCTION_OPEN_WEB -> return miniAppEventUseCase.openWeb(miniAppView, request.params)
            FUNCTION_MOVE_TO_FRONT -> return miniAppEventUseCase.moveToFront(miniAppView)
            FUNCTION_STOP_APP -> miniAppEventUseCase.stopApp(miniAppView)
            FUNCTION_GET_SHARE_TYPE_NAME -> miniAppEventUseCase.getShareTypeName(miniAppView, request.params)
            FUNCTION_PLAY_DTMF_TONE -> miniAppEventUseCase.playDtmfTone(miniAppView, request.params)
            FUNCTION_SET_SPEAKERPHONE -> miniAppEventUseCase.setSpeakerphone(miniAppView, request.params)
            FUNCTION_SET_MUTED -> miniAppEventUseCase.setMuted(miniAppView, request.params)
            FUNCTION_ANSWER -> return miniAppEventUseCase.answer(miniAppView)
        }
        return ""
    }
}