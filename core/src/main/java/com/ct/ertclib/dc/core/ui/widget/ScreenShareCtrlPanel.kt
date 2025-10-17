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

package com.ct.ertclib.dc.core.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import com.ct.ertclib.dc.core.databinding.ScreenShareCtrlPanelLayoutBinding
import com.ct.ertclib.dc.core.utils.common.LogUtils
import kotlin.math.absoluteValue

class ScreenShareCtrlPanel : FrameLayout {

    companion object {
        private const val TAG = "ScreenShareCtrlPanel"
    }

    private var listener: OnCtrlPanelListener? = null
    private var viewBinding: ScreenShareCtrlPanelLayoutBinding =
        ScreenShareCtrlPanelLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    private var isMove = false
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
        setMoveListener()
        LogUtils.debug(TAG, "init")
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
    }
}