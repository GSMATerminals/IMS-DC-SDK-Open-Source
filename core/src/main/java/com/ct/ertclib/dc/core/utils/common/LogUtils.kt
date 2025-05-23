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

import com.ct.ertclib.dc.core.utils.logger.AndroidAppender
import com.ct.ertclib.dc.core.utils.logger.Appender
import com.ct.ertclib.dc.core.utils.logger.ExceptionUtil

object LogUtils {

    private const val DEBUG_LEVEL: Int = 0
    private const val INFO_LEVEL: Int = 1
    private const val WARN_LEVEL: Int = 2
    private const val ERROR_LEVEL: Int = 3
    private const val FATAL_LEVEL: Int = 4

    private var appenders: Array<Appender> = arrayOf(
        AndroidAppender()
    )

    @JvmStatic
    fun debug(tag: String, trace: String) {
        printTrace(tag, trace, DEBUG_LEVEL)
    }

    /**
     * Info trace
     *
     * @param trace Trace
     */
    @JvmStatic
    fun info(tag: String, trace: String) {
        printTrace(tag, trace, INFO_LEVEL)
    }

    /**
     * Warning trace
     *
     * @param trace Trace
     */
    @JvmStatic
    fun warn(tag: String, trace: String) {
        printTrace(tag, trace, WARN_LEVEL)
    }

    /**
     * Warning trace
     *
     * @param trace Trace
     * @param e Exception
     */
    @JvmStatic
    fun warn(tag: String, trace: String, e: Throwable?) {
        printTrace(tag, trace, WARN_LEVEL)
        printTrace(tag, ExceptionUtil.getFullStackTrace(e), WARN_LEVEL)
    }

    /**
     * Error trace
     *
     * @param trace Trace
     */
    @JvmStatic
    fun error(tag: String, trace: String) {
        printTrace(tag, trace, ERROR_LEVEL)
    }

    /**
     * Error trace
     *
     * @param trace Trace
     * @param e Exception
     */
    @JvmStatic
    fun error(tag: String, trace: String, e: Throwable?) {
        printTrace(tag, trace, ERROR_LEVEL)
        printTrace(tag, ExceptionUtil.getFullStackTrace(e), ERROR_LEVEL)
    }

    /**
     * Fatal trace
     *
     * @param trace Trace
     */
    @JvmStatic
    fun fatal(tag: String, trace: String) {
        printTrace(tag, trace, FATAL_LEVEL)
    }

    /**
     * Fatal trace
     *
     * @param trace Trace
     * @param e Exception
     */
    @JvmStatic
    fun fatal(tag: String, trace: String, e: Throwable) {
        printTrace(tag, trace, FATAL_LEVEL)
        printTrace(tag, ExceptionUtil.getFullStackTrace(e), FATAL_LEVEL)
    }


    @JvmStatic
    private fun printTrace(tag: String, trace: String, level: Int) {
        val traceString = trace.replace("\r", "")
        for (appender in appenders) {
            appender.printTrace(tag, level, traceString)
        }
    }
}