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

package com.ct.ertclib.dc.core.factory

import com.ct.ertclib.dc.core.constants.CommonConstants.CALL_APP_EVENT
import com.ct.ertclib.dc.core.constants.CommonConstants.COMMON_APP_EVENT
import com.ct.ertclib.dc.core.constants.CommonConstants.SCREEN_SHARE_APP_EVENT
import com.ct.ertclib.dc.core.dispatcher.appservice.CallAppServiceDispatcher
import com.ct.ertclib.dc.core.dispatcher.appservice.CommonAppServiceDispatcher
import com.ct.ertclib.dc.core.dispatcher.appservice.DefaultAppServiceDispatcher
import com.ct.ertclib.dc.core.dispatcher.appservice.ScreenAppServiceDispatcher
import com.ct.ertclib.dc.core.port.dispatcher.IAppServiceEventDispatcher

object AppServiceEventDispatcherFactory {

    private val defaultDispatcher : IAppServiceEventDispatcher by lazy { DefaultAppServiceDispatcher() }
    private val commonDispatcher: IAppServiceEventDispatcher by lazy { CommonAppServiceDispatcher() }
    private val callDispatcher: IAppServiceEventDispatcher by lazy { CallAppServiceDispatcher() }
    private val screenShareDispatcher: IAppServiceEventDispatcher by lazy { ScreenAppServiceDispatcher() }

    @JvmStatic
    fun getDispatcher(eventName: String): IAppServiceEventDispatcher {
        return when (eventName) {
            CALL_APP_EVENT -> callDispatcher
            COMMON_APP_EVENT -> commonDispatcher
            SCREEN_SHARE_APP_EVENT -> screenShareDispatcher
            else -> defaultDispatcher
        }
    }
}