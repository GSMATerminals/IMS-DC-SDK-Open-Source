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

package com.ct.ertclib.dc.core.dispatcher.appservice


import android.content.Context
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_MOVE_TO_FRONT
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REFRESH_PERMISSION
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REQUEST_START_ADVERSE_APP
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_START_APP
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.miniapp.MiniAppStartManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.dispatcher.IAppServiceEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CommonAppServiceDispatcher : IAppServiceEventDispatcher, KoinComponent {

    private val permissionUseCase: IPermissionUseCase by inject()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val applicationContext: Context by inject()

    override fun dispatchEvent(telecomCallId: String, appId: String, appRequest: AppRequest, iMessageCallback: IMessageCallback?) {
        when (appRequest.actionName) {
            ACTION_REQUEST_START_ADVERSE_APP -> {
                //暂时直接调用，后面把AppService的事件都整理的时候再调整
                MiniAppManager.getAppPackageManager(telecomCallId)?.requestStartAdverseApp(appId)
            }
            ACTION_REFRESH_PERMISSION -> {
                scope.launch {
                    permissionUseCase.refreshPermissionMapFromRepo(appId)
                }
            }
            ACTION_MOVE_TO_FRONT -> {
                MiniAppStartManager.moveMiniAppToFront(applicationContext, appId)
            }
            ACTION_START_APP -> {
                val callId = appRequest.map["telecomCallId"]
                val appId = appRequest.map["appId"]
                callId.let {
                    MiniAppManager.getAppPackageManager(it as String)
                        ?.startMiniApp(appId as String, null)
                }
            }
        }
    }
}