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

package com.ct.ertclib.dc.core.manager.common;

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.manager.call.BDCManager
import com.ct.ertclib.dc.core.manager.call.DCManager
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.miniapp.MiniAppStartManager
import com.ct.ertclib.dc.core.port.common.IActivityManager
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import com.ct.ertclib.dc.core.utils.logger.LogConfig
import com.ct.ertclib.dc.core.utils.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class InCallServiceManager: KoinComponent {

    companion object {
        private const val TAG = "InCallServiceManager"

        val instance: InCallServiceManager by lazy {
            InCallServiceManager()
        }
    }
    private val sLogger: Logger = Logger.getLogger(TAG)
    private val activityManager: IActivityManager by inject()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var isInit = false

    fun onBind(context: Context,inCallService: InCallService?){
        isInit = true
        LogConfig.upDateLogEnabled()
        sLogger.info("onBind")
        // 不干涉通话流程，在这里更新一下版本号，在检查权限时做判断。可能在下次通话时才会提示用户。
        SDKPermissionUtils.updatePrivacyVersion()
        // 清除小程序SP过期数据
        scope.launch(Dispatchers.IO) {
            SPManager.instance.clearExpiredData()
        }
        scope.launch(Dispatchers.IO) {
            FileManager.instance.updateFiles(context)
        }
        scope.launch(Dispatchers.IO) {
            NewCallAppSdkInterface.emitCallState(NewCallAppSdkInterface.CALL_START)
        }
        ScreenUtils.registerListener()
        NewCallAppSdkInterface.init(context)
        NewCallsManager.instance.onCallServiceBind(inCallService)
    }

    fun onUnbind(){
        if (!isInit){
            sLogger.info("onUnbind was not init")
            return
        }
        sLogger.info("onUnbind")
        isInit = false
        scope.launch {
            // 这里不要立即执行，等onCallRemoved回调都执行完毕再执行这里,这里的所有操作必须允许重复执行
            delay(1000)
            DCManager.instance.onCallServiceUnbind(Utils.getApp())
            NewCallAppSdkInterface.emitCallState(NewCallAppSdkInterface.CALL_STOP)
            MiniAppManager.release()

            NewCallsManager.instance.onCallServiceUnBind()
            ScreenUtils.unRegisterListener()
            NewCallAppSdkInterface.release()

            // 只会结束主进程中的所有Activity，是合理的
            activityManager.finishAllActivity()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onCallAdded(callInfo: CallInfo, call: Call?){
        try {
            if (NewCallsManager.instance.isCallExist(callInfo.telecomCallId)){
                sLogger.info("onCallAdded was added")
                return
            }

            // 初始化本次通话小程序管理，并监听通话状态
            val miniAppManager = MiniAppManager(callInfo,MiniAppStartManager)
            NewCallsManager.instance.addCallStateListener(callInfo.telecomCallId,miniAppManager)

            // 初始化本次通话bdc管理，并监听通话状态
            val bdcManager = BDCManager(callInfo, miniAppManager)
            NewCallsManager.instance.addCallStateListener(callInfo.telecomCallId,bdcManager)

            // dc管理监听通话状态
            NewCallsManager.instance.addCallStateListener(callInfo.telecomCallId,DCManager.instance)

            // 触发本次通话的dc业务逻辑，去看上面三个模块的onCallAdded
            NewCallsManager.instance.onCallAdded(callInfo,call)
        } catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun onCallRemoved(callId: String?) {
        try {
            sLogger.info("onCallRemoved callId:$callId")
            callId?.let {
                NewCallsManager.instance.onCallRemoved(it)
            }

            // 防止系统没有回调onUnbind
            if (NewCallsManager.instance.isCallEmpty()){
                onUnbind()
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}
