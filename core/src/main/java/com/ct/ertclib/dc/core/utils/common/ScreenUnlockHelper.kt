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

import android.app.Activity
import android.app.KeyguardManager
import android.app.KeyguardManager.KeyguardDismissCallback
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.ct.ertclib.dc.core.utils.logger.Logger

/**
 * 屏幕解锁辅助类
 */
object ScreenUnlockHelper {

    private const val TAG = "ScreenUnlockHelper"
    private val logger = Logger.getLogger(TAG)

    private var callback: UnlockCallback? = null

    interface UnlockCallback {
        fun onSuccess()
        fun onFailed()
    }

    /**
     * 请求解锁屏幕
     */
    fun requestUnlock(context: Context, callback: UnlockCallback) {
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        val locked = keyguardManager.isKeyguardLocked
        logger.info("screen is locked:$locked")
        if (locked) {
            this.callback = callback
            val intent = Intent(context, UnlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            callback.onSuccess()
        }
    }

    internal fun onResult(success: Boolean) {
        if (success) {
            callback?.onSuccess()
        } else {
            callback?.onFailed()
        }
        callback = null
    }

    /**
     * 解锁屏幕的透明 Activity
     */
    class UnlockActivity : Activity() {
        private val TAG = "UnlockActivity"
        private val logger = Logger.getLogger(TAG)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // 设置透明窗口
            window.setBackgroundDrawableResource(android.R.color.white)
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            window.setLayout(1, 1)

            val keyguardManager = getSystemService(KeyguardManager::class.java)
            val locked = keyguardManager.isKeyguardLocked
            logger.info("screen is locked:$locked")
            if (locked) {
                keyguardManager.newKeyguardLock("unLock")
                keyguardManager.requestDismissKeyguard(this, object : KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        finishWithResult(true)
                    }

                    override fun onDismissCancelled() {
                        finishWithResult(false)
                    }

                    override fun onDismissError() {
                        finishWithResult(false)
                    }
                })
            } else {
                finishWithResult(true)
            }
        }

        private fun finishWithResult(success: Boolean) {
            onResult(success)
            finish()
        }

        override fun onBackPressed() {
            finishWithResult(false)
        }
    }
}