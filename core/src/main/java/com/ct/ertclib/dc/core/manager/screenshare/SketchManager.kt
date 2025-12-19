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

package com.ct.ertclib.dc.core.manager.screenshare

import android.content.Context
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.port.listener.ISketchWindowListener
import com.ct.ertclib.dc.core.port.manager.ISketchManager
import com.ct.ertclib.dc.core.data.screenshare.PointBean
import com.ct.ertclib.dc.core.ui.widget.ScreenShareCtrlPanel
import com.ct.ertclib.dc.core.ui.widget.SketchView
import com.ct.ertclib.dc.core.data.screenshare.DrawingInfo
import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SketchManager(
    private val context: Context,
    private val screenShareUseCase: IScreenShareUseCase
) : ISketchManager {

    companion object {
        private const val TAG = "ScreenShareSketchManager"
    }

    private val logger = Logger.getLogger(TAG)
    private var sketchWindowListener: ISketchWindowListener? = null
    private var windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private var sketchLayout: View? = null
    private var sketchView: SketchView? = null
    private var ctrlPanel: ScreenShareCtrlPanel? = null
    private var ctrlPanelLayoutParams: WindowManager.LayoutParams? = null
    private var sketchLayoutParams: WindowManager.LayoutParams? = null
    private var sketchViewVisible: Boolean = false

    private var localWidth = ScreenUtils.getScreenWidth(context).toFloat()
    private var localHeight = ScreenUtils.getScreenHeight(context).toFloat()


    private var remoteWindowWidth = 0f
    private var remoteWindowHeight = 0f

    private var localTranslationX = 0F
    private var localTranslationY = 0F

    private var rotation = 0
    private var rectF = RectF()
    private var isFirstShowPopupWindow = true

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun setSketchWindowListener(listener: ISketchWindowListener) {
        sketchWindowListener = listener
    }

    override fun showSketchControlWindow(paintColor: String, paintWidth: Float) {
        logger.info("showSketchControlWindow")
        scope.launch(Dispatchers.Main) {
            addSketchView(paintColor, paintWidth)
        }
    }

    override fun exitSketchControlWindow() {
        logger.info("exitSketchControlWindow")
        scope.launch(Dispatchers.Main) {
            sketchView?.clearCanvas(true)
            removeSketchLayout()
            windowManager = null
        }
    }

    override fun setLocalWindowInformation(rectF: RectF, rotation: Int) {
        if (!screenShareUseCase.isInSharing()) {
            localTranslationX = rectF.left
            localTranslationY = rectF.top
            localWidth = rectF.width()
            localHeight = rectF.height()
            sketchWindowListener?.onLocalWindowNotified(rectF.width(), rectF.height())
        } else {
            this.rotation = 0
            sketchWindowListener?.onLocalWindowNotified(ScreenUtils.getScreenWidth(context).toFloat(), ScreenUtils.getScreenHeight(context).toFloat())
        }
        if (rotation != this.rotation) {
            sketchView?.clearCanvas(true)
        }
        this.rectF = rectF
        this.rotation = rotation
    }

    override fun setRemoteWindowSize(width: Float, height: Float) {
        logger.info("setRemoteWindowSize, width: $width, height: $height")
        remoteWindowWidth = width
        remoteWindowHeight = height
    }

    override fun addSketchInfo(sketchInfo: DrawingInfo) {
        logger.info("addSketchInfo")
        scope.launch(Dispatchers.Main) {
            windowManager?.let {
                if (sketchLayout?.isVisible != true) {
                    ctrlPanel?.setDrawRebBubbleVisible(true)
                }
                sketchView?.drawByDrawingInfo(sketchInfo, isFromMiniApp = true)
            }
        }
    }

    override fun initManager() {
        //
    }

    override fun release() {
        logger.info("release")
        exitSketchControlWindow()
        sketchWindowListener = null
    }

    private fun addSketchView(paintColor: String, paintWidth: Float) {
        if (windowManager == null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        }
        removeSketchLayout()
        sketchLayout = LayoutInflater.from(context).inflate(R.layout.sketch_view_layout, null)
        sketchLayoutParams = getWindowLayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
        windowManager?.addView(sketchLayout, sketchLayoutParams)
        sketchView = sketchLayout?.findViewById(R.id.sketch_view)
        sketchView?.let {
            it.sketchCallback = object : SketchView.SketchCallback {
                override fun onSketchEvent(drawingInfo: DrawingInfo) {
                    logger.info("onSketchEvent")
                    val drawInfo = calculateDrawInfo(drawingInfo)
                    sketchWindowListener?.onSketchEvent(drawInfo)
                }
            }
            it.paintColor = paintColor.toColorInt()
            it.localPathSize = paintWidth
        }
        ctrlPanelLayoutParams = getWindowLayoutParams().apply {
            x = ScreenUtils.getScreenWidth(context)
            y = ScreenUtils.getScreenHeight(context)
        }
        ctrlPanel = ScreenShareCtrlPanel(context).apply {
            setOnTouchListener(ctrlLayoutTouchListener)
            setListener(object : ScreenShareCtrlPanel.OnCtrlPanelListener {

                override fun onExit() {
                    sketchWindowListener?.onExitBoardBtnClick()
                }

                override fun onDraw() {
                    sketchLayout?.let {
                        setSketchViewVisibility(!it.isVisible)
                    }
                }
            })
        }
        windowManager?.addView(ctrlPanel, ctrlPanelLayoutParams)
        showPopupWindow()
    }

    private fun showPopupWindow() {
        if (isFirstShowPopupWindow) {
            ctrlPanel?.post {
                LogUtils.debug(TAG, "addSketchView show popup window")
                val view = LayoutInflater.from(context).inflate(R.layout.layout_screen_share_tips, null, false)
                val closeIv = view.findViewById<AppCompatImageView>(R.id.close_image)
                val popupWindow = PopupWindow(
                    view,
                    context.resources.getDimensionPixelSize(R.dimen.screen_share_tips_width),
                    context.resources.getDimensionPixelSize(R.dimen.screen_share_tips_height),
                    true)
                closeIv.setOnClickListener {
                    if (popupWindow.isShowing) {
                        popupWindow.dismiss()
                    }
                }

                ctrlPanel?.let {
                    val ctrlPanelHeight = it.height
                    val ctrlPanelWidth = it.width
                    popupWindow.showAsDropDown(it, -ctrlPanelWidth, ctrlPanelHeight / 2 + popupWindow.height / 2)
                }
            }
            isFirstShowPopupWindow = false
        }
    }

    private var ctrlLayoutTouchListener = object : View.OnTouchListener {
        // 记录上次移动的位置
        private var preTouchX = 0f
        private var preTouchY = 0f

        // 是否是移动事件
        var isMoved = false
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            // 当前点坐标
            val x = event.rawX
            val y = event.rawY
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isMoved = false
                    // 记录按下位置
                    preTouchX = x
                    preTouchY = y
                }

                MotionEvent.ACTION_MOVE -> {
                    // 移动的距离
                    val dx = x - preTouchX
                    val dy = y - preTouchY
                    // 更新位置
                    updateCtrlLayoutLocation(dx, dy)
                    // 当前点操作点
                    preTouchX = x
                    preTouchY = y
                    //
                    isMoved = true
                    isMoved = true
                }

                MotionEvent.ACTION_CANCEL -> isMoved = true
            }
            return isMoved
        }
    }

    /**
     * WindowManager.LayoutParams
     */
    private fun getWindowLayoutParams(): WindowManager.LayoutParams {
        val layoutParams = WindowManager.LayoutParams()
        //设置类型
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        // 设置行为选项
        layoutParams.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        //如果悬浮窗图片为透明图片，需要设置该参数为PixelFormat.RGBA_8888
        layoutParams.format = PixelFormat.RGBA_8888
        //设置悬浮窗的宽/高
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        // 屏幕左上角为起始点
        layoutParams.gravity = Gravity.LEFT or Gravity.TOP
        //设置x、y轴偏移量
        layoutParams.x = 0
        layoutParams.y = 0
        return layoutParams
    }

    private fun setSketchViewVisibility(visible: Boolean) {
        sketchViewVisible = visible
        if (!visible) {
            sketchView?.clearCanvas(true)
        } else {
            ctrlPanel?.setDrawRebBubbleVisible(false)
        }
        ctrlPanel?.setSketchBtnSelected(visible)
        sketchLayout?.isVisible = visible
    }

    private fun updateCtrlLayoutLocation(dx: Float, dy: Float) {
        ctrlPanelLayoutParams?.let {
            it.x += dx.toInt()
            it.y += dy.toInt()
            if (ctrlPanel != null && windowManager != null) {
                windowManager?.updateViewLayout(ctrlPanel, ctrlPanelLayoutParams)
            }
        }
    }

    private fun calculateDrawInfo(it: DrawingInfo): DrawingInfo {
        return if (screenShareUseCase.isInSharing() || (remoteWindowWidth == 0f && remoteWindowHeight == 0f) || (it.color == SketchView.COLOR_PAINT_DEFAULT)) {
            it
        } else {
            val widthRate = remoteWindowWidth / localWidth
            val heightRate = remoteWindowHeight / localHeight
            LogUtils.debug(TAG, "calculateDrawInfo widthRate: $widthRate, heightRate: $heightRate")
            val originPoints = it.pointList
            val pointList = originPoints.map { bean ->
                val pointPair = getRotationCoordination(rectF.centerX(), rectF.centerY(), bean.x, bean.y)
                PointBean((pointPair.first - localTranslationX) * widthRate, (pointPair.second - localTranslationY) * heightRate)
            }.toMutableList()
            DrawingInfo(
                it.width,
                it.color,
                pointList
            )
        }
    }

    private fun removeSketchLayout() {
        LogUtils.debug(TAG, "removeSketchLayout")
        windowManager?.let {
            if (sketchView?.isAttachedToWindow == true) {
                it.removeView(sketchLayout)
            }
            if (ctrlPanel?.isAttachedToWindow == true) {
                it.removeView(ctrlPanel)
            }
        }
    }

    private fun getRotationCoordination(centerX: Float, centerY: Float, originX: Float, originY: Float): Pair<Float, Float> {
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat(), centerX, centerY)
        val point = floatArrayOf(originX, originY)
        matrix.mapPoints(point)
        return Pair(point[0], point[1])
    }
}