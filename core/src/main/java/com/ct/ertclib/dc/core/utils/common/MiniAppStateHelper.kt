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
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import com.ct.ertclib.dc.core.utils.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 小程序状态辅助类
 */
class MiniAppStateHelper(
    private val context: Context,
    private val callId: String,
    private val appId: String
) {
    companion object {
        private const val TAG = "MiniAppStateHelper"
        private const val QUERY_TIMEOUT_MS = 300L
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_APP_ID = "extra_app_id"

        // 启动模式标志
        const val EXTRA_LAUNCH_MODE = "extra_launch_mode"
        const val LAUNCH_MODE_NORMAL = 0      // 正常启动，会回调 onResume
        const val LAUNCH_MODE_SILENT = 1      // 静默启动，不回调 onResume

        // 全局事件总线
        private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
        val events = _events.asSharedFlow()

        fun tryPost(event: Event) {
            Logger.getLogger(TAG).debug("MiniAppStateHelper tryPost event: $event")
            _events.tryEmit(event)
        }
    }

    /**
     * 事件定义
     */
    sealed class Event {
        // 系统生命周期事件
        data class SystemPause(val callId: String, val appId: String) : Event()
        data class SystemResume(val callId: String, val appId: String, val shouldCallback: Boolean = true) : Event()
        data class SystemBackPressed(val callId: String, val appId: String) : Event()

        // 主动控制事件
        data class MoveToBack(val callId: String, val appId: String, val shouldCallback: Boolean = true) : Event()
        data class BringToFront(val callId: String, val appId: String) : Event()

        // 生命周期事件
        data class Finish(val callId: String, val appId: String) : Event()

        // 查询事件
        data class QueryAlive(val callId: String, val appId: String, val queryId: String) : Event()
        data class AliveResponse(val callId: String, val appId: String, val queryId: String) : Event()
    }

    private val logger = Logger.getLogger("MiniAppStateHelper-$callId-$appId")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var eventCollectionJob: kotlinx.coroutines.Job? = null

    interface Callback {
        fun onPause()
        fun onResume()
        fun onBackPressed()
    }

    fun startListen(callback: Callback) {
        logger.info("MiniAppStateHelper startListen - callId: $callId, appId: $appId")
        eventCollectionJob = scope.launch {
            events.collect { event ->
                when (event) {
                    is Event.SystemPause -> {
                        if (event.callId == callId && event.appId == appId) {
                            logger.info("MiniAppStateHelper SystemPause matched, calling onPause - callId: ${event.callId}, appId: ${event.appId}")
                            callback.onPause()
                        }
                    }
                    is Event.SystemResume -> {
                        if (event.callId == callId && event.appId == appId) {
                            // 根据 shouldCallback 标志决定是否回调
                            logger.info("MiniAppStateHelper SystemResume matched, calling onResume - callId: ${event.callId}, appId: ${event.appId} , shouldCallback: ${event.shouldCallback}")
                            if (event.shouldCallback) {
                                callback.onResume()
                            }
                        }
                    }
                    is Event.SystemBackPressed -> {
                        if (event.callId == callId && event.appId == appId) {
                            logger.info("MiniAppStateHelper SystemBackPressed matched, calling onBackPressed - callId: ${event.callId}, appId: ${event.appId}")
                            callback.onBackPressed()
                        }
                    }
                    is Event.AliveResponse -> {
                        if (event.callId == callId && event.appId == appId) {
                            logger.debug("MiniAppStateHelper AliveResponse received - queryId: ${event.queryId} - callId: ${event.callId}, appId: ${event.appId}")
                        }
                    }
                    else -> {}
                }
            }
        }

        // 正常启动，会回调 onResume
        startActivity(launchMode = LAUNCH_MODE_NORMAL)
    }

    private fun startActivity(launchMode: Int = LAUNCH_MODE_NORMAL) {
        logger.info("MiniAppStateHelper startActivity - callId: $callId, appId: $appId, launchMode: $launchMode")
        try {
            val intent = Intent(context, MiniAppStateActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_APP_ID, appId)
                putExtra(EXTRA_LAUNCH_MODE, launchMode)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            logger.error("MiniAppStateHelper startActivity failed", e)
        }
    }

    /**
     * 查询 Activity 是否存活
     */
    private suspend fun isActivityAlive(): Boolean {
        val queryId = System.currentTimeMillis().toString()
        logger.info("MiniAppStateHelper isActivityAlive - queryId: $queryId, callId: $callId, appId: $appId")

        val resultChannel = Channel<Boolean>(capacity = 1)

        val job = scope.launch {
            events.collect { event ->
                if (event is Event.AliveResponse &&
                    event.callId == callId &&
                    event.appId == appId &&
                    event.queryId == queryId) {
                    logger.debug("MiniAppStateHelper isActivityAlive - received AliveResponse for queryId: $queryId")
                    resultChannel.trySend(true)
                }
            }
        }

        tryPost(Event.QueryAlive(callId, appId, queryId))

        val isAlive = withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            resultChannel.receive()
        } ?: false

        job.cancel()
        resultChannel.close()

        logger.info("MiniAppStateHelper isActivityAlive - result: $isAlive, queryId: $queryId")
        return isAlive
    }

    /**
     * 最小化 - 静默模式，不会触发 onPause 回调
     */
    fun minimizeActivity() {
        logger.info("MiniAppStateHelper minimizeActivity - callId: $callId, appId: $appId")
        // 静默最小化，不触发 onPause 回调
        tryPost(Event.MoveToBack(callId, appId, shouldCallback = false))
    }

    /**
     * 最大化 - 静默模式，不会触发 onResume 回调
     */
    fun maximizeActivity() {
        logger.info("MiniAppStateHelper maximizeActivity - callId: $callId, appId: $appId")

        scope.launch {
            val isAlive = isActivityAlive()
            logger.info("MiniAppStateHelper maximizeActivity - isAlive: $isAlive")

            if (isAlive) {
                tryPost(Event.Finish(callId, appId))
                delay(200)
            }
            // 静默启动，不会触发 onResume 回调
            startActivity(launchMode = LAUNCH_MODE_SILENT)
        }
    }

    fun stopListen() {
        logger.info("MiniAppStateHelper stopListen - callId: $callId, appId: $appId")
        tryPost(Event.Finish(callId, appId))
        cleanup()
    }

    private fun cleanup() {
        logger.info("MiniAppStateHelper cleanup - callId: $callId, appId: $appId")
        eventCollectionJob?.cancel()
        eventCollectionJob = null
    }
}

