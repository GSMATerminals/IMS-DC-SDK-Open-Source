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

import android.net.Uri
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import com.blankj.utilcode.util.ArrayUtils
import com.blankj.utilcode.util.BarUtils
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.common.LicenseManager
import com.ct.ertclib.dc.core.data.miniapp.MiniAppPermissions
import com.ct.ertclib.dc.core.port.miniapp.IMiniApp
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CTWebChromeClient(private val miniAppActivity: MiniAppActivity) : WebChromeClient(), KoinComponent {

    companion object {
        private const val PROGRESS_PERCENT_LOADED = 100
        private const val TAG = "CTWebChromeClient"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    private var mCustomView: View? = null
    private var mCallback : CustomViewCallback?= null
    private val permissionMiniUseCase: IPermissionUseCase by inject()

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (sLogger.isDebugActivated) {
            sLogger.debug("onConsoleMessage name:${consoleMessage?.messageLevel()?.name}, message:${consoleMessage?.message()}")
        }
        return true
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        if (newProgress >= PROGRESS_PERCENT_LOADED) {
            sLogger.info("onProgressChanged,newProgress: $newProgress ")
            miniAppActivity.onMiniAppLoaded()
        }
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (sLogger.isDebugActivated) {
            sLogger.info("onShowCustomView, view:$view, callback:$callback")
        }
        if (view == null || callback == null) {
            return
        }
        mCustomView = view
        mCallback = callback

        val decorView = miniAppActivity.window.decorView
        (decorView as FrameLayout).addView(mCustomView)

        BarUtils.setStatusBarVisibility(miniAppActivity.window, false)
        BarUtils.setNavBarVisibility(miniAppActivity.window, false)
    }

    override fun onHideCustomView() {
        if (sLogger.isDebugActivated) {
            sLogger.info("onHideCustomView")
        }
        val decorView = miniAppActivity.window.decorView
        (decorView as FrameLayout).removeView(mCustomView)
        mCallback?.onCustomViewHidden()

        mCustomView = null
        mCallback = null

        BarUtils.setStatusBarVisibility(miniAppActivity.window, true)
        BarUtils.setNavBarVisibility(miniAppActivity.window, true)

        super.onHideCustomView()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (sLogger.isDebugActivated) {
            sLogger.info("onShowFileChooser, filePathCallback:$filePathCallback, fileChooserParams:$fileChooserParams")
        }
        return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        // 目前只有获取视频流的接口getUserMedia会在这里申请权限
        (miniAppActivity as? IMiniApp)?.miniApp?.appId?.let {
            if (permissionMiniUseCase.isPermissionGranted(it, listOf(MiniAppPermissions.MINIAPP_CAMERA, MiniAppPermissions.MINIAPP_RECORD_AUDIO))
                && miniAppActivity.miniApp!=null
                && miniAppActivity.miniToParentManager.systemApiLicenseMap["getUserMedia"]!=null
                && LicenseManager.getInstance().verifyLicense(miniAppActivity.miniApp!!.appId, LicenseManager.ApiCode.GET_USER_MEDIA.apiCode, miniAppActivity.miniToParentManager.systemApiLicenseMap["getUserMedia"].toString()
                )) {
                sLogger.info("onPermissionRequest, request:${ArrayUtils.toString(request?.resources)}")
                request?.grant(request.resources)
            } else {
                sLogger.warn("onPermissionRequest, permission not granted")
            }
        }
    }
}