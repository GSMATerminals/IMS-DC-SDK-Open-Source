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

package com.ct.ertclib.dc.app.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface.SDK_INTENT_MINI_EXPANDED
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.port.common.IUIChangeListener
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import kotlin.math.absoluteValue

class MiniAppEntryHolder(private val context: Context) {

    companion object {
        private const val TAG = "MiniAppEntryHolder"
        private const val ENTRY_SHOW_HIDE_DURATION = 200L
        private const val ENTRY_MOVE_DURATION = 300L
        private const val MSG_HIDE_FLOATING_BALL = 1
        private const val FLOATING_BALL_HIDE_DURATION = 10000L
    }

    enum class EntryMode {
        DISMISS,
        DISPLAY
    }

    private var windowManager: WindowManager? = null

    private var miniAppEntryView: MiniAppEntryView? = null

    private var floatLps: WindowManager.LayoutParams? = null

    private var currentMode = EntryMode.DISMISS
    private var preTouchX = 0f
    private var preTouchY = 0f
    private var isMove = false

    var miniAppList: MiniAppList? = null
    var callInfo: CallInfo? = null
    var style: Int = 0

    private val uiChangeListener: IUIChangeListener = object : IUIChangeListener {
        override fun onUIChanged() {
            refreshUI()
        }
    }

