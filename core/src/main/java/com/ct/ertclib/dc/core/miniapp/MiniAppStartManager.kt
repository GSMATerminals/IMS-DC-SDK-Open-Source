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
import android.content.Intent
import android.os.Build
import android.os.RemoteException
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.PathManager
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.common.Reason
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.service.MiniAppService
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.data.miniapp.MiniAppProperties
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity0
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity1
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity2
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity3
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity4
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity5
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity6
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity7
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity8
import com.ct.ertclib.dc.core.miniapp.ui.activity.MiniAppActivity9
import com.ct.ertclib.dc.core.utils.common.ClassUtils
import com.ct.ertclib.dc.core.port.miniapp.IMiniAppStartManager
import com.ct.ertclib.dc.core.port.miniapp.IMiniAppStartCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

object MiniAppStartManager : IMiniAppStartManager {
    private const val TAG = "MiniAppStartManager"
    private val sLogger: Logger = Logger.getLogger(TAG)
    private val mMiniAppInfoList = ArrayList<MiniAppInfoWrapper>()
    private var appService: MiniAppService? = null


    class MiniAppChecker(private val activityClass: Class<out MiniAppActivity>) :
            (MiniAppInfoWrapper) -> Boolean {
        override fun invoke(miniAppInfo: MiniAppInfoWrapper): Boolean {
            return miniAppInfo.activityClass == activityClass
        }
    }

