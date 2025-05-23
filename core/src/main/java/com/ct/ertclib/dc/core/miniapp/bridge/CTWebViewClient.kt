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

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity
import com.ct.ertclib.dc.core.utils.logger.Logger

class CTWebViewClient(private val miniAppActivity: MiniAppActivity) : WebViewClient() {

    companion object {
        private const val TAG = "CTWebViewClient"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (sLogger.isDebugActivated) sLogger.debug("shouldOverrideUrlLoading request:$request")
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        if (sLogger.isDebugActivated) sLogger.debug("onPageStarted url:$url")
        miniAppActivity.setPageName("")
        super.onPageStarted(view, url, favicon)

    }

    override fun onPageFinished(view: WebView?, url: String?) {
        if (sLogger.isDebugActivated) sLogger.debug("onPageFinished url:$url")
        miniAppActivity.updateBack()
        super.onPageFinished(view, url)

    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (sLogger.isDebugActivated) sLogger.debug("onReceivedError error:${error.toString()}")
        super.onReceivedError(view, request, error)
    }

}