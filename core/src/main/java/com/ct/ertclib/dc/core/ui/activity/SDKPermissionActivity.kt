package com.ct.ertclib.dc.core.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.util.SizeUtils
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.databinding.DialogRequestNewcallPermissionBinding
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.utils.common.PkgUtils
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import androidx.core.net.toUri

/**
 * 问题：被叫首次使用增强通话，手机顶部悬浮窗显示电话，同时弹出权限授权弹窗。用户点击接听后，授权弹窗会被覆盖。
 * 解决方案：用户没有点击同意或拒绝按钮，Activity退至后台时，那就再调用一次startActivity
 *
 */
class SDKPermissionActivity : BaseAppCompatActivity() {
    companion object {
        fun startActivity(context: Context, type:Int) {
            val intent = Intent(context, SDKPermissionActivity::class.java)
            intent.putExtra(SDKPermissionUtils.PERMISSION_TYPE_KEY, type)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        private const val REQUEST_CODE_PERMISSION = 10000
        private const val TAG = "SDKPermissionActivity"
    }
    private val sLogger: Logger = Logger.getLogger(TAG)
    private var mDialog: AlertDialog? = null
    private var type:Int = NewCallAppSdkInterface.PERMISSION_TYPE_IN_APP
    private var userClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = intent.getIntExtra(SDKPermissionUtils.PERMISSION_TYPE_KEY, NewCallAppSdkInterface.PERMISSION_TYPE_IN_APP)
    }

    override fun onStart() {
        super.onStart()
        sLogger.debug("onStart")
        checkPermission()
    }

    override fun onStop() {
        super.onStop()
        if (sLogger.isDebugActivated) {
            sLogger.debug("onStop userClicked：$userClicked")
        }
        // 用户没有操作授权，则重新启动Activity
        if (!userClicked){
            startActivity(this,type)
        }
    }
    // 分三步：sdk控制开关-通话状态等权限-悬浮窗权限
    private fun checkPermission(){
        dismissDialog()

        if (!SDKPermissionUtils.hasAllPermissions(this@SDKPermissionActivity)){
            val builder = AlertDialog.Builder(this@SDKPermissionActivity)
            builder.setCancelable(false)
            val layoutInflater = LayoutInflater.from(this@SDKPermissionActivity)
            val binding = DialogRequestNewcallPermissionBinding.inflate(layoutInflater)
            builder.setView(binding.root)
            mDialog = builder.create()
            val window = mDialog?.window
            window?.let {
                val attributes = it.attributes
                attributes.gravity = Gravity.BOTTOM
                attributes.y = SizeUtils.dp2px(60f)
                it.attributes = attributes
                it.setBackgroundDrawableResource(R.drawable.dialog_window_background)
            }
            if (type == NewCallAppSdkInterface.PERMISSION_TYPE_BEFORE_CALL){
                binding.btnCancel.text = getString(R.string.later_btn)
            } else if (type == NewCallAppSdkInterface.PERMISSION_TYPE_AFTER_CALL){
                binding.btnCancel.text = getString(R.string.cancel_btn)
            }
            if (SDKPermissionUtils.isPrivacyChanged()){
                binding.titleTextview.text = getString(R.string.request_nc_permission_dialog_privacy_change_title)
            } else {
                binding.titleTextview.text = getString(R.string.request_nc_permission_dialog_title)
            }

            binding.privacyPolicyTextview.setOnClickListener{
                SDKPermissionUtils.startPrivacyActivity(this@SDKPermissionActivity)
            }
            binding.userTextview.setOnClickListener{
                SDKPermissionUtils.startUserServiceActivity(this@SDKPermissionActivity)
            }
            binding.btnCancel.setOnClickListener {
                userClicked = true
                dismissDialog()
                sLogger.debug("btnCancel")
                denied()
            }
            binding.btnOk.setOnClickListener {
                if (!binding.privacyPolicyCheckBox.isChecked){
                    ToastUtils.showShortToast(this@SDKPermissionActivity,getString(R.string.authorize_tips))
                    return@setOnClickListener
                }
                userClicked = true
                SDKPermissionUtils.setNewCallEnable(true)
                dismissDialog()
                // 有些厂商默认授予权限，这里要过滤一下，优化体验
                val notGrantedPermissions = SDKPermissionUtils.getNotGrantedPermissions(this@SDKPermissionActivity)
                if (notGrantedPermissions.isEmpty()){
                    checkOverlayPermission()
                } else {
                    XXPermissions.with(this@SDKPermissionActivity)
                        .permission(notGrantedPermissions)
                        .unchecked()
                        .request(object : OnPermissionCallback {
                            override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                                if (sLogger.isDebugActivated) {
                                    sLogger.debug("allGranted: $allGranted, canDrawOverlays:${Settings.canDrawOverlays(this@SDKPermissionActivity)}")
                                }
                                if (allGranted){
                                    checkOverlayPermission()
                                }
                                // 如果没有全部授权，就会走到onDenied
                            }

                            override fun onDenied(
                                permissions: MutableList<String>,
                                doNotAskAgain: Boolean
                            ) {
                                if (sLogger.isDebugActivated) {
                                    sLogger.debug("doNotAskAgain: $doNotAskAgain")
                                }
                                // 用户选择不再提示，跳转到应用详情页，用户可以手动开启权限
                                if (doNotAskAgain) {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.data = Uri.fromParts("package", packageName, null)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                } else {
                                    ToastUtils.showShortToast(this@SDKPermissionActivity, getString(R.string.miss_permission_tips))
                                    denied()
                                }
                            }
                        })
                }

            }
            mDialog?.show()
        } else {
            agree()
        }
    }

    fun checkOverlayPermission(){
        if (!Settings.canDrawOverlays(this@SDKPermissionActivity)) {
            // 悬浮窗授权，这里各个终端情况不一样
            val brand = PkgUtils.brand()
            sLogger.debug("brand:$brand")
            when(brand){
                PkgUtils.SAMSUNG -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", packageName, null)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra(":settings:fragment_args_key", "system_alert_window");
                    startActivity(intent)
                }

                PkgUtils.XIAOMI -> {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${packageName}".toUri())
                    intent.setClassName("com.android.settings","com.android.settings.SubSettings")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra(":settings:show_fragment", "com.android.settings.applications.appinfo.DrawOverlayDetails");
                    startActivity(intent)
                }
                else -> {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${packageName}".toUri())
                    startActivity(intent)
                }
            }
        } else {
            agree()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sLogger.debug("onDestroy")
        dismissDialog()
    }

    private fun dismissDialog() {
        if (mDialog?.isShowing == true) {
            mDialog?.dismiss()
        }
        mDialog = null
    }

    private fun agree(){
        sLogger.debug("AGREE")
        StateFlowManager.emitPermissionAgreeFlow(isAgree = true)
        finishAndRemoveTask()
    }

    private fun denied(){
        sLogger.debug("denied")
        StateFlowManager.emitPermissionAgreeFlow(isAgree = false)
        finishAndRemoveTask()
    }
}