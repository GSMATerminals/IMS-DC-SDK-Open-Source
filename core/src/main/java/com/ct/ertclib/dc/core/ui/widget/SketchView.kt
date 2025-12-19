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
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.blankj.utilcode.util.ColorUtils
import com.blankj.utilcode.util.SizeUtils
import kotlin.math.absoluteValue
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.data.screenshare.DrawingInfo
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.data.screenshare.PointBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import androidx.core.content.withStyledAttributes


class SketchView : View {

    companion object {
        private const val TAG = "SketchView"
        private const val TOUCH_TOLERANCE = 3.0F
        const val COLOR_PAINT_DEFAULT = "#ffff4444"
        private const val DEFAULT_SIZE = 8.0f
        private const val SKETCH_DISAPPEAR_DELAY = 1000L
    }

    private var mAttr: AttributeSet? = null

    @ColorInt
    var paintColor = COLOR_PAINT_DEFAULT.toColorInt()
    var localPathSize: Float = DEFAULT_SIZE
    private lateinit var localPaint: Paint
    private var localPath: Path = Path()
    private lateinit var historyPaint: Paint
    private var historyPath: Path = Path()

    private var bufferBitmap: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    private var lastX = 0.0F
    private var lastY = 0.0F
    private var isDrawing = false
    private val cachedDrawingInfoList = CopyOnWriteArrayList<DrawingInfo>()


    var sketchCallback: SketchCallback? = null
    private var currentDrawingInfo: DrawingInfo? = null

    private var sketchInfoList = CopyOnWriteArrayList<DrawingInfo>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        mAttr = attrs
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        mAttr = attrs
        init()
    }

    private fun init() {

        mAttr?.let {
            context.withStyledAttributes(it, R.styleable.SketchView) {
                localPathSize = getFloat(R.styleable.SketchView_localPathSize, DEFAULT_SIZE)
            }
        }

        localPaint = Paint().apply {
            //抗锯齿效果
            isAntiAlias = true
            //防抖
            isDither = true
            //颜色
            color = paintColor
            //模式
            style = Paint.Style.STROKE
            //结合方式
            strokeJoin = Paint.Join.ROUND
            //画笔两端样式
            strokeCap = Paint.Cap.ROUND
            //线宽
            strokeWidth = SizeUtils.dp2px(localPathSize).toFloat()
        }

        historyPaint = Paint().apply{
            //抗锯齿效果
            isAntiAlias = true
            //防抖
            isDither = true
            //模式
            style = Paint.Style.STROKE
            //结合方式
            strokeJoin = Paint.Join.ROUND
            //画笔两端样式
            strokeCap = Paint.Cap.ROUND
            //线宽
            strokeWidth = SizeUtils.dp2px(localPathSize).toFloat()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        genNewBufferCanvas(w, h)
        sketchInfoList.clear()
    }

    override fun onDraw(canvas: Canvas) {
        // 背景透明
        canvas.drawColor("#00000000".toColorInt())
        // up的时候绘制
        bufferBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        // move的时候绘制
        canvas.drawPath(localPath, localPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { it ->
            val x = it.x
            val y = it.y
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDrawing = true
                    lastX = x
                    lastY = y
                    localPath.moveTo(x, y)
                    localPaint.color = paintColor
                    currentDrawingInfo = DrawingInfo(
                        DEFAULT_SIZE,
                        ColorUtils.int2ArgbString(paintColor),
                        mutableListOf<PointBean>().apply { add(PointBean(x, y)) }
                    )
                }

                MotionEvent.ACTION_UP -> {
                    bufferCanvas?.apply {
                        drawPath(localPath, localPaint)
                        localPath.reset()
                    }
                    scope.launch {
                        delay(SKETCH_DISAPPEAR_DELAY)
                        withContext(Dispatchers.Main) {
                            rollBackPreSketch()
                            invalidate()
                        }
                    }
                    currentDrawingInfo?.let { info ->
                        sketchCallback?.onSketchEvent(info)
                    }
                    isDrawing = false
                    if (cachedDrawingInfoList.isNotEmpty()) {
                        for (info in cachedDrawingInfoList) {
                            drawByDrawingInfo(info, isFromMiniApp = true)
                        }
                        cachedDrawingInfoList.clear()
                    } else {
                        //do nothing
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (x - lastX).absoluteValue
                    val dy = (y - lastY).absoluteValue
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        localPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                        lastX = x
                        lastY = y
                        invalidate()
                    }
                    currentDrawingInfo?.pointList?.add(PointBean(x, y))
                }

                else -> {}
            }
        }
        return true
    }

    private fun genNewBufferCanvas(width: Int, height: Int) {
        bufferBitmap = createBitmap(width, height)
        bufferCanvas = Canvas(bufferBitmap!!)
    }

    private fun drawByPath(path: Path, paint: Paint) {
        path.let {
            bufferCanvas?.drawPath(path, paint)
            path.reset()
            postInvalidate()
        }
    }

    fun drawByDrawingInfo(drawingInfo: DrawingInfo, isFromMiniApp: Boolean = false, isHistoryDrawing: Boolean = false) {
        if (isFromMiniApp) {
            if (isDrawing) {
                cachedDrawingInfoList.add(drawingInfo)
                LogUtils.debug(TAG, "drawByDrawingInfo is drawing")
                return
            }
            sketchInfoList.add(drawingInfo)
        }
        val path = if (isHistoryDrawing) {
            historyPath
        } else {
            localPath
        }
        val paint = if (isHistoryDrawing) {
            historyPaint
        } else {
            localPaint
        }
        drawingInfo.let {
            paint.color = it.color.toColorInt()
            localPathSize = it.width
            localPaint.strokeWidth = SizeUtils.dp2px(localPathSize).toFloat()
            lateinit var preBean: PointBean
            for ((index, bean) in it.pointList.withIndex()) {
                if (index == 0) {
                    path.reset()
                    path.moveTo(bean.x, bean.y)
                } else {
                    path.quadTo(preBean.x, preBean.y, bean.x, bean.y)
                }
                preBean = bean
            }
            drawByPath(path, paint)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
    }

    fun clearCanvas(cleanPath: Boolean = false) {
        bufferCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        if (cleanPath) {
            sketchInfoList.clear()
            localPath.reset()
        }
    }

    private fun rollBackPreSketch() {
        clearCanvas()
        sketchInfoList.forEach { item ->
            drawByDrawingInfo(item, isFromMiniApp = false, isHistoryDrawing = true)
        }
    }

    interface SketchCallback {
        fun onSketchEvent(drawingInfo: DrawingInfo)
    }
}