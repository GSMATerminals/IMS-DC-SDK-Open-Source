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

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.blankj.utilcode.util.Utils

object FlavorUtils {
    private const val TAG = "FlavorUtils"

    const val CHANNEL_NORMAL = "normal"
    const val CHANNEL_DIALER = "dialer"
    const val CHANNEL_SAMSUNG = "samsung"
    const val CHANNEL_LOCAL = "local"

    /**
     * 获取渠道名
     * @return 如果没有获取成功，那么返回值为空
     */
    fun getChannelName(): String {
        var channelName = ""
        try {
            val packageManager: PackageManager = Utils.getApp().packageManager
            //注意此处为ApplicationInfo
            val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(Utils.getApp().packageName, PackageManager.GET_META_DATA)
            if (applicationInfo.metaData != null) {
                channelName = applicationInfo.metaData.getString("CHANNEL").toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return channelName
    }

}