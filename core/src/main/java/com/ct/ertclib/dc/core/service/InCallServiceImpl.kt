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

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.sdkpermission.IPermissionCallback
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionHelper
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.manager.common.InCallServiceManager
import com.ct.ertclib.dc.core.utils.common.CallUtils

class InCallServiceImpl : InCallService() {

    companion object {
        private const val TAG = "InCallServiceImpl"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)
    private var checkPermissionAfterCall = false



    override fun onBind(intent: Intent?): IBinder? {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        sLogger.info("onBind SDK version: ${packageInfo.versionName},${packageInfo.versionCode}")
        InCallServiceManager.instance.onBind(this,this)
        return super.onBind(intent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCallAdded(call: Call?) {
        sLogger.info("onCallAdded call:${call}")
        val permissionHelper = SDKPermissionHelper(Utils.getApp(),object : IPermissionCallback {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onAgree() {
                if (call == null){
                    sLogger.info("onCallAdded call is null")
                    return
                }
                val callInfo = CallUtils.createCallInfo(call)
                if (callInfo == null) {
                    sLogger.info("onCallAdded call info is null")
                    return
                }
                sLogger.info("onCallAdded callInfo:${callInfo}")
                InCallServiceManager.instance.onCallAdded(callInfo,call)
            }
            override fun onDenied() {
                checkPermissionAfterCall = true
                sLogger.debug("checkPermission onCallAdded onDenied and will check permission after call")
            }
        })
        permissionHelper.checkAndRequestPermission(NewCallAppSdkInterface.PERMISSION_TYPE_BEFORE_CALL)

    }

    override fun onCallRemoved(call: Call?) {
        sLogger.info("onCallRemoved")
        val callId = CallUtils.getTelecomCallId(call)
        InCallServiceManager.instance.onCallRemoved(callId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        sLogger.debug("onUnbind intent:$intent")
        InCallServiceManager.instance.onUnbind()
        // 结束后授权
        if (checkPermissionAfterCall){
            checkPermissionAfterCall = false
            val permissionHelper = SDKPermissionHelper(Utils.getApp(),null)
            permissionHelper.checkAndRequestPermission(NewCallAppSdkInterface.PERMISSION_TYPE_AFTER_CALL)
        }
        return super.onUnbind(intent)
    }


}