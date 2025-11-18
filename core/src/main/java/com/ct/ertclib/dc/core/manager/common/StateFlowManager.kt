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

package com.ct.ertclib.dc.core.manager.common


import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.data.common.CallStateData
import com.ct.ertclib.dc.core.data.common.FloatingBallData
import com.ct.ertclib.dc.core.data.event.CloseAdcEvent
import com.ct.ertclib.dc.core.data.event.UpdateDownloadProgressEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

object StateFlowManager: KoinComponent {

    //liveData与flow状态管理类，监听对应事件变化，当监听到事件发生时，通过collect操作去执行对应操作
    //在flow使用collect操作时，请务必注意scope.cancel的回调时机，否则在进程未被杀掉的场景下，flow可能会出现频繁回调的现象。

    val chatBoxUpdateDownloadProgress = MutableSharedFlow<UpdateDownloadProgressEvent>()

    val closeAdcEventFlow =  MutableSharedFlow<CloseAdcEvent>()

    val dialerEntryStatusFlow = MutableSharedFlow<FloatingBallData>()

    val callInfoFlow = MutableSharedFlow<CallStateData>()

    val permissionAgreeFlow = MutableSharedFlow<Boolean>()


    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @JvmStatic
    fun emitUpdateDownloadProgress(event: UpdateDownloadProgressEvent) {
        scope.launch {
            chatBoxUpdateDownloadProgress.emit(event)
        }
    }

    @JvmStatic
    fun emitPermissionAgreeFlow(isAgree: Boolean) {
        scope.launch {
            permissionAgreeFlow.emit(isAgree)
        }
    }


    @JvmStatic
    fun emitCloseAdcEvent(closeAdcEvent: CloseAdcEvent) {
        scope.launch {
            closeAdcEventFlow.emit(closeAdcEvent)
        }
    }

    @JvmStatic
    fun emitFloatingBallDataFlow(floatBallData: FloatingBallData) {
        scope.launch {
            NewCallAppSdkInterface.floatingBallStatusFlow.emit(floatBallData)
        }
    }

    @JvmStatic
    fun emitDialerEntryDataFlow(floatBallData: FloatingBallData) {
        scope.launch {
            dialerEntryStatusFlow.emit(floatBallData)
        }
    }

    @JvmStatic
    fun emitCallInfoFlow(callStateData: CallStateData) {
        scope.launch {
            callInfoFlow.emit(callStateData)
        }
    }
}