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
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 震动管理器
 * 用于来电响铃时的震动控制
 */
object VibratorHelper {

    private var vibrator: Vibrator? = null

    /**
     * 开始震动
     * @param context 上下文
     * @param pattern 震动模式，默认：震动500ms，间隔500ms，循环
     */
    fun startVibration(context: Context, pattern: LongArray = longArrayOf(500, 500)) {
        try {
            val vibrator = getVibrator(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0 及以上
                val vibrationEffect = VibrationEffect.createWaveform(pattern, 0)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 停止震动
     */
    fun stopVibration() {
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getVibrator(context: Context): Vibrator {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }
        return vibrator!!
    }
}