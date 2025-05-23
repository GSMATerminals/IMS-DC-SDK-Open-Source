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

package com.ct.ertclib.dc.core.port.manager

import android.content.Context

interface IScreenShareManager {

    /**
     * 开始屏幕共享接口
     * @return 是否开启成功
     */
    fun startShareScreen(): Boolean

    /**
     * 结束屏幕共享
     *
     */
    fun stopShareScreen()

    /**
     * 查询设备是否支持屏幕共享能力
     * @return 是否支持屏幕共享能力
     */
    fun requestScreenShareAbility(): Boolean

    /**
     * 资源初始化
     */
    fun initManager()

    /**
     * 释放资源
     */
    fun release()

    fun isSharing(): Boolean
}