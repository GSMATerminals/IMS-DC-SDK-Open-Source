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

package com.ct.ertclib.dc.app.manager

import android.annotation.SuppressLint
import android.content.Context
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.app.ui.view.MiniAppEntryHolder
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface.SDK_FLOATING_DISPLAY
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@SuppressLint("StaticFieldLeak")
object FloatingBallManager: KoinComponent {

    private const val TAG = "FloatingBallManager"

    private var entryHolder: MiniAppEntryHolder? = null

    private val context: Context by inject()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @JvmStatic
    fun init() {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "initManager")
        scope.launch(Dispatchers.Main) {
            NewCallAppSdkInterface.floatingBallStatusFlow.distinctUntilChanged().collect { floatingBallData ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "floatingBallStatusFlow status: ${floatingBallData.showStatus}")
                if (floatingBallData.showStatus == SDK_FLOATING_DISPLAY) {
                    floatingBallData.miniAppList?.let {
                        show(floatingBallData.callInfo, it, floatingBallData.style)
                    }
                } else {
                    dismiss()
                }
            }
        }
    }

    @JvmStatic
    fun release() {
        entryHolder = null
        scope.cancel()
    }

    private fun show(callInfo: CallInfo, miniAppList: MiniAppList, style: Int) {
        scope.launch(Dispatchers.Main) {
            if (entryHolder == null){
                entryHolder = MiniAppEntryHolder(context).apply {
                    this.callInfo = callInfo
                    this.miniAppList = miniAppList
                }
            }
            entryHolder?.show(style)
        }
    }

    private fun dismiss() {
        scope.launch(Dispatchers.Main) {
            entryHolder?.dismiss()
            entryHolder = null
        }
    }
}