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

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.databinding.ScreenShareCtrlPanelLayoutBinding
import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.utils.common.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.absoluteValue

class ScreenShareCtrlPanel : FrameLayout, KoinComponent {

    companion object {
        private const val TAG = "ScreenShareCtrlPanel"
    }

    private var listener: OnCtrlPanelListener? = null
    private val screenShareUseCase: IScreenShareUseCase by inject()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var viewBinding: ScreenShareCtrlPanelLayoutBinding =
        ScreenShareCtrlPanelLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    private var isMove = false
    private var isPaused = false
    private var preTouchX = 0f
    private var preTouchY = 0f
    private var windowManager = context.getSystemService(WindowManager::class.java) as? WindowManager


    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    init {
        viewBinding.sketchDrawingBtn.setOnClickListener {
            listener?.onDraw()
        }
        viewBinding.exitBoardBtn.setOnClickListener {
            listener?.onExit()
        }
        viewBinding.pauseShareBtn.setOnClickListener {
            isPaused = !isPaused
            viewBinding.pauseShareBtn.setImageResource(
                if (isPaused) R.drawable.sketch_ctrl_resume_selector else R.drawable.sketch_ctrl_pause_selector
            )
            listener?.onPauseResume(isPaused)
        }
        // 暂停按钮仅共享方显示，观看方隐藏
        viewBinding.pauseShareBtn.visibility =
            if (screenShareUseCase.isInSharing()) VISIBLE else GONE
        setMoveListener()
        LogUtils.debug(TAG, "init")
        scope.launch {
            NewCallAppSdkInterface.callAPPStatusOnFront.collect { isOnFront ->
                withContext(Dispatchers.Main) {
                    if (!screenShareUseCase.isInSharing() && !isOnFront) {
                        viewBinding.controlLayout.isVisible = false
                        viewBinding.tipsLayout.isVisible = true
                    } else {
                        viewBinding.controlLayout.isVisible = true
                        viewBinding.tipsLayout.isVisible = false
                    }
                }
            }
        }
    }

    fun setListener(listener: OnCtrlPanelListener) {
        this.listener = listener
    }

    fun setSketchBtnSelected(selected: Boolean) {
        viewBinding.sketchDrawingBtn.isSelected = selected
    }

    fun setDrawRebBubbleVisible(visible: Boolean) {
        viewBinding.sketchRed.visibility = if (visible) VISIBLE else GONE
    }

    /**
     * 重置暂停状态为未暂停，并恢复按钮图标。
     * 在面板退出或通话结束时调用，确保下次打开面板时按钮状态正确。
     */
    fun resetPauseState() {
        isPaused = false
        viewBinding.pauseShareBtn.setImageResource(R.drawable.sketch_ctrl_pause_selector)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setMoveListener() {
        this.setOnTouchListener { v, event ->
            event?.let {
                val x = it.rawX
                val y = it.rawY
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isMove = false
                        preTouchX = it.rawX
                        preTouchY = it.rawY
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // 移动的距离
                        val dx: Float = x - preTouchX
                        val dy: Float = y - preTouchY
                        updateFloatingButtonLocation(dx, dy)
                        preTouchX = x
                        preTouchY = y
                        if (dx.absoluteValue > 2 || dy.absoluteValue > 2) {
                            isMove = true
                        }
                    }

                    MotionEvent.ACTION_UP -> {

                    }
                }
            }
            isMove
        }
    }

    private fun updateFloatingButtonLocation(dx: Float, dy: Float) {
        val layoutParams = this.layoutParams as WindowManager.LayoutParams
        layoutParams.apply {
            x += dx.toInt()
            y += dy.toInt()
        }
        windowManager?.updateViewLayout(this, layoutParams)
    }

    interface OnCtrlPanelListener {

        fun onExit()

        fun onDraw()

        fun onPauseResume(isPause: Boolean)
    }
}