/**
 * 透明 Activity
 */
class MiniAppStateActivity : Activity() {
    companion object {
        private const val TAG = "MiniAppStateHelper"
        private val logger = Logger.getLogger(TAG)
    }

    private var callId: String? = null
    private var appId: String? = null
    private var launchMode: Int = MiniAppStateHelper.LAUNCH_MODE_NORMAL
    private var hasResumedBefore = false
    private var shouldCallbackOnPause = true
    private var eventJob: kotlinx.coroutines.Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callId = intent.getStringExtra(MiniAppStateHelper.EXTRA_CALL_ID) ?: ""
        appId = intent.getStringExtra(MiniAppStateHelper.EXTRA_APP_ID) ?: ""
        launchMode = intent.getIntExtra(MiniAppStateHelper.EXTRA_LAUNCH_MODE, MiniAppStateHelper.LAUNCH_MODE_NORMAL)

        logger.info("MiniAppStateHelper MiniAppStateActivity onCreate - callId: $callId, appId: $appId")

        startEventListening()
        overridePendingTransition(0, 0)
    }

    private fun startEventListening() {
        logger.debug("MiniAppStateHelper MiniAppStateActivity startEventListening - callId: $callId, appId: $appId")
        eventJob = scope.launch {
            MiniAppStateHelper.events.collect { event ->
                when (event) {
                    is MiniAppStateHelper.Event.Finish -> {
                        if (event.callId == callId && event.appId == appId) {
                            shouldCallbackOnPause = false
                            logger.info("MiniAppStateHelper MiniAppStateActivity Finish event received, finishing activity - callId: ${event.callId}, appId: ${event.appId}")
                            finish()
                        }
                    }
                    is MiniAppStateHelper.Event.QueryAlive -> {
                        if (event.callId == callId && event.appId == appId) {
                            logger.info("MiniAppStateHelper MiniAppStateActivity QueryAlive received - queryId: ${event.queryId}, responding - callId: ${event.callId}, appId: ${event.appId}")
                            MiniAppStateHelper.tryPost(
                                MiniAppStateHelper.Event.AliveResponse(
                                    callId = event.callId,
                                    appId = event.appId,
                                    queryId = event.queryId
                                )
                            )
                        }
                    }
                    is MiniAppStateHelper.Event.MoveToBack -> {
                        if (event.callId == callId && event.appId == appId) {
                            logger.info("MiniAppStateHelper MiniAppStateActivity MoveToBack received, moving to back - callId: ${event.callId}, appId: ${event.appId} - isFinishing: $isFinishing, isDestroyed: $isDestroyed")
                            if (!isFinishing && !isDestroyed) {
                                // 保存 shouldCallback 标志，供 onPause 使用
                                shouldCallbackOnPause = event.shouldCallback
                                moveTaskToBack(false)
                            }
                        }
                    }
                    is MiniAppStateHelper.Event.BringToFront -> {
                        if (event.callId == callId && event.appId == appId) {
                            logger.info("MiniAppStateHelper MiniAppStateActivity BringToFront received, bringing to front - callId: ${event.callId}, appId: ${event.appId}")
                            bringToFront()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun bringToFront() {
        if (!isFinishing && !isDestroyed) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = activityManager.appTasks

            for (task in tasks) {
                val baseIntent = task.taskInfo.baseIntent
                if (baseIntent.getStringExtra(MiniAppStateHelper.EXTRA_CALL_ID) == callId &&
                    baseIntent.getStringExtra(MiniAppStateHelper.EXTRA_APP_ID) == appId) {
                    task.moveToFront()
                    return
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(0, 0)
        logger.info("MiniAppStateHelper MiniAppStateActivity onResume - callId: $callId, appId: $appId, launchMode: $launchMode, hasResumedBefore: $hasResumedBefore")

        callId?.let { cid -> appId?.let { aid ->
            // 决定是否应该回调 onResume
            // 1. 正常模式 (LAUNCH_MODE_NORMAL): 总是回调
            // 2. 静默模式 (LAUNCH_MODE_SILENT): 只有在已经 resume 过的情况下才回调（从后台恢复）
            // 3. 首次启动且是静默模式：不回调
            val shouldCallback = when (launchMode) {
                MiniAppStateHelper.LAUNCH_MODE_NORMAL -> true
                MiniAppStateHelper.LAUNCH_MODE_SILENT -> hasResumedBefore
                else -> true
            }

            logger.info("onResume - shouldCallback: $shouldCallback")
            MiniAppStateHelper.tryPost(
                MiniAppStateHelper.Event.SystemResume(cid, aid, shouldCallback)
            )
        } }

        // 标记已经 resume 过
        hasResumedBefore = true
    }

    override fun onPause() {
        super.onPause()
        logger.info("MiniAppStateHelper MiniAppStateActivity onPause - callId: $callId, appId: $appId")

        // 根据 shouldCallbackOnPause 标志决定是否发送 SystemPause 事件
        if (shouldCallbackOnPause) {
            callId?.let { cid -> appId?.let { aid ->
                MiniAppStateHelper.tryPost(MiniAppStateHelper.Event.SystemPause(cid, aid))
            } }
        } else {
            logger.debug("MiniAppStateHelper MiniAppStateActivity onPause - skip callback")
            // 重置标志
            shouldCallbackOnPause = true
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        logger.info("MiniAppStateHelper MiniAppStateActivity onBackPressed - callId: $callId, appId: $appId")
        callId?.let { cid -> appId?.let { aid ->
            MiniAppStateHelper.tryPost(MiniAppStateHelper.Event.SystemBackPressed(cid, aid))
        } }
    }

    override fun finish() {
        logger.info("MiniAppStateHelper MiniAppStateActivity finish - callId: $callId, appId: $appId")
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        eventJob?.cancel()
        eventJob = null
        logger.info("MiniAppStateHelper MiniAppStateActivity onDestroy - callId: $callId, appId: $appId")
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        finish()
        return super.onTouchEvent(event)
    }
}