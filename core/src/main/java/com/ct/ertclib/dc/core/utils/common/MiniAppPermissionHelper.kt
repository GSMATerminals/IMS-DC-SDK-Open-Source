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

package com.ct.ertclib.dc.core.utils.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.data.miniapp.MiniAppPermissions
import com.ct.ertclib.dc.core.data.miniapp.PermissionData
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.miniapp.ui.adapter.PermissionListAdapter
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 小程序权限管理 Helper
 * 通过启动透明 Activity 来处理权限请求和弹窗
 */
object MiniAppPermissionHelper : KoinComponent {

    private const val TAG = "MiniAppPermissionHelper"
    private const val EXTRA_MINI_APP_INFO = "extra_mini_app_info"

    private val sLogger = Logger.getLogger(TAG)
    private val permissionUseCase: IPermissionUseCase by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 权限检查结果回调
     */
    interface PermissionCallback {
        /**
         * 所有权限都已授权
         */
        fun onAllPermissionsGranted()

        /**
         * 权限被拒绝（系统权限或小程序权限被拒绝）
         */
        fun onPermissionDenied()
    }

    /**
     * 检查权限（启动透明 Activity 处理）
     * @param context 上下文
     * @param miniAppInfo 小程序信息
     * @param callback 回调
     */
    fun checkPermissions(
        context: Context,
        miniAppInfo: MiniAppInfo,
        callback: PermissionCallback
    ) {
        scope.launch(Dispatchers.IO) {
            val allPermissions = (miniAppInfo.appProperties?.permissions ?: emptyList())
                .filter { it in MiniAppPermissions.ALL_PERMISSIONS }
                .takeIf { it.isNotEmpty() }
                ?: run {
                    sLogger.info("No valid permissions")
                    withContext(Dispatchers.Main) { callback.onAllPermissionsGranted() }
                    return@launch
                }
            val grantedMiniAppPermissions = permissionUseCase.getPermission(miniAppInfo.appId)
            val pendingMiniAppPermissions = mutableListOf<String>()

            for (permission in allPermissions) {
                if (grantedMiniAppPermissions[permission] != true) {
                    pendingMiniAppPermissions.add(permission)
                }
            }
            if (pendingMiniAppPermissions.isEmpty()) {
                sLogger.info("All permissions already granted")
                withContext(Dispatchers.Main) {
                    callback.onAllPermissionsGranted()
                }
                return@launch
            }
            sLogger.info("Pending mini app permissions 1: $pendingMiniAppPermissions")
            scope.launch(Dispatchers.Main) {
                sLogger.info("Pending mini app permissions 2: $pendingMiniAppPermissions")
                val intent = Intent(context, PermissionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra(EXTRA_MINI_APP_INFO, miniAppInfo)
                }
                // 将回调传递给 Activity
                PermissionActivity.callback = callback
                context.startActivity(intent)
            }
        }
    }

    /**
     * 透明权限处理 Activity
     */
    class PermissionActivity : Activity() {

        companion object {
            private const val TAG = "PermissionActivity"
            var callback: PermissionCallback? = null
        }

        private val sLogger = Logger.getLogger(TAG)
        private lateinit var miniAppInfo: MiniAppInfo
        private var permissionAdapter: PermissionListAdapter? = null
        private var pendingPermissionDataList: MutableList<PermissionData>? = null
        private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // 视图引用（全屏授权页）
        private var appNameTextView: TextView? = null
        private var panelTitleView: TextView? = null
        private var permissionRecyclerView: RecyclerView? = null
        private var okButton: Button? = null
        private var cancelButton: TextView? = null

        // 存储待处理的权限请求数据
        private var pendingSystemPermissions: MutableList<String>? = null
        private var pendingMiniAppPermissionsForSystem: MutableList<String>? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            overridePendingTransition(0, 0)
            super.onCreate(savedInstanceState)
            setContentView(R.layout.permission_request_activity)

            // 获取参数
            miniAppInfo = intent.getParcelableExtra(EXTRA_MINI_APP_INFO) ?: run {
                sLogger.error("MiniAppInfo is null")
                finishWithResult(false)
                return
            }

            sLogger.info("PermissionActivity created, appId: ${miniAppInfo.appId}")
            initView()

            // 开始权限检查流程
            startPermissionCheck()
        }

        private fun initView() {
            appNameTextView = findViewById(R.id.permission_activity_app_name)
            panelTitleView = findViewById(R.id.request_permission_panel_title)
            permissionRecyclerView = findViewById(R.id.permission_recycler_view)
            okButton = findViewById(R.id.permission_ok_button)
            cancelButton = findViewById(R.id.permission_cancel_button)

            // 顶部展示小程序名称，让用户明确在为哪个小程序授权
            appNameTextView?.text = miniAppInfo.appName

            permissionRecyclerView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

            okButton?.setOnClickListener {
                sLogger.info("Permission page OK clicked")
                pendingPermissionDataList?.let { handleMiniAppPermissionResult(it) }
            }
            cancelButton?.setOnClickListener {
                sLogger.info("Permission page cancel clicked")
                finishWithResult(false)
            }
        }

