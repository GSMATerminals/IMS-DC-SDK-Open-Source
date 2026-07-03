/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.core.miniapp.bridge

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.blankj.utilcode.util.ArrayUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.manager.common.LicenseManager
import com.ct.ertclib.dc.core.data.miniapp.MiniAppPermissions
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.core.graphics.createBitmap
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

class CTWebChromeClient(private val miniAppView: MiniAppView) : WebChromeClient(), KoinComponent {

    companion object {
        private const val PROGRESS_PERCENT_LOADED = 100
        private const val TAG = "CTWebChromeClient"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

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
        }
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
        miniAppView.viewModel.miniAppInfo?.appId?.let {
            if (permissionMiniUseCase.checkPermissionAndRecord(it, listOf(MiniAppPermissions.MINIAPP_CAMERA, MiniAppPermissions.MINIAPP_RECORD_AUDIO))
                && miniAppView.viewModel.systemApiLicenseMap["getUserMedia"]!=null
                && LicenseManager.getInstance().verifyLicense(it, LicenseManager.ApiCode.GET_USER_MEDIA.apiCode, miniAppView.viewModel.systemApiLicenseMap["getUserMedia"].toString()
                )) {
                sLogger.info("onPermissionRequest, request:${ArrayUtils.toString(request?.resources)}")
                request?.grant(request.resources)
            } else {
                sLogger.warn("onPermissionRequest, permission not granted")
            }
        }
    }

    override fun getDefaultVideoPoster(): Bitmap? {
        return createBitmap(1, 1)
    }
}