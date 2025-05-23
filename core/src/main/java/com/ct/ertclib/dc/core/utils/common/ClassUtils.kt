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

import android.app.ActivityManager
import android.content.Context
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity

object ClassUtils {

    fun getRunAppClassName(context: Context, cls: Class<out MiniAppActivity>): String {
        val nameBuilder = StringBuilder()
        nameBuilder.append(context.packageName)
        val simpleName = cls.simpleName
        nameBuilder.append(":")
        for (i in simpleName.indices) {
            val element = simpleName[i]
            if (i == 0 && element.isUpperCase()) {
                nameBuilder.append(element.lowercase())
            } else if (i == 0 && element.isDigit()) {
                nameBuilder.append(element)
            } else if (element.isUpperCase() || element.isDigit()) {
                nameBuilder.append("_")
                nameBuilder.append(element.lowercase())
            } else {
                nameBuilder.append(element)
            }
        }
        return nameBuilder.toString()
    }

    fun isAppClassRunning(context: Context, runName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses

        val iterator = runningAppProcesses.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.processName == runName) {
                return true
            }
        }
        return false
    }

}