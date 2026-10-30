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

package com.ct.ertclib.dc.core.utils.common

import android.content.Context
import android.util.TypedValue

/**
 * 小程序浮窗尺寸 / 宽屏判断的公共工具。
 *
 * 供 [com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView] 和
 * 引导页 Activity 等场景共用，保证宽屏判断与浮窗尺寸口径一致。
 */
object FloatWindowMetrics {

    /** 浮窗宽高比（宽:高 = 1:2，竖长条） */
    const val FLOAT_WINDOW_ASPECT_RATIO = 1f / 2f

    /** 浮窗与屏幕边缘的边距（dp） */
    const val FLOAT_WINDOW_MARGIN_DP = 40

    /** 浮窗圆角半径（dp） */
    const val FLOAT_WINDOW_RADIUS_DP = 24

    /**
     * 判断是否为大屏设备（折叠屏展开态/平板）。
     * 与 [com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView.isLargeScreen] 口径一致：
     * 宽高比 > 0.7 视为宽屏。
     */
    fun isLargeScreen(context: Context): Boolean {
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val aspectRatio = width.toFloat() / height.toFloat()
        return aspectRatio > 0.7f
    }

    /**
     * 计算浮窗尺寸：固定宽高比，根据屏幕方向和比例决定撑满方向。
     * 与 [com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView.calculateFloatWindowSize] 公式一致。
     *
     * @return Pair(width, height)，单位 px
     */
    fun calculateWindowSize(context: Context): Pair<Int, Int> {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            FLOAT_WINDOW_MARGIN_DP.toFloat(),
            displayMetrics
        ).toInt()

        val usableWidth = screenWidth - marginPx * 2
        val usableHeight = screenHeight - marginPx * 2

        val targetAspectRatio = FLOAT_WINDOW_ASPECT_RATIO
        val isLandscape = screenWidth > screenHeight

        var width: Int
        var height: Int

        if (isLandscape) {
            height = usableHeight
            width = (height * targetAspectRatio).toInt()
            if (width > usableWidth) {
                width = usableWidth
                height = (width / targetAspectRatio).toInt()
            }
        } else {
            width = usableWidth
            height = (width / targetAspectRatio).toInt()
            if (height > usableHeight) {
                height = usableHeight
                width = (height * targetAspectRatio).toInt()
            }
        }

        return Pair(width, height)
    }

    /**
     * 把 dp 值转换为 px。
     */
    fun dpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }
}
