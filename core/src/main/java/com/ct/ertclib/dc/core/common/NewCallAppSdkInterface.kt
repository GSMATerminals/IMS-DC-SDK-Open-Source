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

package com.ct.ertclib.dc.core.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.common.sdkpermission.IPermissionCallback
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionHelper
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.FLOATING_DISPLAY
import com.ct.ertclib.dc.core.constants.CommonConstants.MINI_APP_LIST_PAGE_SIZE
import com.ct.ertclib.dc.core.constants.CommonConstants.PERCENT_CONSTANTS
import com.ct.ertclib.dc.core.constants.CommonConstants.SHARE_PREFERENCE_CONSTANTS
import com.ct.ertclib.dc.core.constants.CommonConstants.SHARE_PREFERENCE_STYLE_PARAMS
import com.ct.ertclib.dc.core.constants.ContextConstants.INTENT_MINI_EXPANDED
import com.ct.ertclib.dc.core.constants.MiniAppConstants.STYLE_DEFAULT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.STYLE_WHITE
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.common.FloatingBallData
import com.ct.ertclib.dc.core.data.common.Reason
import com.ct.ertclib.dc.core.data.event.MiniAppListGetEvent
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.miniapp.MiniAppStartManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.miniapp.db.MiniAppDbRepo
import com.ct.ertclib.dc.core.port.miniapp.IStartAppCallback
import com.ct.ertclib.dc.core.ui.activity.StyleSettingActivity
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import com.ct.ertclib.dc.core.utils.extension.startSettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

@SuppressLint("StaticFieldLeak")
object NewCallAppSdkInterface {

    private const val TAG = "NewCallAppSdkInterface"
    const val PERMISSION_TYPE_BEFORE_CALL = 1
    const val PERMISSION_TYPE_AFTER_CALL = 2
    const val PERMISSION_TYPE_IN_APP = 3

    const val SDK_PERCENT_CONSTANTS = PERCENT_CONSTANTS
    const val SDK_MINI_APP_LIST_PAGE_SIZE = MINI_APP_LIST_PAGE_SIZE

    const val SDK_INTENT_MINI_EXPANDED = INTENT_MINI_EXPANDED

    const val SDK_STYLE_WHITE = STYLE_WHITE

    const val SDK_FLOATING_DISPLAY = FLOATING_DISPLAY

    const val YI_SHARE_APP_ID = CommonConstants.DC_YI_SHARE

    const val DEBUG_LEVEL = "debug_level"
    const val ERROR_LEVEL = "error_level"
    const val WARN_LEVEL = "warn_level"
    const val INFO_LEVEL = "info_level"

    val closeExpandedViewFlow = MutableSharedFlow<Boolean>()

    val callInfoEventFlow = MutableSharedFlow<CallInfo>()

    val floatingBallStyle : MutableLiveData<Int> = MutableLiveData(STYLE_DEFAULT)

    val miniAppListEventFlow = MutableSharedFlow<MiniAppListGetEvent>()

    val floatingBallStatusFlow = MutableSharedFlow<FloatingBallData>()

    private var androidContext: Context? = null
    var floatPositionX: Int = 0
    var floatPositionY: Int = 0
    private val scope = CoroutineScope(Dispatchers.Default)
    private val miniAppDbRepo: MiniAppDbRepo by lazy { MiniAppDbRepo() }

    /**
     * 判断当前应用是否有5G增强通话相关的全部权限
     */
    @JvmStatic
    fun hasAllPermissions(context: Context): Boolean {
        return SDKPermissionUtils.hasAllPermissions(context).apply {
            LogUtils.debug(TAG, "hasAllPermissions: $this")
        }
    }

    /**
     * 开启或关闭5G增强通话功能
     */
    @JvmStatic
    fun setNewCallEnabled(isEnabled: Boolean) {
        LogUtils.debug(TAG, "setNewCallEnabled, isEnabled: $isEnabled")
        SDKPermissionUtils.setNewCallEnable(isEnabled)
    }

