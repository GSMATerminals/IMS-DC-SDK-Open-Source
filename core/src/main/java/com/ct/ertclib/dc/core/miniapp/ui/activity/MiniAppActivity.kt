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

package com.ct.ertclib.dc.core.miniapp.ui.activity

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.telecom.Call
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.SizeUtils
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.miniapp.bridge.CTWebChromeClient
import com.ct.ertclib.dc.core.miniapp.bridge.CTWebViewClient
import com.ct.ertclib.dc.core.miniapp.bridge.JSApi
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.databinding.ActivityMiniAppBinding
import com.ct.ertclib.dc.core.utils.common.BitmapUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_APP_ID
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_CALL_ID
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_VERSION_CODE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_CALL_STATE_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_MINI_APP_NOTIFY
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.miniapp.db.MiniAppDbRepo
import com.ct.ertclib.dc.core.data.common.MediaInfo
import com.ct.ertclib.dc.core.data.miniapp.PermissionData
import com.ct.ertclib.dc.core.miniapp.ui.viewmodel.MiniAppViewModel
import com.ct.ertclib.dc.core.port.common.IActivityManager
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.port.manager.IMiniToParentManager
import com.ct.ertclib.dc.core.port.miniapp.IMiniApp
import com.ct.ertclib.dc.core.ui.activity.SettingActivity
import com.ct.ertclib.dc.core.utils.common.PermissionUtils
import com.ct.ertclib.dc.core.ui.widget.PermissionBottomSheetDialog
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.core.utils.logger.LogConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import wendu.dsbridge.DWebView
import androidx.core.graphics.toColorInt
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_AUDIO_DEVICE_NOTIFY
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.utils.common.PkgUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_IME_HEIGHT_NOTIFY

open class MiniAppActivity : AppCompatActivity(), IMiniApp, KoinComponent {

    companion object {
        private const val TAG = "MiniAppActivity"
    }

    val miniToParentManager: IMiniToParentManager by inject()

    private val sLogger: Logger = Logger.getLogger(TAG)

    override var miniApp: MiniAppInfo? = null
    override var callInfo: CallInfo? = null
    override var miniAppListInfo: MiniAppList? = null
    private lateinit var binding: ActivityMiniAppBinding
    private var onPickMediaCallbackListener: OnPickMediaCallbackListener? = null
    private var callState = Call.STATE_DISCONNECTED
    private var miniAppDbRepo:MiniAppDbRepo? = null
    private var hasAllPermission = false
    private var hasDataInit = false
    private lateinit var viewModel: MiniAppViewModel
    private var permissionDialog: PermissionBottomSheetDialog? = null
    private val activityManager: IActivityManager by inject()//本进程中的Activity，如小程序设置页面，小程序的Activity除外

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
        LogConfig.upDateLogEnabled()
        val processName = PkgUtils.getProcessName(this)
        val packageName = this.packageName
        if (sLogger.isDebugActivated) {
            sLogger.debug(
                "onCreate miniApp started, processName:${processName}," +
                        " packageName:${packageName}," +
                        " sp: ${SPUtils.getInstance().getBoolean(processName, false)}"
            )
        }
        if (!packageName.equals(processName) && !SPUtils.getInstance()
                .getBoolean(processName, false)
        ) {
            SPUtils.getInstance().put(processName, true)
            WebView.setDataDirectorySuffix(processName)
        }
        viewModel = ViewModelProvider(this)[MiniAppViewModel::class.java]
        miniAppDbRepo = MiniAppDbRepo()
        binding = ActivityMiniAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        miniToParentManager.miniAppInterface = this
        handleIntent(intent)
        initView()

