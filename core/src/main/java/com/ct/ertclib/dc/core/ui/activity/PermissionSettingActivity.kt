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

import android.os.Bundle
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_APP_ID
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_CALL_ID
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_VERSION_CODE
import com.ct.ertclib.dc.core.data.miniapp.PermissionData
import com.ct.ertclib.dc.core.miniapp.ui.adapter.PermissionListAdapter
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.utils.common.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PermissionSettingActivity: BaseToolBarActivity(), KoinComponent {

    companion object {
        private const val TAG = "PermissionSettingActivity"
    }

    private val logger = Logger.getLogger(TAG)
    private val permissionMap = mutableMapOf<String, Boolean>()
    private val permissionUseCase: IPermissionUseCase by inject()
    private var recyclerView: RecyclerView? = null
    private var titleTips: TextView? = null
    private var adapter: PermissionListAdapter? = null
    private val permissionDataList: MutableList<PermissionData> = mutableListOf()
    private var appId = ""
    private var callId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.permission_setting_layout)
        initView()
        intent.getStringExtra(PARAMS_APP_ID)?.let {
            appId = it
        }
        intent.getStringExtra(PARAMS_CALL_ID)?.let {
            callId = it
        }
        lifecycleScope.launch(Dispatchers.IO) {
            permissionMap.clear()
            permissionMap.putAll(permissionUseCase.getPermission(appId))
            withContext(Dispatchers.Main) {
                val permissionDataList = PermissionUtils.convertMapToPermissionData(permissionMap)
                logger.info("onCreate, permissionDataList: $permissionDataList")
                if (permissionDataList.isEmpty()) {
                    titleTips?.isVisible = true
                } else {
                    titleTips?.isVisible = false
                    adapter?.submitList(permissionDataList)
                }
            }
        }
    }

    override fun getTooBarTitle(): String {
        return resources.getString(R.string.permission_preference_title)
    }

    private fun initView() {
        recyclerView = findViewById(R.id.permission_setting_recycler_view)
        titleTips = findViewById(R.id.permission_setting_tips)
        adapter = PermissionListAdapter(this, permissionDataList, ::onPermissionSelectedClick)
        recyclerView?.let {
            it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            it.adapter = adapter
        }
    }

    private fun onPermissionSelectedClick(position: Int, isAllowed: Boolean) {
        logger.info("onPermissionSelectedClick, position: $position, isAllowed: $isAllowed")
        permissionDataList[position].willBeGranted = isAllowed
        adapter?.submitItem(permissionDataList[position], position)
        lifecycleScope.launch(Dispatchers.IO) {
            permissionUseCase.savePermission(appId, PermissionUtils.convertPermissionDataToPermissionMap(permissionDataList), isMainProcess = true, callId)
        }
    }
}