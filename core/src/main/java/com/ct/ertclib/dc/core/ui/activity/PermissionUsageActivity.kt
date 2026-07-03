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

package com.ct.ertclib.dc.core.ui.activity

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_APP_ID
import com.ct.ertclib.dc.core.data.miniapp.PermissionUsageData
import com.ct.ertclib.dc.core.databinding.LayoutPermissionUsageBinding
import com.ct.ertclib.dc.core.miniapp.ui.adapter.PermissionUsageAdapter
import com.ct.ertclib.dc.core.ui.viewmodel.PermissionUsageViewModel
import com.ct.ertclib.dc.core.ui.viewmodel.PermissionUsageViewModel.Companion.LAYOUT_TYPE_NONE
import com.ct.ertclib.dc.core.ui.viewmodel.PermissionUsageViewModel.Companion.LAYOUT_TYPE_NORMAL
import com.ct.ertclib.dc.core.utils.common.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class PermissionUsageActivity: BaseToolBarActivity(), KoinComponent {

    companion object {
        private const val TAG = "PermissionUsageActivity"
    }

    private lateinit var binding: LayoutPermissionUsageBinding
    private lateinit var viewModel: PermissionUsageViewModel
    private var adapter: PermissionUsageAdapter? = null
    private var appId = ""

    private val permissionUsageList = mutableListOf<PermissionUsageData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.debug(TAG, "onCreate")
        viewModel = ViewModelProvider(this)[PermissionUsageViewModel::class.java]
        binding = LayoutPermissionUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getStringExtra(PARAMS_APP_ID)?.let {
            appId = it
        }
        initView()
        initViewModel()
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.getPermissionUsageList(appId)
        }
    }

    override fun getTooBarTitle(): String {
        return resources.getString(R.string.permission_usage_title)
    }

    private fun initView() {
        adapter = PermissionUsageAdapter(this, permissionUsageList)
        binding.permissionUsageRecyclerView.let {
            it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            it.adapter = adapter
        }
    }

    private fun initViewModel() {
        viewModel.layoutType.observe(this) { type ->
            when (type) {
                LAYOUT_TYPE_NORMAL -> {
                    binding.permissionUsageRecyclerView.isVisible = true
                    binding.permissionUsageTitleNone.isVisible = false
                }
                LAYOUT_TYPE_NONE -> {
                    binding.permissionUsageRecyclerView.isVisible = false
                    binding.permissionUsageTitleNone.isVisible = true
                }
            }
        }
        viewModel.permissionUsageList.observe(this) { list ->
            adapter?.submitList(list)
        }
    }
}