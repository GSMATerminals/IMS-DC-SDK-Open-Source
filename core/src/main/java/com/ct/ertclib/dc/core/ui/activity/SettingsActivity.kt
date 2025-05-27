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

package com.ct.ertclib.dc.core.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface.PERMISSION_TYPE_IN_APP
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.databinding.ActivitySettingsBinding
import com.ct.ertclib.dc.core.ui.viewmodel.SettingsViewModel
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import com.ct.ertclib.dc.core.utils.common.PkgUtils
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.core.utils.extension.startLocalTestActivity

class SettingsActivity : BaseAppCompatActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        initView()
        updateView()
        viewModel.isCreated = true
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        if (FlavorUtils.getChannelName() == FlavorUtils.CHANNEL_LOCAL){
            binding.localTest.visibility = View.VISIBLE
        } else {
            binding.localTest.visibility = View.GONE
        }
        binding.versionTv.text = "V${PkgUtils.getAppVersion(this@SettingsActivity)}"
        binding.backIcon.setOnClickListener {
            finish()
        }
        binding.localTest.setOnClickListener {
            if (binding.swNewcall.isChecked){
                this.startLocalTestActivity()
            } else {
                ToastUtils.showShortToast(this@SettingsActivity,"请先开启5G增强通话")
            }
        }
        binding.swNewcall.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked){
                SDKPermissionUtils.setNewCallEnable(false)
                updateView()
            } else {
                viewModel.checkAndRequestPermission(this@SettingsActivity, PERMISSION_TYPE_IN_APP, ::onPermissionAgree, ::onPermissionDenied)
            }
        }
        binding.privacyPolicyTextview.setOnClickListener{
            SDKPermissionUtils.startPrivacyActivity(this@SettingsActivity)
        }
        binding.userTextview.setOnClickListener{
            SDKPermissionUtils.startUserServiceActivity(this@SettingsActivity)
        }
    }

    private fun onPermissionAgree() {
        updateView()
    }

    private fun onPermissionDenied() {
        updateView()
    }

    private fun updateView() {
        if (binding.swNewcall.isChecked != SDKPermissionUtils.hasAllPermissions(this)){
            binding.swNewcall.isChecked = SDKPermissionUtils.hasAllPermissions(this)
        }
        if (binding.swNewcall.isChecked){
            binding.closeTips.text = "开启表明您已经同意《隐私政策》和《用户协议》"
        } else {
            binding.closeTips.text = "关闭表明您不同意或撤回同意《隐私政策》和《用户协议》"
        }
    }

}