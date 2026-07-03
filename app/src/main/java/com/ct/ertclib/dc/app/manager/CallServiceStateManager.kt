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

package com.ct.ertclib.dc.app.manager

import android.content.Context
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.SystemUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object CallServiceStateManager {

    private const val TAG = "CallStateManager"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startListenCallServiceState(context: Context) {
        LogUtils.debug(TAG, "startListenCallState")
        scope.launch {
            if (SystemUtils.isMainProcess(context)) {
                NewCallAppSdkInterface.callServiceStateFlow.distinctUntilChanged().collect { state ->
                    LogUtils.debug(TAG, "collect callServiceStateFlow state: $state")
                    when (state) {
                        NewCallAppSdkInterface.CALL_SERVICE_START -> {
                            initAppModule()
                        }
                        NewCallAppSdkInterface.CALL_SERVICE_STOP -> {
                            releaseAppModule()
                        }
                    }
                }
            }
        }
    }

    private fun initAppModule() {
        FloatingBallManager.init()
    }

    private fun releaseAppModule() {
        FloatingBallManager.release()
    }
}