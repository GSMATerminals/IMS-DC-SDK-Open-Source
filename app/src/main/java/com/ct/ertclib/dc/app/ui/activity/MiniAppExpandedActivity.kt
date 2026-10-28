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

package com.ct.ertclib.dc.app.ui.activity

import android.app.Activity
import android.app.KeyguardManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.ct.ertclib.dc.app.R
import com.ct.ertclib.dc.app.manager.FloatingBallManager
import com.ct.ertclib.dc.app.ui.fragment.BaseMiniAppListDialogFragment
import com.ct.ertclib.dc.app.ui.fragment.BootstrapDialogFragment
import com.ct.ertclib.dc.app.ui.fragment.MiniAppExpandedDialogFragment
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.model.AdItem
import com.ct.ertclib.dc.core.manager.common.LicenseManager
import com.ct.ertclib.dc.core.ui.activity.BaseFragmentActivity
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.utils.common.FloatWindowMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MiniAppExpandedActivity : BaseFragmentActivity() {

    companion object {
        private const val TAG = "MiniAppExpandedActivity"
        private const val ACTIVITY_ANIMATION_DELAY = 300L
    }

    private var mDialogFragment: BaseMiniAppListDialogFragment? = null
    private val handler = Handler(Looper.getMainLooper())
    private var callInfo: CallInfo? = null
    private var isLeftSide: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mini_app_expanded)

        callInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("callInfo", CallInfo::class.java)
        } else {
            intent.getParcelableExtra("callInfo") as CallInfo?
        }

        val adList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("adList", AdItem::class.java)
        } else {
            intent.getParcelableArrayListExtra<AdItem>("adList")
        }
        if (adList == null || callInfo == null){
            finish()
            return
        }
        isLeftSide = intent.getBooleanExtra("isLeftSide", true)
        // 宽屏时去掉 Activity 根布局的半透明黑遮罩，避免状态栏/全屏区域与小窗之间出现黑色割裂
        if (FloatWindowMetrics.isLargeScreen(this)) {
            findViewById<android.view.View>(R.id.expanded_activity_root)?.setBackgroundColor(Color.TRANSPARENT)
        }
        val miniAppList = callInfo?.telecomCallId?.let { NewCallAppSdkInterface.getMiniAppListInfo(it) }
        miniAppList?.let { appList ->
            val bootstrapPath = FileUtils.getLatestInstalledBootstrapPath()

            // 判断路径是否存在，并且License验证
            val isFolderValid = bootstrapPath != null && LicenseManager.getInstance().verifyMiniAppFolder(bootstrapPath)
            // val isFolderValid = bootstrapPath != null

            if (isFolderValid) {
                showMiniAppDialog(true, appList, adList, callInfo!!, isLeftSide)
            } else {
                showMiniAppDialog(false, appList, adList, callInfo!!, isLeftSide)
            }
        }

        startCollectFlow()
    }

    override fun onResume() {
        super.onResume()
        FloatingBallManager.setEntryVisible(false)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // 折叠屏展开/折叠时，通知 Dialog Fragment 重新计算窗口尺寸与位置
        mDialogFragment?.reapplyWindow()
        // 同步更新 Activity 根布局背景：宽屏透明、非宽屏半透明黑遮罩
        val root = findViewById<android.view.View>(R.id.expanded_activity_root)
        if (root != null) {
            root.setBackgroundColor(
                if (FloatWindowMetrics.isLargeScreen(this)) Color.TRANSPARENT else 0x88000000.toInt()
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        // 如果锁屏就请求用户解锁
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val locked = keyguardManager.isKeyguardLocked
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "is screen lock:$locked")
        if (locked){
            keyguardManager.newKeyguardLock("unLock")
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissError() {
                    super.onDismissError()
                }

                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                }

                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        FloatingBallManager.setEntryVisible(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }

    private fun showMiniAppDialog(
        isBootstrapAllowed: Boolean,
        miniAppList: MiniAppList,
        adList: ArrayList<AdItem>,
        callInfo: CallInfo,
        isLeftSide: Boolean
    ){
        // 宽屏小窗参数计算（非宽屏时 isLarge=false，保持满屏不变）
        val isLarge = FloatWindowMetrics.isLargeScreen(this)
        val winSize = if (isLarge) FloatWindowMetrics.calculateWindowSize(this) else null
        val marginPx = if (isLarge) FloatWindowMetrics.dpToPx(this, FloatWindowMetrics.FLOAT_WINDOW_MARGIN_DP.toFloat()) else 0
        val winX = if (isLarge && winSize != null) {
            if (isLeftSide) marginPx else (resources.displayMetrics.widthPixels - winSize.first - marginPx)
        } else 0
        val winY = if (isLarge && winSize != null) {
            (resources.displayMetrics.heightPixels - winSize.second) / 2
        } else 0

        mDialogFragment = if (!isBootstrapAllowed){
            MiniAppExpandedDialogFragment.newInstance(miniAppList, adList, callInfo)
        } else {
            BootstrapDialogFragment.newInstance(miniAppList, adList, callInfo)
        }
        mDialogFragment?.apply {
            if (isLarge) {
                setWindowSide(isLeftSide)
                setWindowPosition(winX, winY)
                setWindowSize(winSize!!.first, winSize.second)
            }
        }
        mDialogFragment?.setCallback(object : BaseMiniAppListDialogFragment.Callback {
            override fun onDismiss() {
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "mDialogFragment onDismiss")
                handler.postDelayed({
                    this@MiniAppExpandedActivity.finish()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        this@MiniAppExpandedActivity.overrideActivityTransition(
                            Activity.OVERRIDE_TRANSITION_CLOSE,
                            0,
                            0,
                            Color.TRANSPARENT
                        )
                    } else {
                        this@MiniAppExpandedActivity.overridePendingTransition(0, 0)
                    }
                }, ACTIVITY_ANIMATION_DELAY)
            }
        })
        mDialogFragment?.show(
            supportFragmentManager,
            BootstrapDialogFragment::class.simpleName
        )
    }

    private fun startCollectFlow() {
        lifecycleScope.launch(Dispatchers.Main) {
            NewCallAppSdkInterface.startAppCloseExpandedViewFlow.distinctUntilChanged().collect { isClose ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "closeExpandedViewFlow collect: $isClose")
                if (isClose) {
                    try {
                        mDialogFragment?.dismiss()
                    } catch (e: IllegalStateException) {
                        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.ERROR_LEVEL, TAG, "close miniapp fragment failed, message ${e.message}")
                    }
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            NewCallAppSdkInterface.callInfoEventFlow.collect { callInfo ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "collect callInfoEventFlow, event: $callInfo")
                this@MiniAppExpandedActivity.callInfo = callInfo
                mDialogFragment?.refreshCallInfo(callInfo)
            }
        }
        lifecycleScope.launch(Dispatchers.Main){
            NewCallAppSdkInterface.appStatusFlow.collect { appId ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "collect miniAppStatusChangeFlow, appId: $appId")
                mDialogFragment?.refreshAppStatus()
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            NewCallAppSdkInterface.bootstrapMessageFlow.collect { (label, message) ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "collect bootstrapMessageFlow, label: $label")
                mDialogFragment?.onBootstrapMessage(label, message)
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            NewCallAppSdkInterface.bootstrapAppDataChannelStateFlow.collect { (label, state) ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "collect bootstrapDataChannelStateFlow, label: $label")
                mDialogFragment?.onBootstrapAppDataChannelStateChange(label, state)
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            NewCallAppSdkInterface.audioDeviceChangeFlow.collect {
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "collect audioDeviceChangeFlow")
                mDialogFragment?.onAudioDeviceChange()
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            NewCallAppSdkInterface.callTypeChangeFlow.collect { (callId, callType) ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "collect callTypeChangeFlow, callType: $callType")
                mDialogFragment?.onCallTypeChange(callType)
            }
        }
    }
}
