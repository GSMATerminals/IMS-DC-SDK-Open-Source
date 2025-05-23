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

package com.ct.ertclib.dc.core.utils.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.view.OrientationEventListener
import android.view.WindowManager
import com.ct.ertclib.dc.core.port.common.IScreenChangedCallback
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ScreenUtils: KoinComponent {

    const val DEVICE_ROTATION_0 = 0
    const val DEVICE_ROTATION_90 = 90
    const val DEVICE_ROTATION_180 = 180
    const val DEVICE_ROTATION_270 = 270

    private const val TAG = "ScreenUtils"

    private val applicationContext: Context by inject()

    private const val NAVIGATION_BAR_HEIGHT = "navigation_bar_height"
    private const val STATUS_BAR_HEIGHT = "status_bar_height"
    private const val DIMEN_PARAM = "dimen"
    private const val ANDROID_PARAM = "android"

    private const val ROTATION_80 = 80
    private const val ROTATION_160 = 160
    private const val ROTATION_200 = 200
    private const val ROTATION_280 = 280

    private val listenerList: ArrayList<IScreenChangedCallback> = ArrayList()
    private var deviceOrientation = DEVICE_ROTATION_0

    private val orientationListener = object : OrientationEventListener(applicationContext) {
        override fun onOrientationChanged(orientation: Int) {
            val nowDeviceOrientation =
                when (orientation) {
                    in (ROTATION_80 + 1)..ROTATION_160 -> {
                        DEVICE_ROTATION_90
                    }

                    in (ROTATION_160 + 1)..ROTATION_200 -> {
                        DEVICE_ROTATION_180
                    }

                    in (ROTATION_200 + 1)..ROTATION_280 -> {
                        DEVICE_ROTATION_270
                    }

                    else -> {
                        DEVICE_ROTATION_0
                    }
                }
            if (nowDeviceOrientation != deviceOrientation) {
                deviceOrientation = nowDeviceOrientation
                listenerList.forEach {
                    it.onScreenChanged(deviceOrientation)
                }
            }
        }
    }

    fun registerListener() {
        LogUtils.debug(TAG, "registerListener")
        orientationListener.enable()
    }

    fun unRegisterListener() {
        LogUtils.debug(TAG, "unRegisterListener")
        listenerList.clear()
        orientationListener.disable()
    }

    fun addScreenChangedListener(listener: IScreenChangedCallback) {
        LogUtils.debug(TAG, "addScreenChangedListener")
        listenerList.add(listener)
    }

    fun removeScreenChangedListener(listener: IScreenChangedCallback) {
        LogUtils.debug(TAG, "removeScreenChangedListener")
        listenerList.remove(listener)
    }

    /**
     * Return the width of screen, in pixel.
     *
     * @return the width of screen, in pixel
     */

    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val point = Point()
        wm?.defaultDisplay?.getRealSize(point)
        return point.x
    }

    /**
     * Return the height of screen, in pixel.
     *
     * @return the height of screen, in pixel
     */
    fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val point = Point()
        wm?.defaultDisplay?.getRealSize(point)
        return point.y
    }

    /**
     * Return the height of navigation bar, in pixel.
     *
     * @return the height of navigation bar, in pixel
     */
    @SuppressLint("InternalInsetResource")
    fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resources = context.resources
        val resourceId =
            resources.getIdentifier(NAVIGATION_BAR_HEIGHT, DIMEN_PARAM, ANDROID_PARAM)
        if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
        return result
    }

    fun dp2px(context: Context, dp: Float): Float {
        val scale = context.resources.displayMetrics.density
        return dp * scale + 0.5f
    }

    @SuppressLint("InternalInsetResource")
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resources = context.resources
        val resourceId = resources.getIdentifier(STATUS_BAR_HEIGHT, DIMEN_PARAM, ANDROID_PARAM)
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}