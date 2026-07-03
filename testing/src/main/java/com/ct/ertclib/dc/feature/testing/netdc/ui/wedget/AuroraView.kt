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

package com.ct.ertclib.dc.feature.testing.netdc.ui.wedget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.toColorInt

/**
 * 暗色调极光背景View
 * 深色毛玻璃质感 + 柔和光晕 + 轻微颗粒感
 */
class AuroraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 暗色调渐变色 - 深色背景
    private val gradientColors = intArrayOf(
        "#1a1a2e".toColorInt(),  // 深灰蓝
        "#16213e".toColorInt(),  // 深蓝
        "#0f3460".toColorInt(),  // 深靛蓝
        "#1a1a2e".toColorInt()   // 深灰蓝
    )

    // 柔和光晕颜色 - 暗色调
    private val glowColors = listOf(
        "#803366CC".toColorInt(),  // 暗蓝
        "#80663399".toColorInt(),  // 暗紫
        "#80336666".toColorInt(),  // 暗青
        "#80996666".toColorInt()   // 暗红棕
    )

    // 动画进度
    private var progress = 0f
    private var animator: ValueAnimator? = null

    // 噪声纹理
    private var noiseBitmap: Bitmap? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        initNoise()
        startAnimation()
    }

    /**
     * 初始化噪声纹理
     */
    private fun initNoise() {
        val noiseSize = 256
        val noiseArray = IntArray(noiseSize * noiseSize)

        for (i in noiseArray.indices) {
            val gray = (Math.random() * 30 + 20).toInt()
            noiseArray[i] = Color.argb(8, gray, gray, gray)
        }

        noiseBitmap = Bitmap.createBitmap(noiseArray, noiseSize, noiseSize, Bitmap.Config.ARGB_8888)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // 1. 绘制深色渐变背景
        drawDarkGradient(canvas, width, height)

        // 2. 绘制柔和光晕
        drawSoftGlow(canvas, width, height)

        // 3. 绘制微弱光斑
        drawLightSpots(canvas, width, height)

        // 4. 叠加颗粒噪声
        drawNoise(canvas, width, height)
    }

    /**
     * 绘制深色渐变背景
     */
    private fun drawDarkGradient(canvas: Canvas, width: Float, height: Float) {
        val offsetX = sin(progress * Math.PI * 2).toFloat() * 0.08f
        val offsetY = cos(progress * Math.PI * 1.3).toFloat() * 0.08f

        val startX = width * (0.3f + offsetX * 0.1f)
        val startY = height * (0.2f + offsetY * 0.1f)
        val endX = width * (0.7f - offsetX * 0.1f)
        val endY = height * (0.8f - offsetY * 0.1f)

        val gradient = LinearGradient(
            startX, startY, endX, endY,
            gradientColors,
            null,
            Shader.TileMode.MIRROR
        )

        paint.shader = gradient
        paint.alpha = 255
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    /**
     * 绘制柔和光晕
     */
    private fun drawSoftGlow(canvas: Canvas, width: Float, height: Float) {
        val centerX = width / 2f
        val centerY = height / 2f

        for (i in glowColors.indices) {
            val angle = (i.toFloat() / glowColors.size * Math.PI * 2 + progress * Math.PI * 2).toFloat()
            val radius = width * (0.5f + i * 0.1f)

            val glowX = centerX + sin((angle * 0.7f).toDouble()).toFloat() * width * 0.2f
            val glowY = centerY + cos((angle * 0.5f).toDouble()).toFloat() * height * 0.2f

            val radialGradient = RadialGradient(
                glowX, glowY, radius,
                glowColors[i],
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )

            paint.shader = radialGradient
            paint.alpha = 40
            canvas.drawCircle(glowX, glowY, radius, paint)
        }
    }

    /**
     * 绘制微弱光斑
     */
    private fun drawLightSpots(canvas: Canvas, width: Float, height: Float) {
        val spotCount = 6

        for (i in 0 until spotCount) {
            val phase = (i.toFloat() / spotCount + progress).toDouble()
            val spotX = width * (0.15f + (sin(phase * Math.PI * 2) * 0.35f + 0.5f).toFloat())
            val spotY = height * (0.15f + (cos(phase * Math.PI * 1.5) * 0.35f + 0.5f).toFloat())
            val spotRadius = width * (0.06f + sin(phase * Math.PI).toFloat() * 0.02f)

            val gradient = RadialGradient(
                spotX, spotY, spotRadius,
                "#40ffffff".toColorInt(),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )

            paint.shader = gradient
            paint.alpha = 50
            canvas.drawCircle(spotX, spotY, spotRadius, paint)
        }
    }

    /**
     * 叠加噪声纹理
     */
    private fun drawNoise(canvas: Canvas, width: Float, height: Float) {
        noiseBitmap?.let { noise ->
            val shader = BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            paint.shader = shader
            paint.alpha = 15
            canvas.drawRect(0f, 0f, width, height, paint)
        }
    }

    /**
     * 启动动画
     */
    private fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 20000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /**
     * 停止动画
     */
    fun stopAnimation() {
        animator?.cancel()
    }

    /**
     * 恢复动画
     */
    fun resumeAnimation() {
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
        noiseBitmap?.recycle()
        noiseBitmap = null
    }
}