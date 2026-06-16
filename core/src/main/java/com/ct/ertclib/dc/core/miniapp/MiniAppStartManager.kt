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

package com.ct.ertclib.dc.core.miniapp

import android.content.Context
import android.view.WindowManager
import com.ct.ertclib.dc.core.manager.common.LicenseManager
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.PathManager
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.common.Reason
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppProperties
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView
import com.ct.ertclib.dc.core.port.miniapp.IMiniAppStartManager
import com.ct.ertclib.dc.core.port.miniapp.IMiniAppStartCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

object MiniAppStartManager : IMiniAppStartManager {
    private const val TAG = "MiniAppStartManager"
    private val sLogger: Logger = Logger.getLogger(TAG)
    private val mMiniAppInfoWrapperList = ArrayList<MiniAppInfoWrapper>()

    private fun startMiniAppInfo(miniAppInfo: MiniAppInfo, context: Context, callInfo: CallInfo?, miniAppListInfo: MiniAppList?, callback: IMiniAppStartCallback?) {
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        coroutineScope.launch(Dispatchers.IO) {
            if (miniAppInfo.path == null || !File(miniAppInfo.path).exists()){
                sLogger.warn("startMiniAppActivity path is null or not exist")
                callback?.onMiniAppStartFailed(Reason.START_FAILED)
                return@launch
            }
            val deferred = async {
                val file = File(miniAppInfo.path + "/properties.json")
                if (file.exists()) {
                    val propertiesString = file.readText()
                    if (sLogger.isDebugActivated) sLogger.debug("startMiniAppActivity propertiesString:$propertiesString")
                    JsonUtil.fromJson(propertiesString, MiniAppProperties::class.java)
                } else {
                    if (sLogger.isDebugActivated) sLogger.debug("startMiniAppActivity propertiesString null, path:${miniAppInfo.path}")
                    null
                }
            }
            val properties = deferred.await()
            if (sLogger.isDebugActivated) sLogger.debug("startMiniAppActivity properties:$properties")
            if (properties == null){
                callback?.onMiniAppStartFailed(Reason.START_FAILED)
                return@launch
            }
            if (properties.canStartedByOthers != true && miniAppInfo.isStartByOthers == true) {
                if (sLogger.isDebugActivated) sLogger.debug("startMiniAppActivity cannot start by others")
                callback?.onMiniAppStartFailed(Reason.START_FAILED)
                return@launch
            }
            miniAppInfo.appProperties = properties
            if (!LicenseManager.getInstance().verifyMiniAppFolder(miniAppInfo.path!!)){
                sLogger.info("startMiniAppActivity verify failed")
                callback?.onMiniAppStartFailed(Reason.START_FAILED)
                return@launch
            }
            coroutineScope.launch(Dispatchers.Main){
                startMiniAppActivity(context, miniAppInfo, callInfo, miniAppListInfo)
                // 回调启动成功
                callback?.onMiniAppStarted()
            }
        }
    }

    private fun startMiniAppActivity(
        context: Context,
        miniAppInfo: MiniAppInfo,
        callInfo: CallInfo?,
        miniAppListInfo: MiniAppList?
    ) {
        if (sLogger.isDebugActivated) sLogger.debug("startMiniAppActivity miniAppInfo:$miniAppInfo")


        val runningMiniAppWrapper = getRunningMiniAppWrapper(miniAppInfo, callInfo)
        if (runningMiniAppWrapper != null) {
            val coroutineScope = CoroutineScope(EmptyCoroutineContext)
            coroutineScope.launch(Dispatchers.Main) {
                sLogger.debug("startMiniAppActivity runningMiniAppWrapper:$runningMiniAppWrapper")
                runningMiniAppWrapper.view.show()
            }
            return
        }

        val miniAppInfoWrapper = MiniAppInfoWrapper(miniAppInfo, callInfo, MiniAppView(context))
        mMiniAppInfoWrapperList.add(miniAppInfoWrapper)

        // NewCallAppSdkInterface.emitCloseExpandedViewFlow(isClose = true)
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        coroutineScope.launch(Dispatchers.Main) {
            sLogger.debug("startMiniAppActivity addMiniAppView")
            addMiniAppView(context, miniAppInfoWrapper,miniAppListInfo)
        }
    }

