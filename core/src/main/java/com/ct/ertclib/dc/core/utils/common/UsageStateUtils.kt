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

package com.ct.ertclib.dc.core.utils.common

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ct.ertclib.dc.core.utils.logger.Logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object UsageStateUtils: KoinComponent {
    private const val TAG = "UsageStateUtils"
    private val inCallActivityArray = arrayOf(
        "com.android.incallui.InCallActivity",
        "com.android.incallui.LegacyInCallActivity",
        "com.ct.ertclib.dc.app.MainActivity",
        "com.ct.ertclib.dc.feature.testing.LocalTestingMainActivity",
        "com.android.incallui.OplusInCallActivity",
        "com.android.incallui.call.InCallActivity"
    )
    private val sLogger: Logger = Logger.getLogger(TAG)
    private val context: Context by inject()

    @Volatile
    private var isInCallTop: Boolean = false
    private var isExpandedActivityTop: Boolean = false

    private val usageStatsManager by lazy { context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager }

    @JvmStatic
    fun isInCallOnTop(): Boolean {
        val topClassName = getTopClassName(context)
        if (topClassName == null) {
            return isInCallTop
        } else {
            isInCallTop = inCallActivityArray.contains(topClassName)
            return isInCallTop
        }
    }

    fun isMiniAppExpandedActivityShow(): Boolean {
        val topClassName = getTopClassName(context)
        if (topClassName == null) {
            return isExpandedActivityTop
        } else {
            isExpandedActivityTop = "com.ct.ertclib.dc.app.ui.activity.MiniAppExpandedActivity" == topClassName
            return isExpandedActivityTop
        }
    }

    @JvmStatic
    //获取当前栈顶的activity的类名，用于判断当前是否在前台
    private fun getTopClassName(context: Context): String? {
        val currentTime = System.currentTimeMillis()
        var className: String? = null
        val periodTime = 1000 * 10
        try {
            val queryEvents = usageStatsManager.queryEvents(currentTime - periodTime, currentTime)
            val event = UsageEvents.Event()
            while (queryEvents.hasNextEvent()) {
                queryEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    className = event.className
                }
            }
        }catch (e:Exception){
            sLogger.debug("getTopClassName error: ${e.message}")
            e.printStackTrace()
        }
        return className
    }

}