        miniApp?.let {
            val appName = it.appName
            val appIcon = it.appIcon
            val icon = if (appIcon?.isEmpty() != false) {
                BitmapUtils.getBitmap(
                    AppCompatResources.getDrawable(
                        this,
                        R.drawable.icon_ct_dc_shortcut
                    )!!, 192, 192, Bitmap.Config.ARGB_8888
                )
            } else {
                try {
                    BitmapUtils.getBitmapFromBase64(appIcon)
                } catch (e: Exception) {
                    sLogger.error(e.message, e)
                    BitmapUtils.getBitmap(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.icon_ct_dc_shortcut
                        )!!, 192, 192, Bitmap.Config.ARGB_8888
                    )
                }
            }
            sLogger.info("onCreate appName:$appName, icon:$icon")
            setTaskDescription(ActivityManager.TaskDescription(appName, icon))
        }
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        sLogger.info("onNewIntent")
        intent?.let {
            handleIntent(it)
            // 小程序在后台被其他小程序唤起，且携带了参数，这时刷新小程序页面
            if (miniApp?.isStartByOthers == true && miniApp?.startByOthersParams != null) {
                loadUrl()
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        val parcelableExtra: Parcelable? = intent.getParcelableExtra("miniApp")

        if (parcelableExtra != null) {
            miniApp = parcelableExtra as MiniAppInfo
        }

        val callInfoParcelable: Parcelable? = intent.getParcelableExtra("callInfo")
        if (callInfoParcelable != null) {
            callInfo = callInfoParcelable as CallInfo
            callState = callInfo?.state!!
        }
        miniAppListInfo = intent.getParcelableExtra("miniAppListInfo") as? MiniAppList

        if (sLogger.isDebugActivated) {
            sLogger.debug("onCreate miniAppInfo :$miniApp")
            sLogger.debug("onCreate mCallInfo :$callInfo")
        }
    }

    private fun checkMiniAppPermissions(permissions: MutableList<PermissionData>) {
        if (permissions.isEmpty()) {
            hasAllPermission = true
            sLogger.info("checkMiniAppPermissions permissions is empty, return")
            initData()
            return
        } else {
            miniApp?.let {
                permissionDialog = PermissionBottomSheetDialog(this, it, permissions, ::checkMiniAppPermissionResults, ::onPermissionNotGranted).apply {
                    show()
                }
            }
        }
    }

    private fun checkMiniAppPermissionResults(permissionDataList: List<PermissionData>) {
        val permissionMap = PermissionUtils.convertPermissionDataToPermissionMap(permissionDataList)
        miniApp?.let {
            viewModel.savePermissionGrantedResults(it.appId, permissionMap)
        }
        val map = permissionMap.filterValues { !it }
        if (map.isEmpty()) {
            hasAllPermission = true
            initData()
        } else {
            onPermissionNotGranted()
            hasAllPermission = false
        }
        sLogger.info("checkMiniAppPermissionResults hasAllPermission: $hasAllPermission")
    }

    private fun onPermissionNotGranted() {
        ToastUtils.showShortToast(this@MiniAppActivity, R.string.permission_not_grant_tips)
        finishAndKillMiniAppActivity()
    }

    private fun initView() {
        sLogger.info("initView")
        binding.ivBackground.setOnClickListener {
            this.moveTaskToBack(true)
        }
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        binding.ivSetting.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java).apply {
                putExtra(PARAMS_APP_ID, miniApp?.appId)
                putExtra(PARAMS_CALL_ID, miniApp?.callId)
                miniApp?.path?.let { path ->
                    putExtra(PARAMS_VERSION_CODE, FileUtils.getLastPathName(path))
                }
            }
            startActivity(intent)
        }
        binding.ivClose.setOnClickListener {
            finishAndKillMiniAppActivity()
        }


        binding.webView.let {
            setWebViewSettings(it)
            it.setBackgroundColor(Color.TRANSPARENT)
            it.webViewClient = CTWebViewClient(this)
            it.webChromeClient = CTWebChromeClient(this)
            it.setDownloadListener(WebViewDownloadListener(this))
            it.addJavascriptObject(JSApi(this), "")
            it.setLayerType(View.LAYER_TYPE_HARDWARE,null)
        }
        WebView.setWebContentsDebuggingEnabled(false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            // 获取键盘的高度
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            notifyIMEHeight(imeHeight)

            // 继续分发 insets，保证其他 UI 逻辑不受影响
            insets
        }
    }

    private fun initData(){
        if(!hasDataInit){
            hasDataInit = true
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    miniApp?.lastUseTime = System.currentTimeMillis()
                    miniAppDbRepo?.upsert(miniApp!!)
                }
            }
            miniToParentManager.bindService(this)
        }
    }

    private fun setWebViewSettings(dWebView: DWebView) {
        val settings = dWebView.settings
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.displayZoomControls = false
        settings.builtInZoomControls = false
        settings.loadWithOverviewMode = true
        settings.textZoom = 100
        settings.loadsImagesAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false// video预览需要
        settings.userAgentString = settings.userAgentString + ";MiniAppContainer"
    }

    private fun notifyIMEHeight(height: Int){
        val map = mapOf("imeHeight" to height)
        callHandler(FUNCTION_IME_HEIGHT_NOTIFY, arrayOf(JsonUtil.toJson(map)))
    }

    private fun notifyMiniAppState(state:String){
        val map = mapOf("miniAppState" to state)
        callHandler(FUNCTION_MINI_APP_NOTIFY, arrayOf(JsonUtil.toJson(map)))
        sLogger.debug("notifyMiniAppState state:$state")
    }

    override fun setWindowStyle() {
        val windowStyle = miniApp?.appProperties?.windowStyle
        if (sLogger.isDebugActivated) {
            sLogger.debug("setWindowStyle windowStyle:$windowStyle")
        }
        //window应该占满屏幕
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        //胶囊要下移一下，否则会被状态栏遮挡
        val llTitleBarParams = binding.llTitleBar.layoutParams as ViewGroup.MarginLayoutParams
        llTitleBarParams.topMargin = ScreenUtils.getStatusBarHeight(this)
        binding.llTitleBar.layoutParams = llTitleBarParams

        val topViewParams = binding.topView.layoutParams as ViewGroup.MarginLayoutParams
        topViewParams.height = ScreenUtils.getStatusBarHeight(this) + SizeUtils.dp2px(50.0f)
        binding.topView.layoutParams = topViewParams

        val bottomViewParams = binding.bottomView.layoutParams as ViewGroup.MarginLayoutParams
        bottomViewParams.height = ScreenUtils.getNavigationBarHeight(this)
        binding.bottomView.layoutParams = bottomViewParams

        if (windowStyle == null) {
            return
        }
        //全屏
        if (windowStyle.isFullScreen) {
            binding.topView.visibility = View.GONE
            binding.bottomView.visibility = View.GONE
        } else {
            binding.topView.visibility = View.VISIBLE
            binding.bottomView.visibility = View.VISIBLE
            //非全屏时才需要配置状态栏、标题栏、导航栏背景颜色
            windowStyle.statusBarColor?.let {
                val statusBarColor = it.toColorInt()
                binding.topView.setBackgroundColor(statusBarColor)
                setStatusBarColor(statusBarColor)
            }
            windowStyle.navigationBarColor?.let {
                val navigationBarColor = it.toColorInt()
                binding.bottomView.setBackgroundColor(navigationBarColor)
                setNavBarColor(navigationBarColor)
            }
        }
        // 设置标题栏文字和图标颜色，只能黑白
        windowStyle.statusBarTitleColor.let {
            if (it == 1) {
                binding.tvPageName.setTextColor(Color.WHITE)
                binding.ivBack.setImageResource(R.drawable.icon_mini_back_white)
                binding.ivBackground.setImageResource(R.drawable.icon_mini_to_background_white)
                binding.ivSetting.setImageResource(R.drawable.icon_mini_setting_white)
                binding.ivClose.setImageResource(R.drawable.icon_mini_close_white)
            } else {
                binding.tvPageName.setTextColor(Color.BLACK)
                binding.ivBack.setImageResource(R.drawable.icon_mini_back)
                binding.ivBackground.setImageResource(R.drawable.icon_mini_to_background)
                binding.ivSetting.setImageResource(R.drawable.icon_setting_black)
                binding.ivClose.setImageResource(R.drawable.icon_mini_close)
            }
        }

        updateBack()
    }

    fun updateBack(){
        if (binding.webView.canGoBack()) {
            binding.ivBack.visibility = View.VISIBLE
        } else {
            binding.ivBack.visibility = View.GONE
        }
    }

    override fun setPageName(pageName: String) {
        // 防止尴尬的事情发生
        if (pageName == "null" || pageName =="NULL"){
            binding.tvPageName.text = ""
        } else {
            binding.tvPageName.text = pageName
        }
    }

    override fun onAudioDeviceChange() {
        callHandler(FUNCTION_AUDIO_DEVICE_NOTIFY, arrayOf())
    }

    override fun playVoice(path: String) {
        viewModel.playVoice(path)
    }

    override fun stopPlayVoice() {
        viewModel.stopPlayVoice()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onDestroy() {
        if (sLogger.isDebugActivated) {
            sLogger.debug("onDestroy miniApp:$miniApp, processName:${Application.getProcessName()}, activityName:${javaClass.simpleName}")
        }
        if (permissionDialog?.isShowing == true) {
            permissionDialog?.dismiss()
        }
        miniToParentManager.unBindService(this@MiniAppActivity)
        if (miniApp?.appName == CommonConstants.DC_YI_SHARE){
            NewCallAppSdkInterface.saveShareType("")
        }
        stopPlayVoice()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        notifyMiniAppState("onBackground")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStart() {
        super.onStart()
        if (!hasAllPermission){
            miniApp?.let {
                viewModel.startGrantPermission(this, it, ::checkMiniAppPermissions, ::finishAndKillMiniAppActivity)

            }
        }
    }

    override fun onResume() {
        super.onResume()
        notifyMiniAppState("onFront")
    }

    override fun onBackPressed() {
        setPageName("")
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            finishAndKillMiniAppActivity()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (permissionDialog?.isShowing == true) {
            permissionDialog?.reshow()
        }
    }


    fun onMiniAppLoaded() {
        sLogger.info("onMiniAppLoaded")
    }

    private fun setStatusBarColor(statusBarColor: Int) {
        if ((statusBarColor and 0xffffff) < 0x777777) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() //白色字体
        } else {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR //黑色字体
        }
    }

    private fun setNavBarColor(navigationBarColor: Int) {
        if ((navigationBarColor and 0xffffff) < 0x777777) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv() //白色按钮
        } else {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR //黑色按钮
        }
    }

    inner class WebViewDownloadListener(val context: Context) : DownloadListener {
        override fun onDownloadStart(
            url: String?,
            userAgent: String?,
            contentDisposition: String?,
            mimetype: String?,
            contentLength: Long
        ) {
            if (sLogger.isDebugActivated) {
                sLogger.debug(
                    "WebViewDownloadListener onDownloadStart, url:$url," +
                            " userAgent:$userAgent, contentDisposition:$contentDisposition," +
                            " minetype:$mimetype, contentLength:$contentLength"
                )
            }
            if (url.isNullOrEmpty()){
                if (sLogger.isDebugActivated){
                    sLogger.debug("Url is null.")
                }
                return
            }
            if (!URLUtil.isNetworkUrl(url)){
                if (sLogger.isDebugActivated){
                    sLogger.debug("Illegal url: $url")
                }
                return
            }
            val fileName = url.substring(url.lastIndexOf("/") + 1)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setMimeType(mimetype)
                .setDescription("下载")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        }
    }

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        sLogger.debug("onActivityResult requestCode:$requestCode resultCode:$resultCode ${data?.data}")
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            try {
                data?.data?.let { uri ->
                    val size = FileUtils.getFileSizeFromUri(this,uri) // 使用自定义的方法获取文件路径
                    sLogger.debug("onActivityResult uri ${uri}, size $size")
                    if (onPickMediaCallbackListener!=null){
                        val mediaInfo = MediaInfo()
                        mediaInfo.path = uri.toString()
                        mediaInfo.displayName = FileUtils.getFileNameFromUri(this,uri)
                        mediaInfo.size = FileUtils.getFileSizeFromUri(this,uri)
                        mediaInfo.lastModified = FileUtils.getFileLastModifiedFromUri(this,uri)
                        mediaInfo.isDirectory = false
                        onPickMediaCallbackListener!!.onResult(listOf(mediaInfo))
                    }
                }
            }catch (e :Exception){
                sLogger.debug("selectFile onActivityResult exception${e.stackTraceToString()}")
            }

        }
    }

    override fun finishAndKillMiniAppActivity() {
        sLogger.debug("finishAndKillMiniAppActivity miniApp:$miniApp")
        activityManager.finishAllActivity()
        lifecycleScope.launch(Dispatchers.Main) {
            // 1秒之后释放资源
            notifyMiniAppState("onFinish")
            delay(500)
            miniToParentManager.unBindService(this@MiniAppActivity)
            finishAndRemoveTask()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    override fun callHandler(method: String, args: Array<Any>) {
        sLogger.info("callHandler, method: $method")
        binding.webView.callHandler(method, args)
    }

    override fun invokeOnServiceConnected() {
        sLogger.debug("onServiceConnected")
        loadUrl()
    }

    private fun loadUrl() {
        setWindowStyle()
        val path = miniApp?.path
        sLogger.debug("loadUrl path:$path, param:${miniApp?.startByOthersParams}")
        val params = if (!miniApp?.startByOthersParams.isNullOrEmpty()){"?${miniApp?.startByOthersParams}"}else{""}
        binding.webView.loadUrl("file://$path/index.html${params}")
    }

    override fun invokeOnCallStateChange(params: Map<String, Any?>) {
        val callState = params["callState"].toString().toFloatOrNull()?.toInt()
        LogUtils.debug(TAG, "invokeOnCallStateChange params: $params, callState: $callState, mCallState: ${this.callState}")
        if (callState!= null && this.callState != callState){
            this.callState = callState
            callInfo?.state = callState
            lifecycleScope.launch(Dispatchers.Main) {
                if (sLogger.isDebugActivated) {
                    sLogger.debug("IParentToMini.Stub sendMessageToMini mCallState:${this@MiniAppActivity.callState}")
                }
                val map = mapOf("callState" to this@MiniAppActivity.callState)
                callHandler(FUNCTION_CALL_STATE_NOTIFY, arrayOf(JsonUtil.toJson(map)))
                // 呼叫保持的时候显示遮罩，webView内容不可点
                if (this@MiniAppActivity.callState == Call.STATE_HOLDING) {
                    binding.coverView.visibility = View.VISIBLE
                } else {
                    binding.coverView.visibility = View.GONE
                }
            }
        }
    }
    override fun invokeOnCheckAlive() {
        if (isFinishing) {
            finishAndKillMiniAppActivity()
        }
    }

    override fun selectFile(callback: OnPickMediaCallbackListener){
        onPickMediaCallbackListener = callback
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // 选择所有类型的文件
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "选择文件"), 1000)
    }

    override fun refreshPermission() {
        miniApp?.appId?.let {
            viewModel.refreshPermission(it)
        }
    }
}