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

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity
import com.ct.ertclib.dc.core.utils.logger.Logger
import androidx.core.net.toUri
import com.ct.ertclib.dc.core.port.miniapp.IMiniApp


class CTWebViewClient(private val miniAppActivity: MiniAppActivity) : WebViewClient() {

    companion object {
        private const val TAG = "CTWebViewClient"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    @SuppressLint("QueryPermissionsNeeded")
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        sLogger.info("shouldOverrideUrlLoading request:${request?.url}")
        val url = request?.url.toString()

        // 如果不在allowedUrls中，则拦截
        var pass = false
        (miniAppActivity as? IMiniApp)?.miniApp?.appProperties?.allowedUrls?.forEach{
            if (url.startsWith(it)){
                pass = true
            }
        }

        if (!pass){
            sLogger.error("shouldOverrideUrlLoading url:$url not in app urls")
            return true
        }

        // 打开其他应用要拦截处理
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // scheme
            try {
                val uri = url.toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // 检查应用是否存在
                if (intent.resolveActivity(miniAppActivity.packageManager) != null) {
                    sLogger.info("startActivity intent:$intent")
                    miniAppActivity.startActivity(intent)
                } else {
                    // 应用未安装，跳转到fallback_url
                    sLogger.error("Application not installed, redirecting to fallback URL")
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return true
            }
        }
        // 放行
        return false
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