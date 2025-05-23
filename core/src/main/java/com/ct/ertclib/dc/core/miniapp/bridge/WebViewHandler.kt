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

package com.ct.ertclib.dc.core.miniapp.bridge

import com.ct.ertclib.dc.core.utils.logger.Logger
import wendu.dsbridge.DWebView

class WebViewHandler(val webView: DWebView) {

    companion object {
        private const val TAG = "WebViewHandler"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)
    fun callHandler(method: String, args: Array<String>) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("callHandler method:$method, args:$args")
        }

        webView.callHandler(method, args)
    }

}