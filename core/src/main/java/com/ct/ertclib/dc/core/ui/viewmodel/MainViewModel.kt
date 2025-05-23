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

package com.ct.ertclib.dc.core.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface


class MainViewModel: ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    var isCreated = false

    fun checkAndRequestPermission(context: Context, type: Int, onAgree: () -> Unit, onDenied: () -> Unit) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "checkAndRequestPermission, type: $type")
        NewCallAppSdkInterface.checkAndRequestPermission(type,onAgree,onDenied)
    }
}