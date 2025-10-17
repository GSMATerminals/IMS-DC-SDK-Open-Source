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

package com.ct.ertclib.dc.core.ui.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.databinding.ActivityMainBinding
import com.ct.ertclib.dc.core.ui.fragment.MainMiniAppListFragment
import com.ct.ertclib.dc.core.ui.viewmodel.MainViewModel
import com.ct.ertclib.dc.core.utils.extension.startSettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : BaseAppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val mainMiniAppListFragment = MainMiniAppListFragment()
    private var isOnCreateUpdateView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        initView()

        // 不干涉通话流程，在这里更新一下版本号，在检查权限时做判断。可能在下次通话时才会提示用户。
        SDKPermissionUtils.updatePrivacyVersion()
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)  // 协程延迟1000毫秒（1秒）
            updateView()
            isOnCreateUpdateView = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (isOnCreateUpdateView){
            updateView()
        }
    }

    private fun initView() {
        binding.btnSetting.setOnClickListener {
            this@MainActivity.startSettingsActivity()
        }
        binding.btnOpen.setOnClickListener {
            this@MainActivity.startSettingsActivity()
        }
        if (!viewModel.isCreated) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, mainMiniAppListFragment)
                .commit()
        }
        viewModel.isCreated = true
    }


    private fun updateView() {
        if (SDKPermissionUtils.hasAllPermissions(this@MainActivity)){
            binding.contentLayout.visibility = View.VISIBLE
            binding.tipsLayout.visibility = View.GONE
        } else {
            binding.contentLayout.visibility = View.GONE
            binding.tipsLayout.visibility = View.VISIBLE
        }
    }
}

