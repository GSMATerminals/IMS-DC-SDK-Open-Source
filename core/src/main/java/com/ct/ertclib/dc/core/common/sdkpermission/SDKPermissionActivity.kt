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

package com.ct.ertclib.dc.core.common.sdkpermission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.util.SizeUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.databinding.DialogRequestNewcallPermissionBinding
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.ui.activity.BaseAppCompatActivity
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions

/**
 * 问题：被叫首次使用增强通话，手机顶部悬浮窗显示电话，同时弹出权限授权弹窗。用户点击接听后，授权弹窗会被覆盖。
 * 解决方案：用户没有点击同意或拒绝按钮，Activity退至后台时，那就再调用一次startActivity
 *
 */
class SDKPermissionActivity : BaseAppCompatActivity() {
    companion object {
        fun startActivity(context: Context,type:Int) {
            val intent = Intent(context, SDKPermissionActivity::class.java)
            intent.putExtra(SDKPermissionUtils.PERMISSION_TYPE_KEY, type)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        private const val REQUEST_CODE_OVERLAY = 10000
        private const val TAG = "SDKPermissionActivity"
    }
    private val sLogger: Logger = Logger.getLogger(TAG)
    private var mDialog: AlertDialog? = null
    private var type:Int = NewCallAppSdkInterface.PERMISSION_TYPE_IN_APP
    private var userClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = intent.getIntExtra(SDKPermissionUtils.PERMISSION_TYPE_KEY,NewCallAppSdkInterface.PERMISSION_TYPE_IN_APP)
    }

    override fun onStart() {
        super.onStart()
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
    // 分三步：sdk控制开关-通话状态权限-悬浮窗权限
    private fun checkPermission(){
        dismissDialog()

        if (!SDKPermissionUtils.hasAllPermissions(this@SDKPermissionActivity)){
            val builder = AlertDialog.Builder(this@SDKPermissionActivity)
            builder.setCancelable(false)
            val layoutInflater = LayoutInflater.from(this@SDKPermissionActivity)
            val binding = DialogRequestNewcallPermissionBinding.inflate(layoutInflater)
            builder.setView(binding.root)
            mDialog = builder.create()
            val window = mDialog!!.window
            window?.let {
                val attributes = it.attributes
                attributes.gravity = Gravity.BOTTOM
                attributes.y = SizeUtils.dp2px(60f)
                it.attributes = attributes
                it.setBackgroundDrawableResource(R.drawable.dialog_window_background)
            }
            if (type == NewCallAppSdkInterface.PERMISSION_TYPE_BEFORE_CALL){
                binding.btnCancel.text = "稍后"
            } else if (type == NewCallAppSdkInterface.PERMISSION_TYPE_AFTER_CALL){
                binding.btnCancel.text = "取消"
            }
            if (SDKPermissionUtils.hasPhonePermissions(this@SDKPermissionActivity)){
                binding.contentTextview.text = ""
            }

            binding.privacyPolicyTextview.setOnClickListener{
                SDKPermissionUtils.startPrivacyActivity(this@SDKPermissionActivity)
            }
            binding.userTextview.setOnClickListener{
                SDKPermissionUtils.startUserServiceActivity(this@SDKPermissionActivity)
            }
            binding.btnCancel.setOnClickListener {
                userClicked = true
                mDialog!!.dismiss()
                if (sLogger.isDebugActivated) {
                    sLogger.debug("btnCancel")
                }
                denied()
            }
            binding.btnOk.setOnClickListener {
                if (!binding.privacyPolicyCheckBox.isChecked){
                    ToastUtils.showShortToast(this@SDKPermissionActivity,"请先阅读并勾选同意《隐私政策声明》")
                    return@setOnClickListener
                }
                userClicked = true
                SDKPermissionUtils.setNewCallEnable(true)
                mDialog!!.dismiss()
                XXPermissions.with(this@SDKPermissionActivity)
                    .permission(SDKPermissionUtils.PERMISSIONS)
                    .unchecked()
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                            if (sLogger.isDebugActivated) {
                                sLogger.debug("allGranted: $allGranted")
                            }
                            if (allGranted){
                                if (!Settings.canDrawOverlays(this@SDKPermissionActivity)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${packageName}")
                                    )
                                    startActivityForResult(intent, REQUEST_CODE_OVERLAY)
                                } else {
                                    agree()
                                }
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
                            if (doNotAskAgain) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", packageName, null)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            } else {
                                ToastUtils.showShortToast(this@SDKPermissionActivity, "缺少权限增强通话功能将无法使用")
                                denied()
                            }
                        }
                    })
            }
            mDialog!!.show()
        } else {
            if (sLogger.isDebugActivated) {
                sLogger.debug("AGREE")
            }
            agree()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sLogger.debug("AGREE")
        dismissDialog()
    }

    private fun dismissDialog() {
        if (mDialog?.isShowing == true) {
            mDialog?.dismiss()
        }
        mDialog = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!Settings.canDrawOverlays(this)) {
            ToastUtils.showShortToast(this@SDKPermissionActivity, "缺少权限增强通话功能将无法使用")
            denied()
        } else {
            agree()
        }
    }

    private fun agree(){
        StateFlowManager.emitPermissionAgreeFlow(isAgree = true)
        finishAndRemoveTask()
    }

    private fun denied(){
        StateFlowManager.emitPermissionAgreeFlow(isAgree = false)
        finishAndRemoveTask()
    }
}