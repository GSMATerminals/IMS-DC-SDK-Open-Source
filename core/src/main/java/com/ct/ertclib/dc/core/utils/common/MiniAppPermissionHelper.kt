package com.ct.ertclib.dc.core.utils.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.ct.ertclib.dc.core.data.miniapp.MiniAppPermissions
import com.ct.ertclib.dc.core.data.miniapp.PermissionData
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.ui.widget.PermissionBottomSheetDialog
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
        private var permissionDialog: PermissionBottomSheetDialog? = null
        private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // 存储待处理的权限请求数据
        private var pendingSystemPermissions: MutableList<String>? = null
        private var pendingMiniAppPermissionsForSystem: MutableList<String>? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            overridePendingTransition(0, 0)
            // 设置透明主题
            setTheme(android.R.style.Theme_Translucent_NoTitleBar)
            super.onCreate(savedInstanceState)

            // 设置透明窗口
            window.setBackgroundDrawableResource(android.R.color.white)
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            window.setLayout(1, 1)

            // 获取参数
            miniAppInfo = intent.getParcelableExtra(EXTRA_MINI_APP_INFO) ?: run {
                sLogger.error("MiniAppInfo is null")
                finishWithResult(false)
                return
            }

            sLogger.info("PermissionActivity created, appId: ${miniAppInfo.appId}")

            // 开始权限检查流程
            startPermissionCheck()
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
                            showMiniAppPermissionDialog(pendingMiniAppPermissions)
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
                                showMiniAppPermissionDialog(pendingMini)
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

        private fun showMiniAppPermissionDialog(pendingPermissions: MutableList<String>) {
            sLogger.info("Showing mini app permission dialog")
            val permissionDataList = PermissionUtils.convertPermissionDataList(pendingPermissions)

            permissionDialog = PermissionBottomSheetDialog(
                this,
                miniAppInfo,
                permissionDataList,
                onPositiveButtonClick = { resultList ->
                    sLogger.info("Permission dialog result received")
                    handleMiniAppPermissionResult(resultList)
                },
                onNegativeButtonClick = {
                    sLogger.info("Permission dialog cancelled")
                    finishWithResult(false)
                }
            ).apply {
                setOnDismissListener {
                    // 确保在 dismiss 时清理
                    if (!isFinishing && !isDestroyed) {
                        finishWithResult(false)
                    }
                }
                show()
            }
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
            permissionDialog?.dismiss()
            permissionDialog = null

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
            permissionDialog = null
            sLogger.debug("PermissionActivity destroyed")
        }
    }
}