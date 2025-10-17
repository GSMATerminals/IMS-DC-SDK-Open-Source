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

package com.ct.ertclib.dc.core.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.sdkpermission.IPermissionCallback
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionHelper
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.constants.CommonConstants.MINI_APP_SP_EXPIRY_ITEM_SPLIT_KEY
import com.ct.ertclib.dc.core.constants.CommonConstants.MINI_APP_SP_EXPIRY_SPLIT_KEY
import com.ct.ertclib.dc.core.constants.CommonConstants.MINI_APP_SP_KEYS_KEY
import com.ct.ertclib.dc.core.utils.common.CallUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.manager.call.BDCManager
import com.ct.ertclib.dc.core.miniapp.MiniAppStartManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.manager.call.DCManager
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.manager.common.FileManager
import com.ct.ertclib.dc.core.port.common.IActivityManager
import com.ct.ertclib.dc.core.utils.logger.LogConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

class InCallServiceImpl : InCallService(), KoinComponent {

    companion object {
        private const val TAG = "InCallServiceImpl"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    private var mDcManager: DCManager? = null

    private var mCallsManager: NewCallsManager? = null

    private var hasInit = false
    private val activityManager: IActivityManager by inject()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mCallsMap = ConcurrentHashMap<String, Call>()

    private var checkPermissionAfterCall = false

    override fun onBind(intent: Intent?): IBinder? {
        LogConfig.upDateLogEnabled()
        sLogger.info("InCallServiceImpl onBind")
        // 不干涉通话流程，在这里更新一下版本号，在检查权限时做判断。可能在下次通话时才会提示用户。
        SDKPermissionUtils.updatePrivacyVersion()
        // 清除小程序SP过期数据
        scope.launch(Dispatchers.IO) {
            val keysStr = SPUtils.getInstance().getString(MINI_APP_SP_KEYS_KEY,"")
            val keys = keysStr.split(MINI_APP_SP_EXPIRY_ITEM_SPLIT_KEY)
            var builder = StringBuilder()
            keys.forEach {
                try {
                    val key = it.split(MINI_APP_SP_EXPIRY_SPLIT_KEY)[0]
                    val expiryTime = it.split(MINI_APP_SP_EXPIRY_SPLIT_KEY)[1].toLong()
                    if (System.currentTimeMillis() < expiryTime){
                        if (builder.isNotEmpty()){
                            builder.append(MINI_APP_SP_EXPIRY_ITEM_SPLIT_KEY)
                        }
                        builder.append(it)
                    } else {
                        SPUtils.getInstance().remove(key)
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
            SPUtils.getInstance().put(MINI_APP_SP_KEYS_KEY, builder.toString())
        }
        scope.launch(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(
                    this@InCallServiceImpl,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED){// 如果已经有权限了，就扫描更新
                FileManager.instance.updateFiles(this@InCallServiceImpl)
            }
        }
        return super.onBind(intent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCallAdded(call: Call?) {
        val permissionHelper = SDKPermissionHelper(Utils.getApp(),object : IPermissionCallback {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onAgree() {
                dealCall(call)
            }
            override fun onDenied() {
                checkPermissionAfterCall = true
                sLogger.debug("checkPermission onCallAdded onDenied and will check permission after call")
            }
        })
        permissionHelper.checkAndRequestPermission(NewCallAppSdkInterface.PERMISSION_TYPE_BEFORE_CALL)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun dealCall(call: Call?){
        // 开机未解锁过，增强通话功能不能使用。sp会有异常，直接catch住就可以了。
        try {
            if (call == null){
                sLogger.info("onCallAdded call is null")
                return
            }
            val callId = CallUtils.getTelecomCallId(call)
            if (callId == null){
                sLogger.info("onCallAdded callId is null")
                return
            }
            sLogger.info("onCallAdded callId:${callId}")
            if (mCallsMap[callId] != null){
                sLogger.info("onCallAdded was added")
                return
            }
            mCallsMap[callId] = call
            if (!hasInit){
                // 这里面的逻辑，在多个来电时只需要初始化一次
                hasInit = true

                //初始化底层DC管理和通话管理
                mDcManager = DCManager()
                mCallsManager = NewCallsManager()
                mCallsManager?.setInCallService(this)

                //设置小程序下载相关类
                MiniAppManager.setCallsManager(mCallsManager!!)
                MiniAppManager.setNetworkManager(mDcManager!!)
            }

            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            sLogger.info("version: ${packageInfo.versionName}")
            sLogger.info("InCallServiceImpl onCallAdded")

            val callInfo = mCallsManager?.onCallAdded(call)
            if (callInfo == null) {
                if (sLogger.isDebugActivated) {
                    sLogger.debug("InCallServiceImpl onCallAdded call info is null")
                }
                return
            }

            val telecomCallId = callInfo.telecomCallId
            val miniAppManager = MiniAppManager(callInfo)
            miniAppManager.setMiniAppStartManager(MiniAppStartManager)

            val bdcManager = BDCManager(callInfo, miniAppManager)
            mDcManager?.let {
                it.registerBDCCallback(telecomCallId, bdcManager)
                mCallsManager?.addCallStateListener(telecomCallId, it)
                it.setCurrentCallId(telecomCallId)
            }

            mCallsManager?.let {
                it.addCallStateListener(telecomCallId, miniAppManager)
                it.addCallStateListener(telecomCallId, bdcManager)
                it.addCallInfoUpdateListener(telecomCallId, bdcManager)
                it.notifyOnCallAdded(callInfo)
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    override fun onCallRemoved(call: Call?) {
        try {
            sLogger.info("InCallServiceImpl onCallRemoved")
            val callId = CallUtils.getTelecomCallId(call)
            callId?.let {
                mCallsManager?.onCallRemoved(it)
                mCallsMap.remove(it)
            }
            // 只会结束主进程中的所以Activity，是合理的
            activityManager.finishAllActivity()
            // 防止系统没有回调onUnbind
            if (mCallsMap.isEmpty()){
                release()
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        sLogger.debug("onUnbind intent:$intent")
        release()
        return super.onUnbind(intent)
    }

    private fun release(){
        scope.launch {
            // 这里不要立即执行，等其他状态回调都执行完毕再执行这里,这里的所有操作必须允许重复执行
            delay(1000)
            hasInit = false
            mDcManager?.onCallServiceUnbind(Utils.getApp())
            mDcManager = null

            mCallsManager?.onCallServiceUnBind()
            mCallsManager = null
            mCallsMap.clear()

            // 结束后授权
            if (checkPermissionAfterCall){
                checkPermissionAfterCall = false
                val permissionHelper = SDKPermissionHelper(Utils.getApp(),null)
                permissionHelper.checkAndRequestPermission(NewCallAppSdkInterface.PERMISSION_TYPE_AFTER_CALL)
            }
        }
    }
}