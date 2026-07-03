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

package com.ct.ertclib.dc.core.manager.common

import android.content.Context
import com.ct.ertclib.dc.core.ui.widget.ConfirmOverlayWindow
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

object ConfirmOverlayManager {
    private var currentWindow: WeakReference<ConfirmOverlayWindow>? = null
    private val isShowingFlag = AtomicBoolean(false)

    @Synchronized
    fun startConfirm(
        context: Context,
        message: String,
        callback: ConfirmOverlayWindow.ConfirmCallback?,
        accept: String? = null,
        cancel: String? = null
    ) {
        // 使用标志位和同步锁双重检查
        if (isShowingFlag.get()) {
            return
        }

        // 检查现有窗口
        if (isShowing()) {
            return
        }

        isShowingFlag.set(true)

        try {
            val window = ConfirmOverlayWindow(context.applicationContext)

            // 包装回调，在窗口关闭时重置标志
            val wrappedCallback = object : ConfirmOverlayWindow.ConfirmCallback {
                override fun onAccept() {
                    callback?.onAccept()
                    resetState()
                }

                override fun onCancel() {
                    callback?.onCancel()
                    resetState()
                }
            }

            window.show(message, wrappedCallback, accept, cancel)
            currentWindow = WeakReference(window)

        } catch (e: Exception) {
            // 如果显示失败，重置标志
            resetState()
        }
    }

    @Synchronized
    fun dismiss() {
        currentWindow?.get()?.dismiss()
        resetState()
    }

    fun isShowing(): Boolean {
        return currentWindow?.get()?.isShowing == true || isShowingFlag.get()
    }

    private fun resetState() {
        currentWindow = null
        isShowingFlag.set(false)
    }
}