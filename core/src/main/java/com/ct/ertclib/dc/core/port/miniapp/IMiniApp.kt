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

package com.ct.ertclib.dc.core.port.miniapp

import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.miniapp.WindowStyle
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener

interface IMiniApp {

    var miniApp: MiniAppInfo?
    var callInfo: CallInfo?
    var miniAppListInfo: MiniAppList?

    fun finishAndKillMiniAppActivity()

    fun callHandler(method: String, args: Array<Any>)

    fun invokeOnServiceConnected()

    fun invokeOnCallStateChange(params: Map<String, Any?>)

    fun invokeOnCheckAlive()

    fun selectFile(callback: OnPickMediaCallbackListener)

    fun refreshPermission()

    fun setWindowStyle()

    fun setPageName(pageName:String)

    fun onAudioDeviceChange()

    fun playVoice(path:String)

    fun stopPlayVoice()
}