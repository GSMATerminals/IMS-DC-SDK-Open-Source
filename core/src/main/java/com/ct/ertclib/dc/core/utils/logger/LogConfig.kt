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

package com.ct.ertclib.dc.core.utils.logger

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object LogConfig {
    private var logEnable = true

    fun upDateLogEnabled() {
//        try {
//            val process = Runtime.getRuntime().exec("getprop ctnewcall.log.enabled")
//            val inputStream = process.inputStream
//            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
//            val value = bufferedReader.readLine()
//            logEnable = "true".equals(value, ignoreCase = true)
//            return
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        logEnable = false
    }

    fun isLogEnabled(): Boolean {
        return logEnable
    }
}