    /**
     * 启动5G增强通话权限设置页面
     */
    @JvmStatic
    fun checkAndRequestPermission(type: Int,onAgree: () -> Unit, onDenied: () -> Unit){
        val permissionHelper = SDKPermissionHelper(Utils.getApp(),object : IPermissionCallback {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onAgree() {
                onAgree()
            }
            override fun onDenied() {
                onDenied()
            }
        })
        permissionHelper.checkAndRequestPermission(type)
    }


    /**
     * 获取历史使用的小程序列表
     */
    @JvmStatic
    fun getHistoryMiniAppList(telecomId: String): MutableList<MiniAppInfo> {
        MiniAppManager.getAppPackageManager(telecomId)?.let {
            return it.historyStartAppList.apply {
                LogUtils.debug(TAG, "getHistoryMiniAppList, telecomId: $telecomId, listSize: ${this.size}")
            }
        }
        return mutableListOf<MiniAppInfo>().apply {
            LogUtils.debug(TAG, "getHistoryMiniAppList, list is empty")
        }
    }

    /**
     * 启动小程序，回参包含启动小程序结果回调以及下载进度回调
     */
    @JvmStatic
    fun startMiniApp(
        callId: String,
        appId: String,
        onStartResult: (String, Boolean, Reason?) -> Unit,
        onDownloadProgressUpdate: ((String, Int) -> Unit)? = null) {
        LogUtils.debug(TAG, "startMiniApp")
        MiniAppManager.getAppPackageManager(callId)?.startMiniApp(appId, object :
            IStartAppCallback {
            override fun onStartResult(appId: String, isSuccess: Boolean, reason: Reason?) {
                onStartResult.invoke(appId, isSuccess, reason)
            }

            override fun onDownloadProgressUpdated(appId: String, progress: Int) {
                onDownloadProgressUpdate?.invoke(appId, progress)
            }
        })
    }

    /**
     * 通话后直接启动小程序，不包含启动信息与下载信息结果回调
     */
    @JvmStatic
    fun startMiniAppOutOfCall(context: Context, miniAppInfo: MiniAppInfo) {
        LogUtils.debug(TAG, "startMiniAppOutOfCall, miniAPPName: ${miniAppInfo.appName}")
        MiniAppStartManager.startMiniApp(context, miniAppInfo, null,null)
    }

    /**
     *判断当前通话是否是视频通话
     */
    @JvmStatic
    fun isVideoCall(callId: String): Boolean {
        return MiniAppManager.isVideoCall(callId).apply {
            LogUtils.debug(TAG, "isVideoCall, callId: $callId, result: $this")
        }
    }

    /**
     *启动皮肤设置页面
     */
    @JvmStatic
    fun startStyleSettingActivity(context: Context) {
        LogUtils.debug(TAG, "startStyleSettingActivity")
        val intent = Intent(context, StyleSettingActivity::class.java)
        context.startActivity(intent)
    }

    /**
     *判断当前是否属于支持的场景，如果存在配置只能视频但当前非视频或者配置只能音频但当前非音频的情况，返回false
     */
    @JvmStatic
    fun supportScene(data: MiniAppInfo): Boolean {
        return MiniAppManager.supportScene(data).apply {
            LogUtils.debug(TAG, "supportScene: $this, appName: ${data.appName}")
        }
    }

    /**
     *判断当前是否属于支持的通话环节
     */
    @JvmStatic
    fun supportPhase(data: MiniAppInfo): Boolean {
        return MiniAppManager.supportPhase(data).apply {
            LogUtils.debug(TAG, "supportPhase: $this, appName: ${data.appName}")
        }
    }

    /**
     *判断当前是否属于支持DC
     */
    @JvmStatic
    fun supportDC(data: MiniAppInfo): Boolean {
        return MiniAppManager.supportDC(data).apply {
            LogUtils.debug(TAG, "supportDC: $this, appName: ${data.appName}")
        }
    }

