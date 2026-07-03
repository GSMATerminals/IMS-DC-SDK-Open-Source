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

import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_EC_QUERY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_EC_REGISTER
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_EC_REQUEST
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.port.dispatcher.IJsEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.mini.IECUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

class ECJsEventDispatcher : IJsEventDispatcher, KoinComponent {

    private val ecUseCase : IECUseCase by inject()

    override fun dispatchAsyncMessage(miniAppView: MiniAppView, request: JSRequest, handler: CompletionHandler<String?>) {
        when (request.function) {
            FUNCTION_EC_QUERY -> ecUseCase.queryEC(miniAppView, handler)
            FUNCTION_EC_REGISTER -> ecUseCase.registerAsync(miniAppView, request.params, handler)
            FUNCTION_EC_REQUEST -> ecUseCase.requestAsync(miniAppView, request.params, handler)
        }
    }

    override fun dispatchSyncMessage(miniAppView: MiniAppView, request: JSRequest): String? {
        return when (request.function) {
            FUNCTION_EC_REGISTER -> ecUseCase.register(miniAppView, request.params)
            FUNCTION_EC_REQUEST -> ecUseCase.request(miniAppView, request.params)
            else -> ""
        }
    }
}