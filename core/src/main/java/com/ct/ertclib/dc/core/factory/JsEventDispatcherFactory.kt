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

package com.ct.ertclib.dc.core.factory

import com.ct.ertclib.dc.core.constants.MiniAppConstants.EVENT_DC
import com.ct.ertclib.dc.core.constants.MiniAppConstants.EVENT_EC
import com.ct.ertclib.dc.core.constants.MiniAppConstants.EVENT_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.EVENT_MINI_APP
import com.ct.ertclib.dc.core.constants.MiniAppConstants.EVENT_SCREEN_SHARE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.SYSTEM_EVENT
import com.ct.ertclib.dc.core.dispatcher.js.DCJsEventDispatcher
import com.ct.ertclib.dc.core.dispatcher.js.DefaultJsDispatcher
import com.ct.ertclib.dc.core.dispatcher.js.ECJsEventDispatcher
import com.ct.ertclib.dc.core.dispatcher.js.FileJsEventDispatcher
import com.ct.ertclib.dc.core.dispatcher.js.MiniAppJsEventDispatcher
import com.ct.ertclib.dc.core.dispatcher.js.SystemJsEventDispatcher
import com.ct.ertclib.dc.core.dispatcher.js.ScreenShareJsEventDispatcher
import com.ct.ertclib.dc.core.port.dispatcher.IJsEventDispatcher

object JsEventDispatcherFactory {

    private val defaultDispatcher: IJsEventDispatcher by lazy { DefaultJsDispatcher() }
    private val dcEventDispatcher: IJsEventDispatcher by lazy { DCJsEventDispatcher() }
    private val fileEventDispatcher: IJsEventDispatcher by lazy { FileJsEventDispatcher() }
    private val miniAppEventDispatcher: IJsEventDispatcher by lazy { MiniAppJsEventDispatcher() }
    private val screenShareEventDispatcher: IJsEventDispatcher by lazy { ScreenShareJsEventDispatcher() }
    private val ecDispatcher: IJsEventDispatcher by lazy { ECJsEventDispatcher() }
    private val systemEventDispatcher: IJsEventDispatcher by lazy { SystemJsEventDispatcher() }

    @JvmStatic
    fun getEventDispatcher(eventType: String): IJsEventDispatcher {
        return when(eventType) {
            EVENT_DC -> dcEventDispatcher
            EVENT_MINI_APP -> miniAppEventDispatcher
            EVENT_FILE -> fileEventDispatcher
            EVENT_SCREEN_SHARE -> screenShareEventDispatcher
            EVENT_EC -> ecDispatcher
            SYSTEM_EVENT -> systemEventDispatcher
            else -> defaultDispatcher
        }
    }
}