    private val handler: Handler = object: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_HIDE_FLOATING_BALL -> {
                    hideFloatingBall()
                }
            }
        }
    }

    fun show(style: Int) {
        this.style = style
        if (!Settings.canDrawOverlays(context)) {
            NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.WARN_LEVEL, TAG, "do not have overlay permission")
            return
        }

        if (currentMode == EntryMode.DISPLAY) {
            NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "entry is displaying")
            return
        }

        if (windowManager == null) {
            windowManager = context.getSystemService(WindowManager::class.java)
        }

        if (floatLps == null) {
            floatLps = getWindowLayoutParams().apply {
                y = NewCallAppSdkInterface.floatPositionY
                x = NewCallAppSdkInterface.floatPositionX
            }
            miniAppEntryView = MiniAppEntryView(context)
            miniAppEntryView?.let { view ->
                view.setOnClickListener {
                    if (view.isActive) {
                        switchToExpandedView()
                    }
                }
                view.uiListener = uiChangeListener
            }
            setMoveListener()
        }
        refreshUI()
        miniAppEntryView?.let {
            if (!it.isAttachedToWindow) {
                miniAppEntryView?.isVisible = true
                startEntryShowAnimation()
                windowManager?.addView(miniAppEntryView, floatLps)
            }
        }

        currentMode = EntryMode.DISPLAY
    }

    fun dismiss() {
        if (currentMode == EntryMode.DISMISS) {
            return
        }
        miniAppEntryView?.let {
            if (it.isAttachedToWindow) {
                startEntryHideAnimation {
                    it.isVisible = false
                    it.alpha = 1F
                    windowManager?.removeView(it)
                }
            }
        }
        currentMode = EntryMode.DISMISS
    }

    private fun refreshUI() {
        miniAppEntryView?.let {
            it.setFloatingBallIcon(style)
            it.setFloatingHalfBallIcon(style, isLeftSide(NewCallAppSdkInterface.floatPositionX.toFloat()))
        }
    }

    private fun switchToExpandedView() {
        dismiss()

        val intent = Intent(SDK_INTENT_MINI_EXPANDED)
        intent.putExtra("miniAppList", miniAppList)
        intent.putExtra("callInfo", callInfo)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_NEW_TASK)
        val point = Point(0, 0)
        floatLps?.let {
            point.x = it.x
            point.y = it.y
        }
        intent.putExtra("point", point)
        context.startActivity(intent)
    }

    private fun getWindowLayoutParams() = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        flags =
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        format = PixelFormat.RGBA_8888
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        gravity = Gravity.START or Gravity.TOP
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setMoveListener() {
        miniAppEntryView?.setOnTouchListener { v, event ->
            event?.let {
                val x = it.rawX
                val y = it.rawY
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isMove = false
                        preTouchX = it.rawX
                        preTouchY = it.rawY
                        activeFloatingBall()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // 移动的距离
                        val dx: Float = x - preTouchX
                        val dy: Float = y - preTouchY
                        updateFloatingButtonLocation(dx, dy)
                        preTouchX = x
                        preTouchY = y
                        NewCallAppSdkInterface.floatPositionX += dx.toInt()
                        NewCallAppSdkInterface.floatPositionY += dy.toInt()
                        if (dx.absoluteValue > 2 || dy.absoluteValue > 2) {
                            isMove = true
                        } else {

                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isMove) {
                            val layoutParams = miniAppEntryView?.layoutParams as? WindowManager.LayoutParams
                            val startX = layoutParams?.x ?: x
                            val finalX = if (isLeftSide(startX.toFloat())) {
                                0F
                            } else {
                                ScreenUtils.getScreenWidth(context).toFloat()
                            }
                            miniAppEntryView?.setFloatingHalfBallIcon(style, isLeftSide(finalX))
                            val valueAnimation = ValueAnimator.ofFloat(startX.toFloat(), finalX)
                            valueAnimation.duration = ENTRY_MOVE_DURATION
                            valueAnimation.interpolator = AccelerateDecelerateInterpolator()
                            valueAnimation.addUpdateListener { animator ->
                                val value = animator.animatedValue as? Float
                                value?.let {
                                    updateFloatingButtonAbsoluteX(value)
                                }
                            }
                            valueAnimation?.addListener(object: AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    NewCallAppSdkInterface.floatPositionX = finalX.toInt()
                                    hideFloatingBallDelayed()
                                    preTouchX = finalX
                                    isMove = false
                                }
                            })
                            valueAnimation.start()
                        } else {
                            hideFloatingBallDelayed()
                        }
                    }

                    else -> {}
                }
            }
            isMove
        }
    }

    private fun updateFloatingButtonLocation(dx: Float, dy: Float) {
        miniAppEntryView?.let {
            val layoutParams = it.layoutParams as WindowManager.LayoutParams
            layoutParams.apply {
                x += dx.toInt()
                y += dy.toInt()
            }
            windowManager?.updateViewLayout(it, layoutParams)
        }
    }

    private fun updateFloatingButtonAbsoluteX(positionX: Float) {
        miniAppEntryView?.let {
            val layoutParams = it.layoutParams as WindowManager.LayoutParams
            layoutParams.apply {
                x = positionX.toInt()
            }
            windowManager?.updateViewLayout(it, layoutParams)
        }
    }

    private fun startEntryShowAnimation() {
        val valueAnimation = ValueAnimator.ofFloat(0F, 1F)
        valueAnimation.duration = ENTRY_SHOW_HIDE_DURATION
        valueAnimation.addUpdateListener { animator ->
            val value = animator.animatedValue as? Float
            value?.let {
                miniAppEntryView?.alpha = value
            }
        }
        valueAnimation?.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                hideFloatingBallDelayed()
            }
        })
        valueAnimation.start()
    }

    private fun startEntryHideAnimation(onAnimationEnd: () -> Unit) {
        val valueAnimation = ValueAnimator.ofFloat(1F, 0F)
        valueAnimation.duration = ENTRY_SHOW_HIDE_DURATION
        valueAnimation.addUpdateListener { animator ->
            val value = animator.animatedValue as? Float
            value?.let {
                miniAppEntryView?.alpha = value
            }
        }
        valueAnimation?.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd.invoke()
            }

            override fun onAnimationCancel(animation: Animator) {
                onAnimationEnd.invoke()
            }
        })
        valueAnimation.start()
    }

    private fun hideFloatingBallDelayed() {
        handler.removeMessages(MSG_HIDE_FLOATING_BALL)
        val message = Message.obtain()
        message.what = MSG_HIDE_FLOATING_BALL
        handler.sendMessageDelayed(message, FLOATING_BALL_HIDE_DURATION)
    }

    private fun activeFloatingBall() {
        handler.removeMessages(MSG_HIDE_FLOATING_BALL)
        miniAppEntryView?.setFloatingBallMode(true)
    }

    private fun hideFloatingBall() {
        miniAppEntryView?.setFloatingBallMode(false)
    }

    private fun isLeftSide(x: Float): Boolean {
        return x < ScreenUtils.getScreenWidth(context) / 2
    }
}
