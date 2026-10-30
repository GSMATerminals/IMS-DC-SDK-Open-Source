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

package com.ct.ertclib.dc.core.miniapp.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.common.MediaInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.databinding.ViewMiniAppBinding
import com.ct.ertclib.dc.core.manager.common.ExpandingCapacityManager
import com.ct.ertclib.dc.core.miniapp.bridge.CTWebChromeClient
import com.ct.ertclib.dc.core.miniapp.bridge.CTWebViewClient
import com.ct.ertclib.dc.core.miniapp.bridge.JSApi
import com.ct.ertclib.dc.core.miniapp.ui.viewmodel.MiniAppViewModel
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.ui.activity.MiniAppSettingActivity
import com.ct.ertclib.dc.core.utils.common.*
import com.ct.ertclib.dc.core.utils.logger.Logger
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import kotlin.math.abs
import kotlin.math.absoluteValue

class MiniAppView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), KoinComponent {

    companion object {
        private const val TAG = "MiniAppView"
        const val FLOAT_WINDOW_RADIUS_DP = FloatWindowMetrics.FLOAT_WINDOW_RADIUS_DP

        private const val FLOAT_WINDOW_RIGHT_MARGIN_DP = 20

        /** 半屏自定义窗口贴底时的顶部圆角半径（dp） */
        private const val HALF_SCREEN_TOP_RADIUS_DP = 16
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    private lateinit var binding: ViewMiniAppBinding
    lateinit var viewModel: MiniAppViewModel

    private var hasDataInit = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var onBarClickListener: ControlListener? = null
    private var stateHelper: MiniAppStateHelper? = null

    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isAttachedToWindow = false

    // 拖动相关
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f

    private var savedX = 0
    private var savedY = 0

    private var isFloatMode = false

    /** 当前是否处于半屏自定义窗口模式（非宽屏浮窗，但 isFloatMode 也为 true） */
    private var isCustomWindowMode = false

    private var isMinimized = false

    // 缓存宽屏状态
    private var isLargeScreenCached = false

    init {
        initView()
    }

    private fun initView() {
        binding = ViewMiniAppBinding.inflate(LayoutInflater.from(context), this, true)

        viewModel = MiniAppViewModel().apply {
            setEventCallbacks(
                onCallHandler = { method, args -> viewCallHandler(method, args) },
                onFinish = { finishAndKillMiniApp() },
                onSelectFiles = { maxSelectable, fileTypes, callback -> selectFilesInternal(maxSelectable, fileTypes, callback) },
                onSetPageName = { pageName -> setPageNameInternal(pageName) },
                onSetWindowStyle = { setViewWindowStyle() },
                onCallStateChanged = { state -> updateCoverViewVisibility(state) }
            )
        }

        setupWebView()
        setupListeners()
        setupWindowInsetsListener()
        observeViewModel()
        setupDrag()
        isLargeScreenCached = isLargeScreen()

        if (!isLargeScreenCached){
            setupBackGesture()
        }
    }

    /**
     * 应用浮窗圆角 - 仅当 isFloatMode 为 true 时生效
     */
    private fun applyFloatCornerRadius() {
        if (!isFloatMode) {
            // 如果不在浮窗模式，移除圆角
            removeCornerRadius()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                FLOAT_WINDOW_RADIUS_DP.toFloat(),
                resources.displayMetrics
            )

            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
            clipToOutline = true
        }
    }

