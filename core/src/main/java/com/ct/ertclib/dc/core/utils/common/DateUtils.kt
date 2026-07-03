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

import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    fun timestampToDateTime(timestamp: Long): String {
        return try {
            // 定义目标格式：yyyy-MM-dd HH:mm
            val pattern = if (isTodayStamp(timestamp)) {
                "HH:mm"
            } else {
                "MM/dd  HH:mm"
            }
            val dateFormat = SimpleDateFormat(pattern, Locale.CHINA).apply {
                // 关键：指定东八区时区，避免时间偏移（如少8小时）
                timeZone = TimeZone.getTimeZone("GMT+8")
            }
            // 时间戳转Date，再格式化
            dateFormat.format(Date(timestamp))
        } catch (e: Exception) {
            // 捕获异常（如时间戳为负数），返回空字符串
            ""
        }
    }

    fun isTodayStamp(timeStamp: Long): Boolean {
        val zoneId = ZoneId.of("GMT+8")
        // 目标时间戳转本地日期
        val targetDate = Instant.ofEpochMilli(timeStamp)
            .atZone(zoneId)
            .toLocalDate()
        // 当前本地日期
        val todayDate = LocalDate.now(zoneId)
        // 比较日期是否相同
        return targetDate == todayDate
    }
}