    /**
     *从数据库中读取小程序列表
     */
    @JvmStatic
    fun getMiniAppListFromRepo(): List<MiniAppInfo> {
        return miniAppDbRepo.getAll().apply {
            LogUtils.debug(TAG, "getMiniAppListFromRepo, size: ${this.size}")
        }
    }

    /**
     * 获取当前的通话状态
     */
    @JvmStatic
    fun getCallState(callId: String?): Int? {
        return MiniAppManager.getAppPackageManager(callId)?.callState().apply {
            LogUtils.debug(TAG, "getCallState, callId: $callId, state: $this")
        }
    }

    /**
     * 变更callInfo
     * SDK调用
     */
    @JvmStatic
    fun emitCallInfoEventFlow(callInfo: CallInfo) {
        LogUtils.debug(TAG, "emitCallInfoEventFlow, callInfo: $callInfo")
        scope.launch {
            callInfoEventFlow.emit(callInfo)
        }
    }

    /**
     * 发送关闭小程序面板事件
     */
    @JvmStatic
    fun emitCloseExpandedViewFlow(isClose: Boolean) {
        LogUtils.debug(TAG, "emitCloseExpandedViewFlow, isClose: $isClose")
        scope.launch {
            closeExpandedViewFlow.emit(isClose)
        }
    }

    /**
     * 发送小程序获取事件，其中包括：
     * TO_REFRESH: 刷新事件
     * TO_LOADMORE: 加载更多小程序
     * ON_DOWNLOAD: 小程序下载事件
     */
    @JvmStatic
    fun emitAppListEvent(event: MiniAppListGetEvent) {
        LogUtils.debug(TAG, "emitAppListEvent, message: ${event.message}")
        scope.launch {
            miniAppListEventFlow.emit(event)
        }
    }

    @JvmStatic
    fun init(applicationContext: Context) {
        LogUtils.debug(TAG, "init")
        androidContext = applicationContext
        startKoin {
            androidContext(applicationContext)
            modules(coreModule)
        }
        floatPositionY = ScreenUtils.getScreenHeight(applicationContext) / 2
        scope.launch(Dispatchers.IO) {
            androidContext?.let {
                val sharePreference =
                    it.getSharedPreferences(SHARE_PREFERENCE_CONSTANTS, Context.MODE_PRIVATE)
                floatingBallStyle.postValue(
                    sharePreference.getInt(
                        SHARE_PREFERENCE_STYLE_PARAMS, STYLE_DEFAULT
                    )
                )
            }
        }
    }

    /**
     * 释放资源
     */
    @JvmStatic
    fun release() {
        LogUtils.debug(TAG, "release")
        scope.cancel()
    }

    /**
     * 打印log
     */
    @JvmStatic
    fun printLog(level: String, tag: String, trace: String) {
        when (level) {
            DEBUG_LEVEL -> {
                LogUtils.debug(tag, trace)
            }
            ERROR_LEVEL -> {
                LogUtils.error(tag, trace)
            }
            WARN_LEVEL -> {
                LogUtils.warn(tag, trace)
            }
            INFO_LEVEL -> {
                LogUtils.info(tag, trace)
            }
            else -> {
                LogUtils.fatal(tag, trace)
            }
        }
    }

    /**
     * 启动设置页面
     */
    @JvmStatic
    fun startSettingsActivity(context: Context) {
        LogUtils.debug(TAG, "startSettingsActivity")
        context.startSettingsActivity()
    }

    /**
     * 解析base64格式图片中隐写的内容
     */
    @JvmStatic
    fun parseImgData(img:String):String {
        LogUtils.debug(TAG, "parseImgData")
        return LicenseManager.getInstance().parseImgData(img)
    }

    /**
     * 记录翼分享子功能入口
     */
    @JvmStatic
    fun saveShareType(name:String) {
        LogUtils.debug(TAG, "saveShareType")
        SPUtils.getInstance().put("start_share_name_key",name)
    }

    /**
     * 获取翼分享子功能入口
     */
    @JvmStatic
    fun getShareType():String {
        LogUtils.debug(TAG, "getShareType")
        return SPUtils.getInstance().getString("start_share_name_key")
    }


}