    /**
     * 移除圆角
     */
    private fun removeCornerRadius() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = null
            clipToOutline = false
            elevation = 0f
        }
    }

    /**
     * 半屏自定义窗口圆角：贴底边时顶部圆角、底部无圆角；非贴底则无圆角
     */
    private fun applyHalfScreenCornerRadius() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        val params = layoutParams ?: run {
            removeCornerRadius()
            return
        }

        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        // 判断是否贴底边：窗口底部接近屏幕底部
        val windowBottom = params.y + params.height
        val isBottomAnchored = windowBottom >= screenHeight - params.height / 10

        if (!isBottomAnchored) {
            removeCornerRadius()
            return
        }

        val radius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            HALF_SCREEN_TOP_RADIUS_DP.toFloat(),
            displayMetrics
        )

        outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                // 上方两角圆角，下方两角无圆角：用 Path 构造圆角矩形
                val path = android.graphics.Path()
                val rect = android.graphics.RectF(0f, 0f, view.width.toFloat(), view.height.toFloat())
                val radii = floatArrayOf(
                    radius, radius,   // 左上
                    radius, radius,   // 右上
                    0f, 0f,           // 右下
                    0f, 0f            // 左下
                )
                path.addRoundRect(rect, radii, android.graphics.Path.Direction.CW)
                outline.setConvexPath(path)
            }
        }
        clipToOutline = true
    }

    private fun setupWebView() {
        binding.webView.apply {
            setupWebViewSettings()
            webViewClient = CTWebViewClient(this@MiniAppView)
            webChromeClient = CTWebChromeClient(this@MiniAppView)
            setDownloadListener(WebViewDownloadListener(context))
            addJavascriptObject(JSApi(this@MiniAppView), "")
            setBackgroundColor(Color.TRANSPARENT)
        }
        WebView.setWebContentsDebuggingEnabled(false)
    }

    private fun setupWebViewSettings() {
        binding.webView.settings.apply {
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
            binding.webView.isVerticalScrollBarEnabled = true
            binding.webView.isHorizontalScrollBarEnabled = false
        }
        // 开启硬件加速
        binding.webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
    }

    private fun setupListeners() {
        binding.ivBackground.setOnClickListener {
            minimize()
        }

        binding.ivBack.setOnClickListener {
            backPressed()
        }

        binding.ivSetting.setOnClickListener {
            openSettingActivity()
        }

        binding.ivClose.setOnClickListener {
            finishAndKillMiniApp()
        }

        binding.layoutMic.setOnClickListener {
            viewModel.toggleMic()
        }

        binding.iconHangUp.setOnClickListener {
            viewModel.hangup()
        }

        binding.layoutSpeaker.setOnClickListener {
            viewModel.toggleSpeaker()
        }

        binding.iconHangUpRinging.setOnClickListener {
            viewModel.hangup()
        }

        binding.iconAnswerRinging.setOnClickListener {
            viewModel.answer()
        }
    }

    private fun observeViewModel() {
        // 观察 UI 状态变化
        viewModel.micStatus.observe { status ->
            updateMicIcon(status)
        }

        viewModel.speakerStatus.observe { status ->
            updateSpeakerIcon(status)
        }

        viewModel.phoneButtonShowStatus.observe { status ->
            updatePhoneButtons(status)
        }
    }

    private fun updateMicIcon(status: Int) {
        scope.launch(Dispatchers.Main) {
            binding.iconMic.setImageDrawable(
                AppCompatResources.getDrawable(
                    context,
                    if (status == MiniAppViewModel.MIC_STATUS_MUTE) R.drawable.icon_mute_mic else R.drawable.icon_mic
                )
            )
        }
    }

    private fun updateSpeakerIcon(status: Int) {
        scope.launch(Dispatchers.Main) {
            binding.iconSpeaker.setImageDrawable(
                AppCompatResources.getDrawable(
                    context,
                    if (status == MiniAppViewModel.SPEAKER_STATUS_NORMAL) R.drawable.icon_speaker_off else R.drawable.icon_speaker_phone
                )
            )
        }
    }

    private fun updatePhoneButtons(status: Int) {
        scope.launch(Dispatchers.Main) {
            when (status) {
                MiniAppViewModel.PHONE_BUTTON_HIDE -> {
                    binding.phoneOperationLayout.visibility = GONE
                    binding.phoneOperationLayoutRinging.visibility = GONE
                }
                MiniAppViewModel.PHONE_BUTTON_RINGING -> {
                    binding.phoneOperationLayout.visibility = GONE
                    binding.phoneOperationLayoutRinging.visibility = VISIBLE
                }
                MiniAppViewModel.PHONE_BUTTON_CALLING -> {
                    binding.phoneOperationLayout.visibility = VISIBLE
                    binding.phoneOperationLayoutRinging.visibility = GONE
                }
            }
        }
    }

    private fun setupWindowInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            viewModel.notifyIMEHeight(imeHeight)
            insets
        }
    }

    /**
     * 判断是否为大屏设备（折叠屏展开状态/平板）
     * 使用屏幕宽度 >= 600dp 作为判断标准
     */
    private fun isLargeScreen(): Boolean {
        return FloatWindowMetrics.isLargeScreen(context)
    }

    /**
     * 将视图添加到 WindowManager
     */
    private fun attachToWindow() {
        if (isAttachedToWindow) return

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val displayMetrics = context.resources.displayMetrics
            // 判断是否为大屏设备（折叠屏展开/平板）
            val isLarge = isLargeScreen()

            layoutParams = WindowManager.LayoutParams().apply {
                if (isLarge && viewModel.miniAppInfo?.appProperties?.isWideScreenAdapted != true) {
                    // 折叠屏展开：浮窗模式
                    isFloatMode = true

                    width = calculateFloatWindowSize().first
                    height = calculateFloatWindowSize().second
                    val rightMarginPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        FLOAT_WINDOW_RIGHT_MARGIN_DP.toFloat(),
                        displayMetrics
                    ).toInt()

                    gravity = Gravity.START or Gravity.TOP
                    x = displayMetrics.widthPixels - width - rightMarginPx
                    y = (displayMetrics.heightPixels - height) / 2
                    flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

                } else {
                    // 单屏手机/折叠屏折叠状态
                    val windowStyle = viewModel.miniAppInfo?.appProperties?.windowStyle
                    val hasCustomWindow = windowStyle?.width != null
                            && windowStyle.height != null
                            && windowStyle.windowStart != null
                            && windowStyle.windowTop != null

                    if (hasCustomWindow) {
                        // 非宽屏自定义窗口模式
                        isFloatMode = true
                        isCustomWindowMode = true
                        val wRatio = if (windowStyle.width == -1f) 1.0f else windowStyle.width!!.coerceIn(0.1f, 1.0f)
                        val hRatio = if (windowStyle.height == -1f) 1.0f else windowStyle.height!!.coerceIn(0.1f, 1.0f)
                        width = (displayMetrics.widthPixels * wRatio).toInt()
                        height = (displayMetrics.heightPixels * hRatio).toInt()

                        gravity = Gravity.START or Gravity.TOP
                        x = (displayMetrics.widthPixels * windowStyle.windowStart!!).toInt()
                        y = (displayMetrics.heightPixels * windowStyle.windowTop!!).toInt()
                        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    } else {
                        // 全屏
                        isFloatMode = false
                        width = WindowManager.LayoutParams.MATCH_PARENT
                        height = WindowManager.LayoutParams.MATCH_PARENT
                        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    }
                }

                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                format = PixelFormat.TRANSLUCENT
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                alpha = 1f
            }

            windowManager?.addView(this, layoutParams)
            isAttachedToWindow = true

            layoutParams?.let { params ->
                savedX = params.x
                savedY = params.y
                sLogger.debug("Init saved position: ($savedX, $savedY)")
                updateViewLayout()
            }

            // 宽屏浮窗模式应用浮窗圆角，半屏自定义窗口应用半屏圆角
            if (isFloatMode && isLarge) {
                applyFloatCornerRadius()
            } else if (isCustomWindowMode) {
                applyHalfScreenCornerRadius()
            }

            sLogger.debug("View attached to window, isFloatMode: $isFloatMode")
        } catch (e: Exception) {
            sLogger.error("Failed to attach view", e)
        }
    }

    /**
     * 从 WindowManager 移除视图
     */
    fun detachFromWindow() {
        if (!isAttachedToWindow) return

        try {
            windowManager?.removeView(this)
            isAttachedToWindow = false
            isFloatMode = false
            isCustomWindowMode = false
            // 移除圆角
            removeCornerRadius()
            sLogger.debug("View detached from window")
        } catch (e: Exception) {
            sLogger.error("Failed to detach view", e)
        }
    }

    private fun updateViewLayout() {
        if (!isAttachedToWindow) return
        layoutParams?.let { params ->
            try {
                windowManager?.updateViewLayout(this, params)
            } catch (e: Exception) {
                sLogger.error("Failed to update view layout", e)
            }
        }
    }

    /**
     * 设置拖动功能 - 仅在大屏浮窗模式下生效
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag() {
        setOnTouchListener { view, event ->
            // 只在宽屏浮窗模式下处理拖动，半屏自定义窗口不可拖动
            if (!isFloatMode || !isLargeScreen()) {
                return@setOnTouchListener false
            }

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val dx = event.rawX - dragStartX
                        val dy = event.rawY - dragStartY

                        layoutParams?.let { params ->
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            updateViewLayout()
                        }

                        dragStartX = event.rawX
                        dragStartY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    layoutParams?.let { params ->
                        savedX = params.x
                        savedY = params.y
                        sLogger.debug("Drag ended, saved position: ($savedX, $savedY)")
                    }
                    isDragging = false
                    true
                }
                else -> false
            }
        }
    }

    fun start(
        miniApp: MiniAppInfo,
        callInfo: CallInfo?,
        miniAppListInfo: MiniAppList?
    ) {

        viewModel.miniAppInfo = miniApp
        viewModel.callInfo = callInfo
        viewModel.miniAppListInfo = miniAppListInfo

        attachToWindow()

        dealStateHelper()

        viewModel.init()

        initDataIfNeeded()
    }

    fun dealStateHelper(){
        // 宽屏模式不需要伴生 Activity
        if (isLargeScreen()) {
            stateHelper?.stopListen()
            stateHelper = null
            sLogger.debug("Large screen mode, StateHelper destroyed")
        } else {
            viewModel.miniAppInfo?.let { miniApp ->
                stateHelper = MiniAppStateHelper(context, miniApp.callId, miniApp.appId)
                stateHelper?.startListen(object : MiniAppStateHelper.Callback {
                    override fun onPause() {
                        sLogger.debug("onPause for: ${viewModel.miniAppInfo?.appId}")
                        viewToMinimize()
                    }

                    override fun onResume() {
                        sLogger.debug("onResume for: ${viewModel.miniAppInfo?.appId}")
                        viewToMaximize()
                    }

                    override fun onBackPressed() {
                        sLogger.debug("onBackPressed for: ${viewModel.miniAppInfo?.appId}")
                        backPressed()
                    }
                })
            }
            sLogger.debug("Non-large screen mode, StateHelper created")
        }
    }

    private fun initDataIfNeeded() {
        if (!hasDataInit) {
            hasDataInit = true
            scope.launch(Dispatchers.IO) {
                viewModel.miniAppInfo?.lastUseTime = System.currentTimeMillis()
                viewModel.miniAppInfo?.let { viewModel.saveMiniAppToDb(it) }
            }
            loadUrl()
        }
    }

    private fun loadUrl() {
        setViewWindowStyle()
        val path = viewModel.miniAppInfo?.path
        val params = if (!viewModel.miniAppInfo?.startByOthersParams.isNullOrEmpty()) {
            "?${viewModel.miniAppInfo?.startByOthersParams}"
        } else {
            ""
        }
        binding.webView.loadUrl("file://$path/index.html$params")
    }

    fun updateBack() {
        binding.ivBack.visibility = if (binding.webView.canGoBack()) VISIBLE else GONE
    }

    fun setViewWindowStyle() {
        val windowStyle = viewModel.miniAppInfo?.appProperties?.windowStyle
        if (windowStyle == null) {
            return
        }

        if (windowStyle.isFullScreen) {
            binding.topView.visibility = GONE
        } else {
            binding.topView.visibility = VISIBLE
            applyStatusBarColor(windowStyle.statusBarColor)
        }
        applyTitleBarColors(windowStyle.statusBarTitleColor)

        viewModel.updatePhoneButtonVisibility(
            showPhoneButton = viewModel.miniAppInfo?.appProperties?.showPhoneButton == true,
            callState = viewModel.callInfo?.state ?: Call.STATE_DISCONNECTED
        )
        updateBack()

        // 非宽屏自定义窗口模式：动态更新 LayoutParams
        if (isAttachedToWindow && !isLargeScreen()) {
            val hasCustomWindow = windowStyle.width != null
                    && windowStyle.height != null
                    && windowStyle.windowStart != null
                    && windowStyle.windowTop != null
            if (hasCustomWindow) {
                val displayMetrics = context.resources.displayMetrics
                val wRatio = if (windowStyle.width == -1f) 1.0f else windowStyle.width!!.coerceIn(0.1f, 1.0f)
                val hRatio = if (windowStyle.height == -1f) 1.0f else windowStyle.height!!.coerceIn(0.1f, 1.0f)
                layoutParams?.let { params ->
                    params.width = (displayMetrics.widthPixels * wRatio).toInt()
                    params.height = (displayMetrics.heightPixels * hRatio).toInt()
                    params.gravity = Gravity.START or Gravity.TOP
                    params.x = (displayMetrics.widthPixels * windowStyle.windowStart!!).toInt()
                    params.y = (displayMetrics.heightPixels * windowStyle.windowTop!!).toInt()
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    // 半屏自定义窗口应用半屏圆角
                    applyHalfScreenCornerRadius()
                    sLogger.debug("setViewWindowStyle updated custom window: ${params.width}x${params.height} at (${params.x}, ${params.y})")
                    // 半屏自定义窗口不应用圆角
                    sLogger.debug("setViewWindowStyle updated custom window: ${params.width}x${params.height} at (${params.x}, ${params.y})")
                }

                // closeGuidePage 仅在四字段全配时生效
                if (windowStyle.closeGuidePage == true) {
                    NewCallAppSdkInterface.emitStartAppCloseExpandedViewFlow(isClose = true)
                }
            }
        }
    }

    private fun String.toColorInt(defaultColor: Int = android.graphics.Color.BLACK): Int {
        return try {
            when {
                matches(Regex("^#[0-9A-Fa-f]{6}$")) -> {
                    android.graphics.Color.parseColor(this)
                }
                matches(Regex("^#[0-9A-Fa-f]{8}$")) -> {
                    val a = substring(1, 3).toInt(16)
                    val r = substring(3, 5).toInt(16)
                    val g = substring(5, 7).toInt(16)
                    val b = substring(7, 9).toInt(16)
                    android.graphics.Color.argb(a, r, g, b)
                }
                else -> {
                    sLogger.warn("Invalid color format: '$this', using default")
                    defaultColor
                }
            }
        } catch (e: Exception) {
            sLogger.error("Parse color failed: '$this'", e)
            defaultColor
        }
    }

    private fun applyStatusBarColor(color: String?) {
        color?.let {
            val statusBarColor = it.toColorInt()
            binding.topView.setBackgroundColor(statusBarColor)
        }
    }

    private fun applyTitleBarColors(titleColor: Int) {
        val isLight = titleColor == 1
        binding.tvPageName.setTextColor(if (isLight) Color.WHITE else Color.BLACK)
        binding.ivBack.setImageResource(if (isLight) R.drawable.icon_mini_back_white else R.drawable.icon_mini_back)
        binding.ivBackground.setImageResource(if (isLight) R.drawable.icon_mini_to_background_white else R.drawable.icon_mini_to_background)
        binding.ivSetting.setImageResource(if (isLight) R.drawable.icon_mini_setting_white else R.drawable.icon_setting_black)
        binding.ivClose.setImageResource(if (isLight) R.drawable.icon_mini_close_white else R.drawable.icon_mini_close)
    }
    private fun openSettingActivity() {
        // 打开设置页前先把浮窗移出屏幕，避免 TYPE_APPLICATION_OVERLAY 浮窗遮挡设置页
        viewToMinimize()
        val intent = Intent(context, MiniAppSettingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(CommonConstants.PARAMS_APP_ID, viewModel.miniAppInfo?.appId)
            putExtra(CommonConstants.PARAMS_CALL_ID, viewModel.miniAppInfo?.callId)
            viewModel.miniAppInfo?.path?.let { path ->
                putExtra(CommonConstants.PARAMS_VERSION_CODE, FileUtils.getLastPathName(path))
            }
        }
        context.startActivity(intent)
    }

    fun setPageNameInternal(pageName: String) {
        binding.tvPageName.text = if (pageName == "null" || pageName == "NULL") "" else pageName
    }

    fun finishAndKillMiniApp() {
        sLogger.debug("finishAndKillMiniAppActivity miniApp:${viewModel.miniAppInfo}")

        // activityManager.finishAllActivity()
        scope.launch {
            viewModel.notifyMiniAppState("onFinish")
            delay(500)
            binding.phoneOperationLayout.visibility = GONE
            binding.phoneOperationLayoutRinging.visibility = GONE
            // 先销毁 WebView 内部资源
            destroyWebView()
            if (viewModel.miniAppInfo?.appName == CommonConstants.DC_YI_SHARE) {
                NewCallAppSdkInterface.saveShareType("")
            }
            viewModel.callInfo?.telecomCallId?.let {
                viewModel.miniAppInfo?.appId?.let { appId ->
                    ExpandingCapacityManager.instance.unregisterECListener(context, it, appId)
                }
            }
            viewModel.onCleared()
            onBarClickListener?.onToRemove()
            stateHelper?.stopListen()
            stateHelper = null
            detachFromWindow()
        }
    }

    private fun destroyWebView() {
        sLogger.debug("destroyWebView")
        binding.webView.apply {
            stopLoading()
            clearHistory()
            removeJavascriptInterface("")
            clearCache(true)
            clearFormData()
            webChromeClient = null
            setDownloadListener(null)
        }
        (binding.webView.parent as? ViewGroup)?.removeView(binding.webView)
        binding.webView.destroy()
    }

    private fun viewCallHandler(method: String, args: Array<Any>) {
        sLogger.info("callHandler, method: $method args: ${args.joinToString()}")
        binding.webView.callHandler(method, args)
    }

    private fun updateCoverViewVisibility(callState: Int) {
        binding.coverView.visibility = when (callState) {
            Call.STATE_HOLDING -> VISIBLE
            Call.STATE_ACTIVE -> {
                if (viewModel.phoneButtonShowStatus.value == MiniAppViewModel.PHONE_BUTTON_RINGING) {
                    viewModel.updatePhoneButtonVisibility(
                        showPhoneButton = viewModel.miniAppInfo?.appProperties?.showPhoneButton != false,
                        callState = callState
                    )
                }
                GONE
            }
            else -> GONE
        }
    }

    private fun selectFilesInternal(maxSelectable: Int?, fileTypes: List<String>?, callback: OnPickMediaCallbackListener) {
        sLogger.debug("selectFiles")
        FilePickerHelper.pickFiles(
            context = context,
            maxSelectable = maxSelectable,
            fileTypes = fileTypes,
            callback = createFilePickCallback(callback)
        )
    }

    private fun createFilePickCallback(callback: OnPickMediaCallbackListener): FilePickerHelper.FilePickCallback {
        return object : FilePickerHelper.FilePickCallback {
            override fun onSuccess(mediaInfoList: List<MediaInfo>) {
                if (mediaInfoList.isNotEmpty()) {
                    callback.onResult(mediaInfoList)
                } else {
                    callback.onCancel()
                }
            }

            override fun onFailed() = callback.onCancel()
            override fun onCancel() = callback.onCancel()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 配置变化处理

        // 检测屏幕尺寸变化（折叠屏折叠/展开）
        val isLarge = isLargeScreen()

        // 宽屏状态发生变化
        if (isLargeScreenCached != isLarge) {
            isLargeScreenCached = isLarge
            // 最小化状态下销毁伴生 Activity，避免折叠屏切换时出现；非最小化时正常处理
            if (isMinimized) {
                stateHelper?.stopListen()
                stateHelper = null
                sLogger.debug("Minimized state, StateHelper destroyed on config change")
            } else {
                dealStateHelper()
            }
        }

        // 处理浮窗尺寸变化
        // 半屏自定义窗口模式下 isFloatMode 也为 true，需要额外用 isCustomWindowMode 区分
        if (isFloatMode != isLarge || (isCustomWindowMode && isLarge)) {
            binding.webView.post {
                handleScreenModeChange(isLarge)
            }
        }
    }

    /**
     * 处理屏幕模式切换（折叠<->展开）
     */
    private fun handleScreenModeChange(isLarge: Boolean) {
        if (!isAttachedToWindow) return

        layoutParams?.let { params ->
            if (isLarge && viewModel.miniAppInfo?.appProperties?.isWideScreenAdapted != true) {
                // 切换到浮窗模式
                isFloatMode = true
                isCustomWindowMode = false

                params.width = calculateFloatWindowSize().first
                params.height = calculateFloatWindowSize().second
                val rightMarginPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    FLOAT_WINDOW_RIGHT_MARGIN_DP.toFloat(),
                    context.resources.displayMetrics
                ).toInt()
                val displayMetrics = context.resources.displayMetrics

                params.gravity = Gravity.START or Gravity.TOP
                params.x = displayMetrics.widthPixels - params.width - rightMarginPx
                params.y = (displayMetrics.heightPixels - params.height) / 2

                // 恢复显示（如果之前被最小化）
                params.alpha = 1f
                params.flags = (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()) or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

                // 应用圆角
                applyFloatCornerRadius()

            } else {
                // 切换到非宽屏模式
                val windowStyle = viewModel.miniAppInfo?.appProperties?.windowStyle
                val hasCustomWindow = windowStyle?.width != null
                        && windowStyle.height != null
                        && windowStyle.windowStart != null
                        && windowStyle.windowTop != null
                val displayMetrics = context.resources.displayMetrics

                if (hasCustomWindow) {
                    // 非宽屏自定义窗口模式
                    isFloatMode = true
                    isCustomWindowMode = true
                    val wRatio = if (windowStyle.width == -1f) 1.0f else windowStyle.width!!.coerceIn(0.1f, 1.0f)
                    val hRatio = if (windowStyle.height == -1f) 1.0f else windowStyle.height!!.coerceIn(0.1f, 1.0f)
                    params.width = (displayMetrics.widthPixels * wRatio).toInt()
                    params.height = (displayMetrics.heightPixels * hRatio).toInt()
                    params.gravity = Gravity.START or Gravity.TOP
                    params.x = (displayMetrics.widthPixels * windowStyle.windowStart!!).toInt()
                    params.y = (displayMetrics.heightPixels * windowStyle.windowTop!!).toInt()

                    params.alpha = 1f
                    params.flags = (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()) or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

                    // 半屏自定义窗口应用半屏圆角
                    applyHalfScreenCornerRadius()

                } else {
                    // 切换到全屏模式
                    isFloatMode = false
                    isCustomWindowMode = false
                    params.width = WindowManager.LayoutParams.MATCH_PARENT
                    params.height = WindowManager.LayoutParams.MATCH_PARENT
                    params.gravity = Gravity.TOP or Gravity.START
                    params.x = 0
                    params.y = 0

                    // 恢复显示
                    params.alpha = 1f
                    params.flags = (params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()) or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

                    // 移除圆角
                    removeCornerRadius()
                }
            }

            // 如果当前处于最小化状态，保持最小化（不恢复 alpha 和触摸状态）
            if (isMinimized) {
                params.alpha = 0f
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }

            updateViewLayout()
            // 只在非拖动状态下执行这些
            if (!isDragging) {
                binding.webView.requestLayout()
                fixLayoutSize()
            }
        }

        layoutParams?.let { params ->
            // 最小化状态下不覆盖 savedX/savedY，保持之前保存的位置
            if (!isMinimized) {
                savedX = params.x
                savedY = params.y
                sLogger.debug("Init saved position: ($savedX, $savedY)")
            }
        }
    }

    fun setControlListener(listener: ControlListener) {
        onBarClickListener = listener
    }

    // 最小化界面
    fun minimize() {
        stateHelper?.minimizeActivity()
        viewToMinimize()
    }

    fun show(){
        stateHelper?.maximizeActivity()
        viewToMaximize()
    }

    private fun viewToMinimize() {
        scope.launch(Dispatchers.Main) {
            isMinimized = true
            // 收起系统软键盘
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(binding.webView.windowToken, 0)
            binding.webView.clearFocus()
            layoutParams?.let { params ->
                // 保存当前位置
                savedX = params.x
                savedY = params.y
                params.x = -params.width
                params.y = -params.height
                params.alpha = 0f
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

                updateViewLayout()
                sLogger.debug("View moved off-screen, saved: ($savedX, $savedY)")
            }
            viewModel.notifyMiniAppState("onBackground")
        }
    }

    private fun viewToMaximize() {
        scope.launch(Dispatchers.Main) {
            isMinimized = false
            // 重新根据当前屏幕尺寸计算窗口参数（折叠屏可能在最小化期间展开/收起）
            val isLarge = isLargeScreen()
            handleScreenModeChange(isLarge)
            sLogger.debug("View moved to screen, re-applied screen mode: isLarge=$isLarge")
            viewModel.notifyMiniAppState("onFront")
        }
    }

    private fun backPressed(){
        scope.launch(Dispatchers.Main){
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                finishAndKillMiniApp()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBackGesture() {
        val gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    return e.x < 50f  // 只在边缘区域拦截
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    // 检查起始点是否在边缘
                    if (e1 == null || e1.x >= 50f) {
                        return false
                    }

                    if (e2.x - e1.x > 100f && abs(velocityY) < 1000f) {
                        minimize()
                        return true
                    }
                    return false
                }
            })

        binding.webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false  // 不消费，让 WebView 继续处理
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                val exclusionRects = listOf(
                    Rect(0, 0, 100, height)  // 左侧100px区域禁用返回手势
                )
                ViewCompat.setSystemGestureExclusionRects(v, exclusionRects)
                insets
            }
        }
    }

    /**
     * 计算浮窗尺寸，固定宽高比，根据屏幕方向和比例决定撑满方向
     * 使用固定边距确保不遮挡系统栏
     */
    private fun calculateFloatWindowSize(): Pair<Int, Int> {
        return FloatWindowMetrics.calculateWindowSize(context)
    }

    /**
     * 固定所有布局尺寸，防止 density 变化时变大
     */
    private fun fixLayoutSize() {
        val density = context.resources.displayMetrics.density

        // 1. 标题栏高度 50dp
        val titleBarHeight = (50 * density).toInt()
        binding.topView.layoutParams.height = titleBarHeight
        binding.llTitleBar.layoutParams.height = titleBarHeight

        // 2. 标题文字 20sp → px
        binding.tvPageName.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20 * density)

        // 3. 返回按钮 50dp + padding 12dp
        val iconSize = (50 * density).toInt()
        val backPadding = (12 * density).toInt()
        binding.ivBack.layoutParams.width = iconSize
        binding.ivBack.layoutParams.height = iconSize
        binding.ivBack.setPadding(backPadding, backPadding, backPadding, backPadding)

        // 4. 右侧胶囊按钮 40dp x 32dp + padding 10dp/6dp
        val smallWidth = (40 * density).toInt()
        val smallHeight = (32 * density).toInt()
        val paddingHorizontal = (10 * density).toInt()
        val paddingVertical = (6 * density).toInt()
        listOf(binding.ivBackground, binding.ivSetting, binding.ivClose).forEach {
            it.layoutParams.width = smallWidth
            it.layoutParams.height = smallHeight
            it.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        }

        // 5. setting_layout margin
        val marginVertical = (7 * density).toInt()
        val marginEnd = (6 * density).toInt()
        val settingLayoutParams = binding.settingLayout.layoutParams as? ViewGroup.MarginLayoutParams
        settingLayoutParams?.let {
            it.topMargin = marginVertical
            it.bottomMargin = marginVertical
            it.marginEnd = marginEnd
            binding.settingLayout.layoutParams = it
        }

        // 6. 电话操作栏高度 80dp
        val phoneHeight = (80 * density).toInt()
        binding.phoneOperationLayout.layoutParams.height = phoneHeight
        binding.phoneOperationLayoutRinging.layoutParams.height = phoneHeight

        sLogger.debug("fixLayoutSize: density=$density")
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        sLogger.debug("dispatchKeyEvent${event?.keyCode}")
        if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
            backPressed()
            return false
        }
        return super.dispatchKeyEvent(event)
    }

    inner class WebViewDownloadListener(val context: Context) : android.webkit.DownloadListener {
        override fun onDownloadStart(
            url: String?,
            userAgent: String?,
            contentDisposition: String?,
            mimetype: String?,
            contentLength: Long
        ) {
            if (url.isNullOrEmpty() || !URLUtil.isNetworkUrl(url)) return

            val fileName = url.substring(url.lastIndexOf("/") + 1)
            val request = android.app.DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setMimeType(mimetype)
                .setDescription("下载")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            downloadManager.enqueue(request)
        }
    }

    interface ControlListener {
        fun onToRemove()
    }
}