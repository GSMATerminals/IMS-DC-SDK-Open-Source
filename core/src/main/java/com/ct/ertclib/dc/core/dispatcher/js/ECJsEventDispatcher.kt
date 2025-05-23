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
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_EC_REGISTER
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_EC_REQUEST
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.port.dispatcher.IJsEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.mini.IECUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import wendu.dsbridge.CompletionHandler

class ECJsEventDispatcher : IJsEventDispatcher, KoinComponent {

    private val ecUseCase : IECUseCase by inject()

    override fun dispatchAsyncMessage(context: Context, request: JSRequest, handler: CompletionHandler<String?>) {
        when (request.function) {

        }
    }

    override fun dispatchSyncMessage(context: Context, request: JSRequest): String? {
        return when (request.function) {
            FUNCTION_EC_REQUEST -> ecUseCase.request(context, request.params)
            FUNCTION_EC_REGISTER -> ecUseCase.register(context, request.params)
            else -> ""
        }
    }
}