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

package com.ct.ertclib.dc.app.ui.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.ct.ertclib.dc.app.R
import android.webkit.WebSettings
import android.webkit.WebView
import com.ct.ertclib.dc.app.databinding.BootstrapDialogBinding
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.model.AdItem
import com.ct.ertclib.dc.core.miniapp.bridge.BootstrapJSApi
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.utils.common.FloatWindowMetrics
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
import android.telecom.Call

class BootstrapDialogFragment : BaseMiniAppListDialogFragment() {

    companion object {
        private const val TAG = "BootstrapDialogFragment"

        fun newInstance(miniAppList: MiniAppList, adList: ArrayList<AdItem>, callInfo: CallInfo): BootstrapDialogFragment {
            val fragment = BootstrapDialogFragment()
            val args = Bundle().apply {
                putParcelable("miniAppList", miniAppList)
                putParcelableArrayList("adList", adList)
                putParcelable("callInfo", callInfo)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var miniAppList: MiniAppList
    private lateinit var adList: ArrayList<AdItem>
    private lateinit var callInfo: CallInfo

    private val sLogger: Logger = Logger.getLogger(TAG)

    private lateinit var viewBinding: BootstrapDialogBinding
    private var callback: Callback? = null
    private var api: BootstrapJSApi? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "onCreate")
        super.onCreate(savedInstanceState)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        arguments?.let {
            miniAppList = it.getParcelable("miniAppList")!!
            adList = it.getParcelableArrayList("adList")!!
            callInfo = it.getParcelable("callInfo")!!
        }
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
            if (isLargeWindow) {
                // 宽屏：可拖动小窗，尺寸/位置由 Activity 透传
                window.setLayout(windowWidth, windowHeight)
                window.setGravity(Gravity.START or Gravity.TOP)
                window.attributes = window.attributes.apply {
                    x = windowX
                    y = windowY
                }
                window.setBackgroundDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.expanding_window_background)
                        ?: Color.TRANSPARENT.toDrawable()
                )
                // 用 elevation + 圆角 outline 产生真阴影，替代 layer-list 假阴影（黑色硬边框）
                window.decorView.elevation = 16f
                window.decorView.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            } else {
                // 非宽屏：保持满屏不变
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                window.setGravity(Gravity.CENTER)
                window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
        }

        // 仅宽屏小窗启用右上角拖拽按钮拖动（避开 WebView 对触摸事件的拦截）
        if (isLargeWindow) {
            viewBinding.dragHandle.visibility = View.VISIBLE
            setupDraggable(viewBinding.dragHandle)
        } else {
            viewBinding.dragHandle.visibility = View.GONE
        }

        dialog.setCanceledOnTouchOutside(true)
        initView()
        loadUrl()
        return dialog
    }

    override fun setCallback(callback: Callback) {
        this.callback = callback
    }

    /**
     * 折叠屏展开/折叠切换时，由 Activity 的 onConfigurationChanged 调用，
     * 重新计算并应用窗口尺寸、位置和背景。
     * 如果用户已拖动过窗口，保留用户位置，仅更新尺寸。
     */
    override fun reapplyWindow() {
        if (dialog == null) return
        val ctx = context ?: return
        val isLarge = FloatWindowMetrics.isLargeScreen(ctx)
        val wasLarge = isLargeWindow
        isLargeWindow = isLarge

        // 宽屏 → 宽屏（尺寸变化），或 宽屏 → 非宽屏 / 非宽屏 → 宽屏（模式切换）
        if (isLarge) {
            val (w, h) = FloatWindowMetrics.calculateWindowSize(ctx)
            windowWidth = w
            windowHeight = h
            val marginPx = FloatWindowMetrics.dpToPx(ctx, FloatWindowMetrics.FLOAT_WINDOW_MARGIN_DP.toFloat())
            val dm = ctx.resources.displayMetrics
            // 仅首次进入宽屏或从非宽屏切回宽屏时，重置到贴侧初始位置；
            // 若用户已拖动过（wasLarge && hasWindowBeenDragged），保留当前位置
            if (!wasLarge || !hasWindowBeenDragged) {
                windowX = if (isLeftSide) marginPx else (dm.widthPixels - w - marginPx)
                windowY = (dm.heightPixels - h) / 2
            } else {
                // 保留用户位置，但 clamp 到新屏幕范围内
                windowX = windowX.coerceIn(0, (dm.widthPixels - w).coerceAtLeast(0))
                windowY = windowY.coerceIn(0, (dm.heightPixels - h).coerceAtLeast(0))
            }
        } else {
            // 宽屏 → 非宽屏：重置为满屏
            if (wasLarge) hasWindowBeenDragged = false
            windowX = 0
            windowY = 0
            windowWidth = 0
            windowHeight = 0
        }

        dialog?.window?.let { window ->
            if (isLargeWindow) {
                window.setLayout(windowWidth, windowHeight)
                window.setGravity(Gravity.START or Gravity.TOP)
                window.attributes = window.attributes.apply {
                    x = windowX
                    y = windowY
                }
                window.setBackgroundDrawable(
                    ContextCompat.getDrawable(ctx, R.drawable.expanding_window_background)
                        ?: Color.TRANSPARENT.toDrawable()
                )
                // 用 elevation + 圆角 outline 产生真阴影，替代 layer-list 假阴影（黑色硬边框）
                window.decorView.elevation = 16f
                window.decorView.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            } else {
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                window.setGravity(Gravity.CENTER)
                window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
        }

        if (::viewBinding.isInitialized) {
            if (isLargeWindow) {
                viewBinding.dragHandle.visibility = View.VISIBLE
                setupDraggable(viewBinding.dragHandle)
            } else {
                viewBinding.dragHandle.visibility = View.GONE
                viewBinding.dragHandle.setOnTouchListener(null)
            }
        }
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


    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggable(rootView: View) {
        val startX = windowX
        val startY = windowY
        var isDragging = false
        var downX = 0f
        var downY = 0f
        var initialWinX = startX
        var initialWinY = startY

        rootView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    initialWinX = windowX
                    initialWinY = windowY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!isDragging && (Math.abs(dx) > 8 || Math.abs(dy) > 8)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val newX = initialWinX + dx.toInt()
                        val newY = initialWinY + dy.toInt()
                        windowX = newX
                        windowY = newY
                        hasWindowBeenDragged = true
                        dialog?.window?.let { window ->
                            window.attributes = window.attributes.apply {
                                x = newX
                                y = newY
                            }
                            window.windowManager.updateViewLayout(window.decorView, window.attributes)
                        }
                    }
                    isDragging
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    false // 非拖动时让事件继续传递给子 View（如 WebView 内按钮点击）
                }
                else -> false
            }
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

    override fun onCallTypeChange(callType: Int) {
        sLogger.info("onCallTypeChange callType:$callType")
        viewBinding.webView.callHandler(
            MiniAppConstants.FUNCTION_CALL_TYPE_NOTIFY,
            arrayOf(JsonUtil.toJson(mapOf("callType" to callType)))
        )
    }

    override fun refreshCallInfo(callInfo: CallInfo) {
        this.callInfo = callInfo
        viewBinding.webView.callHandler(
            MiniAppConstants.FUNCTION_CALL_STATE_NOTIFY,
            arrayOf(JsonUtil.toJson(mapOf("state" to callInfo.state)))
        )
        if (callInfo.state == Call.STATE_HOLDING) {
            dismiss()
        }
    }
}