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

package com.ct.ertclib.dc.core.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ct.ertclib.dc.core.data.miniapp.PermissionUsageData
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PermissionUsageViewModel: ViewModel(), KoinComponent {

    companion object {
        private const val TAG = "PermissionUsageViewModel"
        const val LAYOUT_TYPE_NORMAL = 0
        const val LAYOUT_TYPE_NONE = 1
        const val SEVEN_DAYS_MILLS = 7 * 24 * 60 * 60 * 1000L
    }

    val layoutType = MutableLiveData(LAYOUT_TYPE_NORMAL)
    val permissionUsageList = MutableLiveData<MutableList<PermissionUsageData>>()
    private val permissionUseCase: IPermissionUseCase by inject()

    fun getPermissionUsageList(appId: String) {
        val permissionList = permissionUseCase.getPermissionUsage(appId, System.currentTimeMillis() - SEVEN_DAYS_MILLS)
        permissionUsageList.postValue(permissionList)
        if (permissionList.isEmpty()) {
            layoutType.postValue(LAYOUT_TYPE_NONE)
        } else {
            layoutType.postValue(LAYOUT_TYPE_NORMAL)
        }
    }
}