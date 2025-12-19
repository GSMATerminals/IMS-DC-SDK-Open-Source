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

package com.ct.ertclib.dc.core.usecase.main

import android.content.Context
import android.graphics.RectF
import android.provider.Settings
import com.ct.ertclib.dc.core.constants.OEMECConstants
import com.ct.ertclib.dc.core.constants.OEMECConstants.FUNCTION_ON_VIDEO_SHOW_INFO
import com.ct.ertclib.dc.core.constants.OEMECConstants.MODULE_NEW_CALL_SDK
import com.ct.ertclib.dc.core.data.common.ECBaseData
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_DRAWING_INO_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SCREEN_SIZE_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SKETCH_STATUS_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_VIDEO_WINDOW_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.NOTIFY_DRAWING_INFO_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.NOTIFY_HEIGHT_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.NOTIFY_STATUS_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.NOTIFY_WIDTH_PARAM
import com.ct.ertclib.dc.core.data.common.VideoInfo
import com.ct.ertclib.dc.core.data.event.NotifyEvent
import com.ct.ertclib.dc.core.port.common.IParentToMiniNotify
import com.ct.ertclib.dc.core.port.listener.ISketchWindowListener
import com.ct.ertclib.dc.core.port.manager.ISketchManager
import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.port.usecase.main.ISketchBoardUseCase
import com.ct.ertclib.dc.core.data.screenshare.DrawingInfo
import com.ct.ertclib.dc.core.manager.common.ExpandingCapacityManager
import com.ct.ertclib.dc.core.port.common.IScreenChangedCallback
import com.ct.ertclib.dc.core.port.expandcapacity.IExpandingCapacityListener
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