    private fun startMiniAppInfo(miniAppInfo: MiniAppInfo, context: Context, callInfo: CallInfo?, miniAppListInfo: MiniAppList?, callback: IMiniAppStartCallback?) {
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        coroutineScope.launch(Dispatchers.IO) {
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
        if (sLogger.isDebugActivated) sLogger.debug("startMiniAppActivity miniAppInfp:$miniAppInfo")

        checkAllRunningApp(context)

        val runningMiniAppWrapper = getRunningMiniAppWrapper(miniAppInfo, callInfo)
        if (runningMiniAppWrapper != null) {
            val intent = Intent(context, runningMiniAppWrapper.activityClass)
            intent.putExtra("miniApp", runningMiniAppWrapper.miniApp)
            intent.putExtra("callInfo", runningMiniAppWrapper.callInfo)
            intent.putExtra("miniAppListInfo", miniAppListInfo)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_NEW_TASK)
            val coroutineScope = CoroutineScope(EmptyCoroutineContext)
            coroutineScope.launch(Dispatchers.Main) {
                delay(500)
                context.startActivity(intent)
            }
            return
        }

        val targetActivityClass = getTargetActivityClass(context, miniAppInfo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMiniAppInfoList.removeIf {
                MiniAppChecker(targetActivityClass).invoke(it)
            }
        }

        val miniAppInfoWrapper = MiniAppInfoWrapper(miniAppInfo, callInfo, targetActivityClass)
        mMiniAppInfoList.add(miniAppInfoWrapper)

        NewCallAppSdkInterface.emitCloseExpandedViewFlow(isClose = true)
        val intent = Intent(context, miniAppInfoWrapper.activityClass)
        intent.putExtra("miniApp", miniAppInfoWrapper.miniApp)
        intent.putExtra("callInfo", callInfo)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_NEW_TASK)
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        coroutineScope.launch(Dispatchers.Main) {
            delay(500)
            context.startActivity(intent)
        }
    }


    private fun getTargetActivityClass(
        context: Context,
        miniAppInfo: MiniAppInfo
    ): Class<out MiniAppActivity> {

        val clsArr = arrayOf(
            MiniAppActivity0::class.java,
            MiniAppActivity1::class.java,
            MiniAppActivity2::class.java,
            MiniAppActivity3::class.java,
            MiniAppActivity4::class.java,
            MiniAppActivity5::class.java,
            MiniAppActivity6::class.java,
            MiniAppActivity7::class.java,
            MiniAppActivity8::class.java,
            MiniAppActivity9::class.java
        )
        //如果没有有没有使用的app界面，直接返回使用拉起。
        clsArr.forEach {
            val runAppClassName = ClassUtils.getRunAppClassName(context, it)
            if (!ClassUtils.isAppClassRunning(context, runAppClassName)) {
                return it
            }
        }

        //如果没有空闲activity,那么根据properties 优先级结束最低优先级的小程序，返回此小程序界面
        val priority = miniAppInfo.appProperties?.priority

        val launchAppProperties = priority ?: -1
        val lowPropertiesMiniAppList = ArrayList<MiniAppInfoWrapper>()
        for (miniAppWrapper in mMiniAppInfoList) {
            val appProperties = miniAppWrapper.miniApp.appProperties?.priority
            if ((appProperties ?: -1) <= launchAppProperties) {
                lowPropertiesMiniAppList.add(miniAppWrapper)
            }
        }

        if (lowPropertiesMiniAppList.isEmpty()) {
            throw Exception("No low priority found value:$launchAppProperties")
        }
        var miniAppInfoWraper: MiniAppInfoWrapper?
        if (lowPropertiesMiniAppList.size == 1) {
            miniAppInfoWraper = lowPropertiesMiniAppList[0]
        } else {
            val iterator = lowPropertiesMiniAppList.iterator()
            miniAppInfoWraper = iterator.next()
            var desPriority = miniAppInfoWraper.miniApp.appProperties?.priority ?: -1
            do {
                val next = iterator.next()
                val nextPriority = next.miniApp.appProperties?.priority ?: -1
                if (desPriority > nextPriority) {
                    miniAppInfoWraper = next
                    desPriority = nextPriority
                }
            } while (iterator.hasNext())
        }

        if (miniAppInfoWraper != null) {
            finishMiniApp(miniAppInfoWraper.miniApp.callId,miniAppInfoWraper.miniApp.appId)
            return miniAppInfoWraper.activityClass
        }

        throw Exception("can not get target class")

    }

    private fun finishMiniApp(callId: String,appId: String) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("finishMiniApp, appId:$appId")
        }
        if (appService != null) {
            val iParentToMini = appService?.mParentToMiniCallbackMap?.get(appService?.getKey(callId,appId))
            if (iParentToMini != null) {
                try {
                    iParentToMini.finishMiniAppActivity()
                } catch (e: RemoteException) {
                    if (sLogger.isDebugActivated) sLogger.error("finishMiniApp", e)
                    appService!!.onRemoteError(callId,appId)
                }
            }
        }
    }

    private fun getRunningMiniAppWrapper(
        miniAppInfo: MiniAppInfo,
        callInfo: CallInfo?
    ): MiniAppInfoWrapper? {
        for (miniAppInfoWraper in mMiniAppInfoList) {
            if (miniAppInfoWraper.miniApp.appId == miniAppInfo.appId) {
                if (miniAppInfoWraper.miniApp.appProperties?.version?.let { miniAppInfo.appProperties?.version?.let { it1 ->
                        PathManager().compareVersion(it,
                            it1
                        )
                    } } == 1) {
                    miniAppInfoWraper.miniApp = miniAppInfo
                    if (callInfo != null) {
                        miniAppInfoWraper.callInfo = callInfo
                    }
                }
                return miniAppInfoWraper
            }
        }
        return null
    }

    private fun checkAllRunningApp(context: Context) {
        val list = ArrayList<MiniAppInfoWrapper>()
        mMiniAppInfoList.forEach {
            val runAppClassName = ClassUtils.getRunAppClassName(context, it.activityClass)
            if (!ClassUtils.isAppClassRunning(context, runAppClassName)) {
                list.add(it)
            }
        }
        mMiniAppInfoList.removeAll(list.toSet())
    }

    override fun startMiniApp(context: Context, miniAppInfo: MiniAppInfo, callInfo: CallInfo?, miniAppListInfo: MiniAppList?, callback: IMiniAppStartCallback?) {
        startMiniAppInfo(miniAppInfo, context, callInfo, miniAppListInfo, callback)
    }

    override fun stopMiniApp(context: Context, callId: String,appId: String) {
        finishMiniApp(callId,appId)
    }

    fun setMiniAppAidlService(appService: MiniAppService?) {
        this.appService = appService
    }

    fun sendMessageToMiniApp(callId: String,miniappId: String, message: String, callback: IMessageCallback?) {
        LogUtils.debug(TAG, "sendMessageToMiniApp, callId: $callId, miniappId: $miniappId, message: $message")
        appService?.let {
            it.mParentToMiniCallbackMap[it.getKey(callId,miniappId)]?.sendMessageToMini(miniappId, message, callback)
        }
    }

    override fun clearBackgroundTaskList() {
        mMiniAppInfoList.clear()
    }

    override fun moveMiniAppToFront(context: Context, appId: String) {
        val miniAppWrapper = mMiniAppInfoList.firstOrNull { it.miniApp.appId == appId }
        miniAppWrapper?.let {
            val intent = Intent(context, it.activityClass)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}