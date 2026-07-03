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

package com.ct.ertclib.dc.core.port.usecase.mini

import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

interface IDCMiniEventUseCase {

    /**
     * 创建数据通道
     * @param params: 创建数据通道时需要的参数集合
     * @param handler: 用于返回异步信息结果
     */
    fun createAppDataChannel(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun sendData(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun closeAppDataChannel(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun isPeerSupportDC(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getBufferedAmount(miniAppView: MiniAppView, params: Map<String, Any>) : String?

    fun getBufferedAmountAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)
}