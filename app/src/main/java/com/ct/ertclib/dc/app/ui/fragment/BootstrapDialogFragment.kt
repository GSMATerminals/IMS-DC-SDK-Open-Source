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

package com.ct.ertclib.dc.app.ui.fragment

import android.app.Dialog
import android.app.WallpaperManager
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.ct.ertclib.dc.app.databinding.BootstrapDialogBinding
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.model.AdItem
import com.ct.ertclib.dc.core.miniapp.bridge.BootstrapJSApi
import com.ct.ertclib.dc.core.utils.common.FileUtils
import androidx.core.graphics.drawable.toDrawable
import com.ct.ertclib.dc.core.data.miniapp.MiniAppProperties
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import com.ct.ertclib.dc.core.constants.MiniAppConstants
import com.ct.ertclib.dc.core.data.bootstrap.BootstrapProperties
import com.ct.ertclib.dc.core.manager.call.DCManager
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File


import android.view.View
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build

class BootstrapDialogFragment(
    private var miniAppList: MiniAppList,
    private var adList: ArrayList<AdItem>,
    private var callInfo: CallInfo
) :
    BaseMiniAppListDialogFragment() {

    companion object {
        private const val TAG = "BootstrapDialogFragment"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    private lateinit var viewBinding: BootstrapDialogBinding
    private var callback: Callback? = null
    private var api: BootstrapJSApi? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "onCreate")
        super.onCreate(savedInstanceState)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewBinding = BootstrapDialogBinding.inflate(layoutInflater)
        dialog.setContentView(viewBinding.root)
        scope.launch(Dispatchers.IO) {
            FileUtils.getLatestInstalledBootstrapPath()?.let {
                val deferred = async {
                    val file = File("$it/properties.json")
                    if (file.exists()) {
                        val propertiesString = file.readText()
                        JsonUtil.fromJson(propertiesString, BootstrapProperties::class.java)
                    } else {
                        null
                    }
                }
                val properties = deferred.await()
                if (sLogger.isDebugActivated) sLogger.debug("startBootstrap properties:$properties")
                // todo 处理窗口样式
            }
        }

        dialog.window?.let { window ->
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }

        if (!NewCallAppSdkInterface.isVideoCall(callInfo.telecomCallId)) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(requireContext())
                val wallpaperDrawable = wallpaperManager.drawable
                viewBinding.ivBackground.apply {
                    visibility = View.VISIBLE
                    setImageDrawable(wallpaperDrawable)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRenderEffect(
                            RenderEffect.createBlurEffect(
                                120f,
                                120f,
                                Shader.TileMode.CLAMP
                            )
                        )
                    }
                }
                // viewBinding.viewOverlay.visibility = View.VISIBLE
            } catch (e: Exception) {
                sLogger.error("Failed to set background effects", e)
            }
        }
        dialog.setCanceledOnTouchOutside(true)
        initView()
        loadUrl()
        return dialog
    }

    override fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private fun initView() {
        setupWebView()
    }

    private fun setupWebView() {
        api = BootstrapJSApi(requireContext(), miniAppList,callInfo)
        api?.setFinishCallback(object : BootstrapJSApi.IBootstrapFinishCallback {
            override fun onFinish() {
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "Exit BootstrapDialog")
                dismiss()
            }
        })
        api?.init()
        viewBinding.webView.apply {
            setupWebViewSettings()
            addJavascriptObject(api, "")
            setBackgroundColor(Color.TRANSPARENT)
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        if (sLogger.isDebugActivated) {
                            sLogger.debug("onConsoleMessage name:${it.messageLevel()?.name}, message:${it.message()}")
                        }
                    }
                    return true
                }
            }
        }
        WebView.setWebContentsDebuggingEnabled(true)
    }

    private fun setupWebViewSettings() {
        viewBinding.webView.settings.apply {
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadWithOverviewMode = false
            useWideViewPort = false
            javaScriptEnabled = true
            domStorageEnabled = true
            displayZoomControls = false
            builtInZoomControls = false
            textZoom = 100
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "$userAgentString;MiniAppContainer"

            // 渲染优先级
            setRenderPriority(WebSettings.RenderPriority.HIGH)

            // 启用平滑滚动
            viewBinding.webView.isVerticalScrollBarEnabled = true
            viewBinding.webView.isHorizontalScrollBarEnabled = false
        }
        // 开启硬件加速
        viewBinding.webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
    }

    fun loadUrl(){
        FileUtils.getLatestInstalledBootstrapPath()?.let {
            NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "loadUrl: $it")
            viewBinding.webView.loadUrl("file://$it/index.html?callId=${callInfo.telecomCallId}")
        }

    }


    override fun onDismiss(dialog: DialogInterface) {
        notifyBootstrapAppState("onFinish")
        super.onDismiss(dialog)
        scope.cancel()
        api?.release()
        api = null
        callback?.onDismiss()
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "onDismiss")
    }

    override fun onResume() {
        super.onResume()
        notifyBootstrapAppState("onFront")
    }

    override fun onPause() {
        super.onPause()
        notifyBootstrapAppState("onBackground")
    }

    private fun notifyBootstrapAppState(state: String) {
        if (::viewBinding.isInitialized) {
            viewBinding.webView.callHandler(
                MiniAppConstants.FUNCTION_MINI_APP_NOTIFY,
                arrayOf(JsonUtil.toJson(mapOf("miniAppState" to state)))
            )
        }
    }

    override fun refreshAppStatus() {
    }

    override fun onBootstrapMessage(label: String, message: String) {
        sLogger.info("onBootstrapMessage label:$label")
        val map = mapOf(
            "dcLabel" to label,
            "message" to message
        )
        viewBinding.webView.callHandler(
            MiniAppConstants.FUNCTION_NOTIFY_MESSAGE,
            arrayOf(JsonUtil.toJson(map))
        )

    }

    override fun onBootstrapAppDataChannelStateChange(label: String, status: Int) {
        sLogger.info("onBootstrapDataChannelStateChange label:$label, status:$status")
        val map = mapOf(
            "dcLabel" to label,
            "imsDCStatus" to status
        )
        viewBinding.webView.callHandler(
            MiniAppConstants.FUNCTION_NOTIFY_DATA_CHANNEL,
            arrayOf(JsonUtil.toJson(map))
        )
    }

    override fun onAudioDeviceChange() {
        sLogger.info("onAudioDeviceChange")
        viewBinding.webView.callHandler(
            MiniAppConstants.FUNCTION_AUDIO_DEVICE_NOTIFY,
            arrayOf("{}")
        )
    }

    override fun refreshCallInfo(callInfo: CallInfo) {
        this.callInfo = callInfo
        viewBinding.webView.callHandler(
            MiniAppConstants.FUNCTION_CALL_STATE_NOTIFY,
            arrayOf(JsonUtil.toJson(mapOf("state" to callInfo.state)))
        )
    }
}