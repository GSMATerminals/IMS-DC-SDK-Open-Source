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

package com.ct.ertclib.dc.core.dispatcher.js

import android.content.Context
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_ADD_CONTACT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_CALL_STATE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_CONTACT_LIST
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_CONTACT_NAME
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_HTTP_RESULT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_MINI_APP_INFO
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
import wendu.dsbridge.CompletionHandler

class MiniAppJsEventDispatcher : IJsEventDispatcher, KoinComponent {

    private val miniAppEventUseCase : IAppMiniUseCase by inject()

    override fun dispatchAsyncMessage(context: Context, request: JSRequest, handler: CompletionHandler<String?>) {
        when (request.function) {
            FUNCTION_GET_MINI_APP_INFO -> miniAppEventUseCase.getMiniAppInfo(context, request.params, handler)
            FUNCTION_START_APP -> miniAppEventUseCase.startApp(context, request.params, handler)
            FUNCTION_SET_WINDOW -> miniAppEventUseCase.setWindow(context, request.params, handler)
            FUNCTION_GET_REMOTE_NUMBER -> miniAppEventUseCase.getRemoteNumber(context, handler)
            FUNCTION_GET_HTTP_RESULT -> miniAppEventUseCase.getHttpResult(request.params, handler)
            FUNCTION_GET_CONTACT_LIST -> miniAppEventUseCase.getContactList(context, request.params,handler)
            FUNCTION_IS_SPEAKERPHONE_ON -> miniAppEventUseCase.isSpeakerphoneOn(context, request.params,handler)
            FUNCTION_IS_MUTED -> miniAppEventUseCase.isMuted(context, request.params,handler)

        }
    }

    override fun dispatchSyncMessage(context: Context, request: JSRequest): String? {
        when (request.function) {
            FUNCTION_ADD_CONTACT -> return miniAppEventUseCase.addOrEditContact(context, request.params)
            FUNCTION_GET_CONTACT_NAME -> return miniAppEventUseCase.getContactName(context, request.params)
            FUNCTION_GET_SDK_INFO -> miniAppEventUseCase.getSDKInfo(context, request.params)
            FUNCTION_GET_SCREEN_INFO -> miniAppEventUseCase.getScreenInfo(context, request.params)
            FUNCTION_HANG_UP -> return miniAppEventUseCase.hangup(context)
            FUNCTION_GET_CALL_STATE -> return miniAppEventUseCase.getCallState(context)
            FUNCTION_REQUEST_START_ADVERSE_APP -> return miniAppEventUseCase.requestStartAdverseApp(context)
            FUNCTION_SET_SYSTEM_API_LICENSE -> return miniAppEventUseCase.setSystemApiLicense(context, request.params)
            FUNCTION_OPEN_WEB -> return miniAppEventUseCase.openWeb(context, request.params)
            FUNCTION_MOVE_TO_FRONT -> return miniAppEventUseCase.moveToFront()
            FUNCTION_STOP_APP -> miniAppEventUseCase.stopApp()
            FUNCTION_GET_SHARE_TYPE_NAME -> miniAppEventUseCase.getShareTypeName(context, request.params)
            FUNCTION_PLAY_DTMF_TONE -> miniAppEventUseCase.playDtmfTone(context, request.params)
            FUNCTION_SET_SPEAKERPHONE -> miniAppEventUseCase.setSpeakerphone(context, request.params)
            FUNCTION_SET_MUTED -> miniAppEventUseCase.setMuted(context, request.params)
        }
        return ""
    }
}