        private fun startPermissionCheck() {
            activityScope.launch(Dispatchers.IO) {
                val allPermissions = (miniAppInfo.appProperties?.permissions ?: emptyList())
                    .filter { it in MiniAppPermissions.ALL_PERMISSIONS }
                    .takeIf { it.isNotEmpty() }
                    ?: run {
                        sLogger.info("No valid permissions")
                        withContext(Dispatchers.Main) { finishWithResult(true) }
                        return@launch
                    }

                val systemPermissions = PermissionUtils.convertToSystemPermissions(allPermissions)
                val grantedMiniAppPermissions = permissionUseCase.getPermission(miniAppInfo.appId)
                val pendingSystemPermissions = mutableListOf<String>()
                val pendingMiniAppPermissions = mutableListOf<String>()

                // 检查系统权限
                for (permission in systemPermissions) {
                    if (!permissionUseCase.isSystemPermissionGranted(permission)) {
                        pendingSystemPermissions.add(permission)
                    }
                }

                // 检查小程序权限
                for (permission in allPermissions) {
                    if (grantedMiniAppPermissions[permission] != true) {
                        pendingMiniAppPermissions.add(permission)
                    }
                }

                sLogger.info("Pending system permissions: $pendingSystemPermissions")
                sLogger.info("Pending mini app permissions: $pendingMiniAppPermissions")

                withContext(Dispatchers.Main) {
                    when {
                        pendingSystemPermissions.isNotEmpty() -> {
                            // 请求系统权限
                            requestSystemPermissions(
                                systemPermissions = pendingSystemPermissions,
                                pendingMiniAppPermissions = pendingMiniAppPermissions
                            )
                        }
                        pendingMiniAppPermissions.isNotEmpty() -> {
                            // 请求小程序权限
                            showMiniAppPermissionPage(pendingMiniAppPermissions)
                        }
                        else -> {
                            // 所有权限已授权
                            finishWithResult(true)
                        }
                    }
                }
            }
        }

        private fun requestSystemPermissions(
            systemPermissions: MutableList<String>,
            pendingMiniAppPermissions: MutableList<String>
        ) {
            sLogger.info("Requesting system permissions: $systemPermissions")

            // 存储待处理的数据
            this.pendingSystemPermissions = systemPermissions
            this.pendingMiniAppPermissionsForSystem = pendingMiniAppPermissions

            // 使用 XXPermissions 请求权限
            XXPermissions.with(this)
                .permission(systemPermissions)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                        if (allGranted) {
                            sLogger.info("System permissions granted")
                            val pendingMini = pendingMiniAppPermissionsForSystem
                            if (!pendingMini.isNullOrEmpty()) {
                                showMiniAppPermissionPage(pendingMini)
                            } else {
                                finishWithResult(true)
                            }
                        } else {
                            sLogger.info("Not all system permissions granted")
                            finishWithResult(false)
                        }

                        // 清理临时数据
                        this@PermissionActivity.pendingSystemPermissions = null
                        this@PermissionActivity.pendingMiniAppPermissionsForSystem = null
                    }

                    override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        sLogger.info("System permissions denied, never: $never")
                        finishWithResult(false)

                        // 清理临时数据
                        this@PermissionActivity.pendingSystemPermissions = null
                        this@PermissionActivity.pendingMiniAppPermissionsForSystem = null
                    }
                })
        }

        private fun showMiniAppPermissionPage(pendingPermissions: MutableList<String>) {
            sLogger.info("Showing mini app permission page")
            val permissionDataList = PermissionUtils.convertPermissionDataList(pendingPermissions)
            pendingPermissionDataList = permissionDataList

            panelTitleView?.text = "“${miniAppInfo.appName}”需要使用以下权限"
            permissionAdapter = PermissionListAdapter(this, permissionDataList) { position, isAllowed ->
                sLogger.info("Permission selected, position: $position, isAllowed: $isAllowed")
                permissionDataList[position].willBeGranted = isAllowed
                permissionAdapter?.submitItem(permissionDataList[position], position)
            }
            permissionRecyclerView?.adapter = permissionAdapter
        }

        private fun handleMiniAppPermissionResult(permissionDataList: List<PermissionData>) {
            val permissionMap = PermissionUtils.convertPermissionDataToPermissionMap(permissionDataList)
            val allGranted = permissionMap.values.all { it }

            // 保存授权结果到数据库
            activityScope.launch(Dispatchers.IO) {
                permissionUseCase.savePermission(
                    miniAppInfo.appId,
                    permissionMap,
                    isMainProcess = true
                )
                withContext(Dispatchers.Main) {
                    if (allGranted) {
                        finishWithResult(true)
                    } else {
                        finishWithResult(false)
                    }
                }
            }
        }

        private fun finishWithResult(success: Boolean) {
            sLogger.info("Finishing with result: $success")

            if (success) {
                callback?.onAllPermissionsGranted()
            } else {
                callback?.onPermissionDenied()
            }
            callback = null

            // 延迟 finish，确保回调执行完毕
            window.decorView.post {
                if (!isFinishing && !isDestroyed) {
                    finish()
                    overridePendingTransition(0, 0)
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            activityScope.cancel()
            permissionAdapter = null
            pendingPermissionDataList = null
            sLogger.debug("PermissionActivity destroyed")
        }
    }
}