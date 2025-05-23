/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.common.sdkpermission

import android.content.Context
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.PkgUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SDKPermissionHelper(
    private val context: Context,
    private val callback: IPermissionCallback?
) {

    companion object {
        private const val TAG = "SDKPermissionHelper"
    }

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private val sLogger: Logger = Logger.getLogger(TAG)

    fun checkAndRequestPermission(type: Int) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("checkAndRequestPermission type $type")
        }
        if (SDKPermissionUtils.hasAllPermissions(context)) {
            callback?.onAgree()
            // 创建桌面图标
            PkgUtils.checkAndCreatePinnedShortcuts(context)
        } else {
            when(type){
                NewCallAppSdkInterface.PERMISSION_TYPE_BEFORE_CALL ->{
                    if (!SDKPermissionUtils.permissionDoneAllTimes()){
                        if (sLogger.isDebugActivated) {
                            sLogger.debug("checkAndRequestPermission already done three times")
                        }
                        return
                    }
                    // 增加一次授权流程记录
                    SDKPermissionUtils.addPermissionDidOnce()
                }
                NewCallAppSdkInterface.PERMISSION_TYPE_AFTER_CALL ->{

                }

            }
            scope.launch(Dispatchers.Main) {
                if (FlavorUtils.getChannelName() != FlavorUtils.CHANNEL_LOCAL && type == NewCallAppSdkInterface.PERMISSION_TYPE_BEFORE_CALL ){
                    delay(2000)  // 协程延迟2000毫秒（2秒）
                }
                SDKPermissionActivity.startActivity(context,type)

                StateFlowManager.permissionAgreeFlow.distinctUntilChanged().collect { isAgree ->
                    LogUtils.debug(TAG, "collect permissionAgreeFlow : $isAgree")
                    if (isAgree) {
                        callback?.onAgree()
                        SDKPermissionUtils.setPermissionDidZero()
                        // 创建桌面图标
                        PkgUtils.checkAndCreatePinnedShortcuts(context)
                    } else {
                        callback?.onDenied()
                    }
                }
                scope.cancel()
            }
        }
    }
}

interface IPermissionCallback {
    fun onAgree()
    fun onDenied()
}