class SketchBoardUseCase(
    private val context: Context,
    private val screenShareSketchManager: ISketchManager,
    private val parentToMiniNotifier: IParentToMiniNotify,
    private val screenShareUseCase: IScreenShareUseCase
    ): ISketchBoardUseCase, KoinComponent {

    companion object {
        private const val TAG = "SketchBoardUseCase"
        private const val STATUS_CLOSE = "close"
        private const val STATUS_OPEN = "open"
    }

    private var callId = ""
    private var appId = ""
    private val logger = Logger.getLogger(TAG)
    private val applicationContext: Context by inject()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    //传递给终端，方便终端计算视频流信息的坐标点
    private var remoteScreenWidth: Int = 0
    private var remoteScreenHeight: Int = 0

    private val screenChangedCallback = object : IScreenChangedCallback {
        override fun onScreenChanged(rotation: Int) {
            LogUtils.debug(TAG, "onScreenChanged, rotation: $rotation")
            scope.launch {
                delay(1000)
                requestVideoInfo()
            }
        }
    }

    override fun openSketchBoard(telecomCallId: String, appId: String, paintColor: String, paintWidth: Float) {
        logger.info("openSketchBoard")
        this.appId = appId
        this.callId = telecomCallId
        screenShareUseCase.setCallIdAndAppId(telecomCallId, appId)
        if (!Settings.canDrawOverlays(context)) {
            logger.info("openSketchBoard, return")
            return
        }
        val screenSizeNotifyEvent = NotifyEvent(
            FUNCTION_SCREEN_SIZE_NOTIFY,
            mapOf(
                NOTIFY_WIDTH_PARAM to ScreenUtils.getScreenWidth(context),
                NOTIFY_HEIGHT_PARAM to ScreenUtils.getScreenHeight(context)
            )
        )

        parentToMiniNotifier.notifyEvent(callId, appId, screenSizeNotifyEvent)
        screenShareSketchManager.showSketchControlWindow(paintColor, paintWidth)
        ScreenUtils.addScreenChangedListener(screenChangedCallback)
    }

    override fun closeSketchBoard(needNotifyToMini: Boolean) {
        logger.info("closeSketchBoard")
        screenShareSketchManager.exitSketchControlWindow()
        if (needNotifyToMini) {
            val closeBoardNotifyEvent = NotifyEvent(
                FUNCTION_SKETCH_STATUS_NOTIFY,
                mapOf(NOTIFY_STATUS_PARAM to STATUS_CLOSE)
            )
            parentToMiniNotifier.notifyEvent(callId, appId, closeBoardNotifyEvent)
        }
        ScreenUtils.removeScreenChangedListener(screenChangedCallback)
    }

    override fun addDrawingInfo(drawingInfo: String) {
        val drawData = JsonUtil.fromJson(drawingInfo, DrawingInfo::class.java)
        LogUtils.debug(TAG, "addDrawingInfo, drawData: $drawData")
        drawData?.let {
            screenShareSketchManager.addSketchInfo(it)
        } ?: run {
            LogUtils.warn(TAG, "addDrawingInfo drawData is null")
        }
    }

    override fun addRemoteSizeInfo(width: Int, height: Int) {
        remoteScreenWidth = width
        remoteScreenHeight = height
        requestVideoInfo()
    }

    override fun addRemoteWindowSizeInfo(width: Int, height: Int) {
        screenShareSketchManager.setRemoteWindowSize(width.toFloat(), height.toFloat())
    }

    override fun initManager() {
        screenShareSketchManager.initManager()
        screenShareSketchManager.setSketchWindowListener(sketchListener)
        initETEC()
    }

    override fun release() {
        screenShareSketchManager.release()
        unRegisterETEC()
    }

    private fun requestVideoInfo() {
        LogUtils.debug(TAG, "requestVideoInfo")
        ExpandingCapacityManager.instance.request(
            applicationContext,
            TAG,
            TAG,
            "{\"provider\":\"${ExpandingCapacityManager.OEM}\",\"module\":\"${MODULE_NEW_CALL_SDK}\",\"func\":\"${OEMECConstants.FUNCTION_GET_VIDEO_SHOW_INFO}\",\"data\":{\"shareDeviceWidth\":$remoteScreenWidth,\"shareDeviceHeight\":$remoteScreenHeight}}"
        )
    }

    private val sketchListener = object : ISketchWindowListener {
        override fun onExitBoardBtnClick() {
            closeSketchBoard(needNotifyToMini = true)
            screenShareUseCase.stopScreenShare(needNotifyToMini = true)
        }

        override fun onSketchEvent(drawingInfo: DrawingInfo) {
            logger.info("onSketchEvent")
            val drawingNotifyEvent = NotifyEvent(
                FUNCTION_DRAWING_INO_NOTIFY,
                mapOf(NOTIFY_DRAWING_INFO_PARAM to JsonUtil.toJson(drawingInfo))
            )
            parentToMiniNotifier.notifyEvent(callId, appId, drawingNotifyEvent)
            LogUtils.debug(TAG, "onSketchEvent param: ${JsonUtil.toJson(drawingInfo)}")
        }

        override fun onLocalWindowNotified(width: Float, height: Float) {
            val screenSizeNotifyEvent = NotifyEvent(
                FUNCTION_VIDEO_WINDOW_NOTIFY,
                mapOf(
                    NOTIFY_WIDTH_PARAM to width.toString(),
                    NOTIFY_HEIGHT_PARAM to height.toString()
                )
            )

            parentToMiniNotifier.notifyEvent(callId, appId, screenSizeNotifyEvent)
        }
    }

    private fun initETEC() {
        LogUtils.debug(TAG, "initETEC")
        val modules = ArrayList<String>()
        modules.add(MODULE_NEW_CALL_SDK)
        val providerModules = ConcurrentHashMap<String, ArrayList<String>>()
        providerModules[ExpandingCapacityManager.OEM] = modules
        ExpandingCapacityManager.instance.registerECListener(
            TAG,
            TAG,
            providerModules,
            object : IExpandingCapacityListener {
                override fun onCallback(content: String?) {
                    LogUtils.debug(TAG, "SketchBoardUseCase onCallback content: $content")
                    kotlin.runCatching {
                        content?.let {
                            val ecResponse = JsonUtil.fromJson(content, ECBaseData::class.java)
                            when (ecResponse?.func) {
                                FUNCTION_ON_VIDEO_SHOW_INFO -> {
                                    ecResponse.data?.let {
                                        val videoInfo = JsonUtil.convertVariableStringToClass(content, "data", VideoInfo::class.java)
                                        val rectString = videoInfo?.rect
                                        var rectF = RectF()
                                        rectString?.let {
                                            val rectSplitArray = it.split(",")
                                            if (rectSplitArray.size == 4) {
                                                rectF = RectF(rectSplitArray[0].toFloat(), rectSplitArray[1].toFloat(), rectSplitArray[2].toFloat(), rectSplitArray[3].toFloat())
                                            }
                                        }
                                        val rotation = videoInfo?.rotation?.toInt()
                                        if (rectF.height() != 0F && rectF.width() != 0F && rotation != null) {
                                            screenShareSketchManager.setLocalWindowInformation(rectF, rotation)
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }.onFailure {
                        LogUtils.error(TAG, "onCallback error: $it")
                    }
                }
            })
    }

    private fun unRegisterETEC() {
        LogUtils.debug(TAG, "unRegisterETEC")
        ExpandingCapacityManager.instance.unregisterECListener(applicationContext,
            TAG,
            TAG
        )
    }
}