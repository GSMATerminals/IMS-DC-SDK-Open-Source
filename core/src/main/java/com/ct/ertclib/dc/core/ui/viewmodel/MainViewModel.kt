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

package com.ct.ertclib.dc.core.ui.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.miniapp.db.MiniAppDbRepo
import com.ct.ertclib.dc.core.utils.common.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainViewModel: ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
        const val PAGE_STATUS_INIT = 0
        const val PAGE_STATUS_NOT_OPEN = 1
        const val PAGE_STATUS_NO_HISTORY = 2
        const val PAGE_STATUS_LIST = 3
        const val DELETE_STATUS_HIDE = 0
        const val DELETE_STATUS_SHOW = 1
        const val DELETE_STATUS_ENSURE = 2
    }

    val pageStatus = MutableLiveData(PAGE_STATUS_INIT)
    val miniAppInfos = MutableLiveData<List<MiniAppInfo>>()
    val deleteStatus = MutableLiveData(DELETE_STATUS_HIDE)

    private val miniAppDbRepo: MiniAppDbRepo by lazy { MiniAppDbRepo() }

    fun refreshStatus(context: Context) {
        if (!SDKPermissionUtils.hasAllPermissions(context)) {
            pageStatus.postValue(PAGE_STATUS_NOT_OPEN)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val list = miniAppDbRepo.getAll()
                miniAppInfos.postValue(list)
                if (list.isNotEmpty()) {
                    pageStatus.postValue(PAGE_STATUS_LIST)
                } else {
                    pageStatus.postValue(PAGE_STATUS_NO_HISTORY)
                }
            }
        }
    }

    fun deleteMiniAppInfo(position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            LogUtils.debug(TAG, "deleteMiniAppInfo, position: $position")
            miniAppInfos.value?.let { it ->
                if (position < it.size) {
                    val miniAppInfo = it[position]
                    miniAppInfos.postValue(miniAppInfos.value?.filterNot { it.appId == miniAppInfo.appId })
                    miniAppDbRepo.delete(miniAppInfo)
                }
            }
        }
    }
}