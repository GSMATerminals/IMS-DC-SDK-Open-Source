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
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_CLOSE_DATA_CHANNEL
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_CREATE_DATA_CHANNEL
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_BUFFER_AMOUNT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_IS_PEER_SUPPORT_DC
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SEND_DATA
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.port.dispatcher.IJsEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.mini.IDCMiniEventUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import wendu.dsbridge.CompletionHandler

class DCJsEventDispatcher : IJsEventDispatcher, KoinComponent {

    private val dcEventUseCase : IDCMiniEventUseCase by inject()

    override fun dispatchAsyncMessage(context: Context, request: JSRequest, handler: CompletionHandler<String?>) {
        when (request.function) {
            FUNCTION_CREATE_DATA_CHANNEL -> { dcEventUseCase.createAppDataChannel(context, request.params, handler) }
            FUNCTION_CLOSE_DATA_CHANNEL -> { dcEventUseCase.closeAppDataChannel(context, request.params, handler) }
            FUNCTION_SEND_DATA -> { dcEventUseCase.sendData(context, request.params, handler) }
            FUNCTION_IS_PEER_SUPPORT_DC -> { dcEventUseCase.isPeerSupportDC(context, request.params, handler) }
        }
    }

    override fun dispatchSyncMessage(context: Context, request: JSRequest): String? {
        return when (request.function) {
            FUNCTION_GET_BUFFER_AMOUNT -> dcEventUseCase.getBufferedAmount(context, request.params)
            else -> ""
        }
    }
}