    private fun finishMiniApp(callId: String,appId: String) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("finishMiniApp, appId:$appId")
        }
        val miniAppWrapper = mMiniAppInfoWrapperList.firstOrNull { it.miniApp.appId == appId && it.miniApp.callId == callId }
        if (miniAppWrapper == null) {
            sLogger.warn("finishMiniApp: MiniApp not found, appId:$appId")
            return
        }

        try {
            miniAppWrapper.view.finishAndKillMiniApp()
            sLogger.debug("Removed view for callId: $callId, app: $appId")
        } catch (e: Exception) {
            sLogger.error("Failed to remove view for callId: $callId, app: $appId", e)
        }

        sLogger.debug("Removed wrapper for callId: $callId, app: $appId, remaining count: ${mMiniAppInfoWrapperList.size}")
    }

    private fun getRunningMiniAppWrapper(
        miniAppInfo: MiniAppInfo,
        callInfo: CallInfo?
    ): MiniAppInfoWrapper? {
        for (miniAppInfoWrapper in mMiniAppInfoWrapperList) {
            if (miniAppInfoWrapper.miniApp.appId == miniAppInfo.appId && miniAppInfoWrapper.miniApp.callId == miniAppInfo.callId) {
                if (miniAppInfoWrapper.miniApp.appProperties?.version?.let { miniAppInfo.appProperties?.version?.let { it1 ->
                        PathManager().compareVersion(it,
                            it1
                        )
                    } } == 1) {
                    miniAppInfoWrapper.miniApp = miniAppInfo
                    if (callInfo != null) {
                        miniAppInfoWrapper.callInfo = callInfo
                    }
                }
                return miniAppInfoWrapper
            }
        }
        return null
    }

    override fun startMiniApp(context: Context, miniAppInfo: MiniAppInfo, callInfo: CallInfo?, miniAppListInfo: MiniAppList?, callback: IMiniAppStartCallback?) {
        startMiniAppInfo(miniAppInfo, context, callInfo, miniAppListInfo, callback)
    }

    override fun stopMiniApp(context: Context, callId: String,appId: String) {
        finishMiniApp(callId,appId)
    }

    override fun clearBackgroundTaskListByCall(callId: String) {
        mMiniAppInfoWrapperList.filter { it.miniApp.callId == callId }.forEach { wrapper ->
            try {
                wrapper.view.finishAndKillMiniApp()
            } catch (e: Exception) {
                sLogger.error("Failed to remove view for app: ${wrapper.miniApp.appId}", e)
            }
            mMiniAppInfoWrapperList.remove(wrapper)
        }
        sLogger.debug("Removed views, remaining count: ${mMiniAppInfoWrapperList.size}")
    }

    fun getRunningMiniApp(callId: String,appId: String): MiniAppView? {
        return mMiniAppInfoWrapperList.firstOrNull { it.miniApp.appId == appId && it.miniApp.callId == callId }?.view
    }

    private fun addMiniAppView(context: Context, miniAppInfoWrapper: MiniAppInfoWrapper, miniAppListInfo: MiniAppList?) {
        // 直接让 View 自己添加到 WindowManager
        miniAppInfoWrapper.view.attachToWindow()

        miniAppInfoWrapper.view.setControlListener(object : MiniAppView.ControlListener {
            override fun onToRemove() {
                // 移除 wrapper
                mMiniAppInfoWrapperList.remove(miniAppInfoWrapper)
                sLogger.debug("Removed wrapper for app: ${miniAppInfoWrapper.miniApp.appId}, remaining count: ${mMiniAppInfoWrapperList.size}")
            }
        })

        miniAppInfoWrapper.view.start(miniAppInfoWrapper.miniApp, miniAppInfoWrapper.callInfo, miniAppListInfo)
        sLogger.debug("addMiniAppView success, appId: ${miniAppInfoWrapper.miniApp.appId}")
    }
}