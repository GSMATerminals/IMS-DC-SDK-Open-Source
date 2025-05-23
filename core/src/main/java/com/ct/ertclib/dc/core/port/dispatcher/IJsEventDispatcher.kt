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

package com.ct.ertclib.dc.core.port.dispatcher

import android.content.Context
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import wendu.dsbridge.CompletionHandler

interface IJsEventDispatcher {


    /**
     * 分发异步消息
     * @param request: 小程序传递的详细请求信息
     * @param handler： 用于传递异步的返回结果
     */
    fun dispatchAsyncMessage(context: Context, request: JSRequest, handler: CompletionHandler<String?>)

    /**
     * 分发同步消息
     * @param request: 小程序传递的详细请求信息
     * @return 同步消息的返回结果
     */
    fun dispatchSyncMessage(context: Context, request: JSRequest): String?
}