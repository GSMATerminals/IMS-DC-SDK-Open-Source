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

package com.ct.ertclib.dc.app.ui.fragment

import androidx.fragment.app.DialogFragment
import com.ct.ertclib.dc.core.data.call.CallInfo
abstract class BaseMiniAppListDialogFragment() : DialogFragment() {

    /** 宽屏小窗模式标记（非宽屏默认满屏，不变） */
    var isLargeWindow = false

    /** 宽屏小窗初始坐标（Gravity.START|TOP 语义） */
    var windowX = 0
    var windowY = 0

    /** 宽屏小窗尺寸 */
    var windowWidth = 0
    var windowHeight = 0

    /** 悬浮球所在侧（true=左侧，false=右侧），用于小窗初始位置计算 */
    var isLeftSide: Boolean = true

    /** 用户是否已手动拖动过宽屏小窗（配置变化时保留用户位置，仅重算尺寸） */
    var hasWindowBeenDragged: Boolean = false

    /** 设置宽屏小窗初始位置（需在 dialog window 就绪后调用） */
    fun setWindowPosition(x: Int, y: Int) {
        windowX = x
        windowY = y
    }

    /** 设置宽屏小窗尺寸 */
    fun setWindowSize(w: Int, h: Int) {
        windowWidth = w
        windowHeight = h
        isLargeWindow = true
    }

    /** 设置悬浮球所在侧（用于小窗贴侧定位） */
    fun setWindowSide(leftSide: Boolean) {
        isLeftSide = leftSide
    }

    /** 配置变化（折叠屏展开/折叠）时重新应用窗口参数。子类按需重写。 */
    open fun reapplyWindow() {}

    open fun refreshAppStatus() {
    }


    open fun refreshCallInfo(callInfo: CallInfo) {

    }

    open fun onBootstrapMessage(label: String, message: String) {

    }

    open fun onBootstrapAppDataChannelStateChange(label: String, status: Int) {

    }

    open fun onAudioDeviceChange() {

    }

    open fun onCallTypeChange(callType: Int) {

    }

    open fun setCallback(callback: Callback){

    }

    interface Callback {
        fun onDismiss()
    }
}