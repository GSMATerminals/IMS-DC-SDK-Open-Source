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

package com.ct.ertclib.dc.core.manager.call

import android.annotation.SuppressLint
import com.ct.ertclib.dc.core.constants.CommonConstants.FLOATING_DISPLAY
import com.ct.ertclib.dc.core.data.common.FloatingBallData
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * 要处理多个通话得逻辑，约束条件是悬浮球只能有一个
 */
@SuppressLint("StaticFieldLeak")
object FloatingBallDataManager {

    private val floatingBallDataMap = ConcurrentHashMap<String, FloatingBallData>()
    private var refreshData: FloatingBallData? = null
    private val lock = Any()

    fun update(newData: FloatingBallData) {
        synchronized(lock) {
            // 1. 更新数据
            floatingBallDataMap[newData.callInfo.telecomCallId] = newData

            // 2. 查找需要显示的数据（优先显示 FLOATING_DISPLAY 状态的数据）
            val showData = findDisplayData() ?: newData

            // 3. 检查是否需要刷新
            if (shouldRefresh(showData)) {
                refreshData = showData
                emitData(showData)
            }
        }
    }

    private fun findDisplayData(): FloatingBallData? {
        // 优先返回 FLOATING_DISPLAY 状态的数据
        return floatingBallDataMap.values.find { it.showStatus == FLOATING_DISPLAY }
    }

    private fun shouldRefresh(newData: FloatingBallData): Boolean {
        return when {
            // 第一次刷新
            refreshData == null -> true
            // 显示状态变化
            refreshData?.showStatus != newData.showStatus -> true
            // 都是显示状态，但通话ID不同（切换了显示的通话）
            newData.showStatus == FLOATING_DISPLAY &&
                    refreshData?.callInfo?.telecomCallId != newData.callInfo.telecomCallId -> true
            // 其他情况不需要刷新
            else -> false
        }
    }

    private fun emitData(data: FloatingBallData) {
        if (FlavorUtils.getChannelName() == FlavorUtils.CHANNEL_DIALER) {
            StateFlowManager.emitDialerEntryDataFlow(data)
        } else {
            StateFlowManager.emitFloatingBallDataFlow(data)
        }
    }

    fun release() {
        synchronized(lock) {
            floatingBallDataMap.clear()
            refreshData = null
        }
    }
}