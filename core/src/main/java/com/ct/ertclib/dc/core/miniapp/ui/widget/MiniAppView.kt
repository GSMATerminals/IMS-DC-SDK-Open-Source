package com.ct.ertclib.dc.core.miniapp.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.toColorInt
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
import com.ct.ertclib.dc.core.port.common.IActivityManager
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.ui.activity.MiniAppSettingActivity
import com.ct.ertclib.dc.core.utils.common.*
import com.ct.ertclib.dc.core.utils.logger.Logger
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs

class MiniAppView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), KoinComponent {

    companion object {
        private const val TAG = "MiniAppView"
    }

    private val activityManager: IActivityManager by inject()
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

    init {
        initView()
        setupBackGesture()
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
     * 将视图添加到 WindowManager
     */
    fun attachToWindow() {
        if (isAttachedToWindow) return

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                format = android.graphics.PixelFormat.TRANSLUCENT
                flags = 0
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                alpha = 1f
            }

            windowManager?.addView(this, layoutParams)
            isAttachedToWindow = true
            sLogger.debug("View attached to window")
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

    fun start(
        miniApp: MiniAppInfo,
        callInfo: CallInfo?,
        miniAppListInfo: MiniAppList?
    ) {

        viewModel.miniAppInfo = miniApp
        viewModel.callInfo = callInfo
        viewModel.miniAppListInfo = miniAppListInfo
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

        viewModel.init()

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        ScreenUnlockHelper.requestUnlock(context, object : ScreenUnlockHelper.UnlockCallback {
            override fun onSuccess() {
                checkMiniAppPermissions()
            }

            override fun onFailed() {
                finishAndKillMiniApp()
            }
        })
    }
    private fun checkMiniAppPermissions() {
        viewModel.miniAppInfo?.let {
            viewModel.checkMiniAppPermissions(
                context = context,
                miniAppInfo = it,
                onPermissionsResult = ::onPermissionsResult,
                onPermissionNotGranted = ::onPermissionNotGranted
            )
        }
    }

    private fun onPermissionsResult() {
        initDataIfNeeded()
    }

    private fun onPermissionNotGranted() {
        ToastUtils.showShortToast(context, R.string.permission_not_grant_tips)
        finishAndKillMiniApp()
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
            layoutParams?.let { params ->
                params.x = -params.width
                params.y = -params.height
                params.alpha = 0f
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

                updateViewLayout()
                sLogger.debug("View moved off-screen")
            }
            viewModel.notifyMiniAppState("onBackground")
        }
    }

    private fun viewToMaximize() {
        scope.launch(Dispatchers.Main) {
            layoutParams?.let { params ->
                params.x = 0
                params.y = 0
                params.alpha = 1f
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()

                updateViewLayout()
                sLogger.debug("View moved to screen")
            }
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