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

package com.ct.ertclib.dc.core.ui.widget

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.utils.logger.Logger

/**
 * 悬浮窗确认弹窗
 * 保持与原ConfirmDialog一致的UI风格
 */
class ConfirmOverlayWindow(private val context: Context) {

    private val TAG = "ConfirmOverlayWindow"
    private val logger = Logger.getLogger(TAG)

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var confirmCallback: ConfirmCallback? = null

    interface ConfirmCallback {
        fun onAccept()
        fun onCancel()
    }

    /**
     * 显示悬浮窗弹窗
     */
    fun show(
        message: String,
        callback: ConfirmCallback?,
        acceptText: String? = null,
        cancelText: String? = null
    ) {
        this.confirmCallback = callback

        if (!hasOverlayPermission()) {
            logger.info("No SYSTEM_ALERT_WINDOW permission")
            return
        }

        dismiss() // 确保先移除已有的悬浮窗

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayView = View.inflate(context, R.layout.window_confirm, null)

            setupViews(message, acceptText, cancelText)
            initLayoutParams()

            windowManager?.addView(overlayView, params)
            logger.info("Confirm overlay window shown")

        } catch (e: Exception) {
            logger.info("Failed to show overlay window: ${e.message}")
        }
    }

    /**
     * 初始化布局参数
     */
    private fun initLayoutParams() {
        params = WindowManager.LayoutParams().apply {
            // 设置类型 - 根据不同系统版本选择合适的类型
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                type = WindowManager.LayoutParams.TYPE_PHONE
            }

            // 设置flags - 保持与原Dialog类似的行为
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN

            // 锁屏状态下显示
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                flags = flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            }

            format = PixelFormat.TRANSLUCENT

            // 设置宽高 - 与原Dialog一致：宽度MATCH_PARENT，高度WRAP_CONTENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER

            // 窗口标题
            title = "ConfirmOverlay"

            // 窗口透明度
            alpha = 1.0f

            // 窗口背景变暗效果（模拟Dialog的dim背景）
            dimAmount = 0.6f
            flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        }
    }

    /**
     * 设置视图内容 - 保持与原ConfirmDialog一致的UI逻辑
     */
    private fun setupViews(message: String, acceptText: String?, cancelText: String?) {
        overlayView?.apply {
            val tvMessage = findViewById<TextView>(R.id.tv_message)
            val btnCancel = findViewById<Button>(R.id.btn_cancel)
            val btnDone = findViewById<Button>(R.id.btn_done)

            // 设置消息
            tvMessage?.visibility = View.VISIBLE
            tvMessage?.text = message

            // 设置按钮文本
            if (!acceptText.isNullOrEmpty()) {
                btnDone?.text = acceptText
            } else {
                btnDone?.setText(R.string.accept_btn)
            }

            if (!cancelText.isNullOrEmpty()) {
                btnCancel?.text = cancelText
            } else {
                btnCancel?.setText(R.string.cancel_btn)
            }

            // 设置点击事件 - 与原ConfirmDialog的onClick逻辑一致
            btnCancel?.setOnClickListener {
                logger.info("confirm dialog onCancel")
                confirmCallback?.onCancel()
                dismiss()
            }

            btnDone?.setOnClickListener {
                logger.info("confirm dialog accept")
                confirmCallback?.onAccept()
                dismiss()
            }

            // 点击外部不消失（与原Dialog行为一致）
            setOnClickListener { }
        }
    }

    /**
     * 检查是否有悬浮窗权限
     */
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 销毁悬浮窗
     */
    fun dismiss() {
        try {
            if (overlayView != null && windowManager != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
                confirmCallback = null
                logger.info("Confirm overlay window dismissed")
            }
        } catch (e: Exception) {
            logger.error("Error dismissing overlay: ${e.message}")
        }
    }

    /**
     * 是否正在显示
     */
    val isShowing: Boolean
        get() = overlayView?.isAttachedToWindow ?: false
}