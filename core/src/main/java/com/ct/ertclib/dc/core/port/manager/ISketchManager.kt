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

package com.ct.ertclib.dc.core.port.manager

import android.graphics.RectF
import com.ct.ertclib.dc.core.port.listener.ISketchWindowListener
import com.ct.ertclib.dc.core.data.screenshare.DrawingInfo

interface ISketchManager {

    /**
     *设置涂鸦窗口监听事件
     * @param listener: 涂鸦窗口事件回调
     */
    fun setSketchWindowListener(listener: ISketchWindowListener)

    /**
     *显示共享时的画板悬浮窗
     */
    fun showSketchControlWindow(paintColor: String, paintWidth: Float)

    /**
     *退出屏幕共享画板悬浮窗
     */
    fun exitSketchControlWindow()

    /**
     *设置本端屏幕窗口信息
     * @param rectF: 本端窗口矩阵
     * @param rotation: 屏幕旋转信息
     */
    fun setLocalWindowInformation(rectF: RectF, rotation: Int)

    /**
     *设置对端屏幕窗口信息
     * @param width: 对端屏幕宽度
     * @param height: 对端屏幕高度
     */
    fun setRemoteWindowSize(width: Float, height: Float)

    /**
     *添加涂鸦信息
     * @param sketchInfo: 涂鸦信息
     */
    fun addSketchInfo(sketchInfo: DrawingInfo)

    /**
     * 初始化
     */
    fun initManager()

    /**
     * 释放资源
     */
    fun release()
}