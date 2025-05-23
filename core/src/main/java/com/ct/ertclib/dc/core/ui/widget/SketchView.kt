/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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
import com.ct.ertclib.dc.core.data.screenshare.SketchBean
import com.ct.ertclib.dc.core.data.screenshare.xml.DrawingInfo
import com.ct.ertclib.dc.core.data.screenshare.xml.PointsInfo
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.constants.MiniAppConstants.ROLE_SHARE_SIDE
import com.ct.ertclib.dc.core.data.screenshare.xml.PointBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList


class SketchView : View {

    private val sLogger = Logger.getLogger(TAG)

    enum class MODE {
        MODE_DISABLE,
        MODE_DRAW,
        MODE_ERASE
    }

    companion object {
        private const val TAG = "SketchView"
        private const val TOUCH_TOLERANCE = 3.0F
        const val COLOR_PAINT_SHARE = "#ffff4444"
        const val COLOR_PAINT_WATCH = "#ff44ff44"
        private const val DEFAULT_SIZE = 8.0f
        private const val SKETCH_DISAPPEAR_DELAY = 1000L
    }

    private var role: Int = ROLE_SHARE_SIDE
    private var mAttr: AttributeSet? = null

    @ColorInt
    private var sharePaintColor = Color.parseColor(COLOR_PAINT_SHARE)
    @ColorInt
    private var watchPaintColor = Color.parseColor(COLOR_PAINT_WATCH)
    private var mLocalPathSize: Float = DEFAULT_SIZE
    private lateinit var mLocalPaint: Paint
    private var mLocalPath: Path = Path()
    private lateinit var historyPaint: Paint
    private var historyPath: Path = Path()

    private var mBufferBitmap: Bitmap? = null
    private var mBufferCanvas: Canvas? = null
    private var mLastX = 0.0F
    private var mLastY = 0.0F
    private var mCurrType = MODE.MODE_DRAW
    private var isDrawing = false
    private val cachedDrawingInfoList = CopyOnWriteArrayList<DrawingInfo>()


    var mSketchCallback: SketchCallback? = null
    var currentDrawingInfo: DrawingInfo? = null

    private var mSketchInfoList = CopyOnWriteArrayList<SketchBean>()
    private val scope = CoroutineScope(Dispatchers.Default)

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
            val obtain = context.obtainStyledAttributes(it, R.styleable.SketchView)
            mLocalPathSize = obtain.getFloat(R.styleable.SketchView_localPathSize, DEFAULT_SIZE)

            obtain.recycle()
        }

        mLocalPaint = Paint().apply {
            //抗锯齿效果
            isAntiAlias = true
            //防抖
            isDither = true
            //颜色
            color = getPaintColor()
            //模式
            style = Paint.Style.STROKE
            //结合方式
            strokeJoin = Paint.Join.ROUND
            //画笔两端样式
            strokeCap = Paint.Cap.ROUND
            //线宽
            strokeWidth = SizeUtils.dp2px(mLocalPathSize).toFloat()
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
            strokeWidth = SizeUtils.dp2px(mLocalPathSize).toFloat()
        }
    }

    fun setRole(role: Int) {
        this.role = role
        mLocalPaint.color = getPaintColor()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        genNewBufferCanvas(w, h)
        mSketchInfoList.clear()
    }

    override fun onDraw(canvas: Canvas) {
        // 背景透明
        canvas.drawColor(Color.parseColor("#00000000"))
        // up的时候绘制
        mBufferBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        // move的时候绘制
        canvas.drawPath(mLocalPath, mLocalPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (mCurrType != MODE.MODE_DISABLE) {
            return handleTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchEvent(event: MotionEvent?): Boolean {
        event?.let { it ->
            val x = it.x
            val y = it.y
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDrawing = true
                    mLastX = x
                    mLastY = y
                    mLocalPath.moveTo(x, y)
                    mLocalPaint.color = getPaintColor()
                    currentDrawingInfo = DrawingInfo()
                        .apply {
                        this.color = ColorUtils.int2ArgbString(getPaintColor())
                        this.erase = mCurrType == MODE.MODE_ERASE
                        this.width = mLocalPathSize
                        this.points = PointsInfo()
                            .apply {
                            pointS = mutableListOf()
                        }
                        this.points.pointS.add(PointBean(x, y))
                    }
                }

                MotionEvent.ACTION_UP -> {
                    mBufferCanvas?.apply {
                        drawPath(mLocalPath, mLocalPaint)
                        mLocalPath.reset()
                    }
                    scope.launch {
                        delay(SKETCH_DISAPPEAR_DELAY)
                        withContext(Dispatchers.Main) {
                            rollBackPreSketch()
                            invalidate()
                        }
                    }
                    currentDrawingInfo?.let { info ->
                        mSketchCallback?.onSketchEvent(info)
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
                    val dx = (x - mLastX).absoluteValue
                    val dy = (y - mLastY).absoluteValue
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        mLocalPath.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2)
                        mLastX = x
                        mLastY = y
                        invalidate()
                    }
                    currentDrawingInfo?.points?.pointS?.add(PointBean(x, y))
                }

                else -> {}
            }
        }
        return true
    }

    private fun genNewBufferCanvas(width: Int, height: Int) {
        mBufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mBufferCanvas = Canvas(mBufferBitmap!!)
    }

    private fun drawByPath(path: Path, paint: Paint) {
        path.let {
            mBufferCanvas?.drawPath(path, paint)
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
            mSketchInfoList.add(SketchBean(drawingInfo))
        }
        val path = if (isHistoryDrawing) {
            historyPath
        } else {
            mLocalPath
        }
        val paint = if (isHistoryDrawing) {
            historyPaint
        } else {
            mLocalPaint
        }
        drawingInfo.let {
            paint.color = Color.parseColor(it.color)
            setLocalPathSize(it.width)
            val points = it.points
            lateinit var preBean: PointBean
            for ((index, bean) in points.pointS.withIndex()) {
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
        mBufferCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        if (cleanPath) {
            mSketchInfoList.clear()
            mLocalPath.reset()
        }
    }

    private fun setLocalPathSize(size: Float) {
        mLocalPathSize = size
        mLocalPaint.strokeWidth = SizeUtils.dp2px(mLocalPathSize).toFloat()
    }

    private fun rollBackPreSketch() {
        clearCanvas()
        mSketchInfoList.forEach { item ->
            drawByDrawingInfo(item.drawingInfo, isFromMiniApp = false, isHistoryDrawing = true)
        }
    }

    private fun getPaintColor(): Int {
        LogUtils.debug(TAG, "getPaintColor, role: $role")
        return if (role == ROLE_SHARE_SIDE) {
            sharePaintColor
        } else {
            watchPaintColor
        }
    }

    interface SketchCallback {
        fun onSketchEvent(drawingInfo: DrawingInfo)
    }
}