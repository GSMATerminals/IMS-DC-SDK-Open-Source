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

package com.ct.ertclib.dc.core.miniapp.bridge

import android.content.Context
import android.os.Build
import android.webkit.JavascriptInterface
import androidx.annotation.RequiresApi
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.factory.JsEventDispatcherFactory
import wendu.dsbridge.CompletionHandler
import java.io.InputStream
import java.io.OutputStream

class JSApi(private val context: Context) {

    companion object {
        private const val TAG = "JSApi"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    @JavascriptInterface
    fun async(msg: Any, handler: CompletionHandler<String?>) {
        try {
            sLogger.info("JSApi asyn ,msg:$msg, handler:$handler")
            val jsRequest = JsonUtil.fromJson(msg.toString(), JSRequest::class.java)
            jsRequest?.let {
                JsEventDispatcherFactory.getEventDispatcher(jsRequest.event).dispatchAsyncMessage(context, jsRequest, handler)
            }
        }catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }


    @Deprecated(
        message = "This function is deprecated. Use async instead.",
        replaceWith = ReplaceWith("async"),
        level = DeprecationLevel.WARNING
    )
    @RequiresApi(Build.VERSION_CODES.Q)
    @JavascriptInterface
    fun sync(msg: Any): String? {
        try {
            sLogger.info("JSApi sync ,msg:$msg")
            val jsRequest = JsonUtil.fromJson(msg.toString(), JSRequest::class.java)
            jsRequest?.let {
                return JsEventDispatcherFactory.getEventDispatcher(jsRequest.event).dispatchSyncMessage(context, jsRequest)
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        return "$msg［syn call］"
    }
}