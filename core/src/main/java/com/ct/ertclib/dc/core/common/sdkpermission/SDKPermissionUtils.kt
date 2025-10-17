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

package com.ct.ertclib.dc.core.common.sdkpermission

import android.Manifest
import android.content.Context
import android.provider.Settings
import com.blankj.utilcode.util.SPUtils
import com.ct.ertclib.dc.core.common.WebActivity
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.data.common.PolicyValue
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException

object SDKPermissionUtils {
    private const val TAG = "SDKPermissionUtils"

    const val PERMISSION_TYPE_KEY = "permission_type_key"
    const val PERMISSION_DID_CALL_NUM_SP_KEY = "permission_did_call_num_sp_key"
    const val POLICY_VERSION_KEY = "policy_version_key"
    const val ENABLE_NEW_CALL_SP_KEY = "enableNewCall"
    const val POLICY_CHANGE_KEY = "policy_change_key"
    val PERMISSIONS = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.PACKAGE_USAGE_STATS,
    )
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    fun hasAllPermissions(context: Context):Boolean{
        return isNewCallEnable() && XXPermissions.isGranted(context, PERMISSIONS) && Settings.canDrawOverlays(context)
    }

    fun hasPhonePermissions(context: Context):Boolean{
        return XXPermissions.isGranted(context, PERMISSIONS) && Settings.canDrawOverlays(context)
    }

    fun setNewCallEnable(enable:Boolean){
        SPUtils.getInstance().put(ENABLE_NEW_CALL_SP_KEY, enable)
    }

    fun isNewCallEnable():Boolean{
        return SPUtils.getInstance().getBoolean(ENABLE_NEW_CALL_SP_KEY, false)
    }

    fun isPrivacyChanged():Boolean{
        return SPUtils.getInstance().getBoolean(POLICY_CHANGE_KEY, false)
    }

    fun addPermissionDidOnce(){
        // 累加一次
        val num = SPUtils.getInstance().getInt(PERMISSION_DID_CALL_NUM_SP_KEY,0)
        SPUtils.getInstance().put(PERMISSION_DID_CALL_NUM_SP_KEY, num+1)
    }

    fun permissionDoneAllTimes():Boolean{
        // 最多三次
        val num = SPUtils.getInstance().getInt(PERMISSION_DID_CALL_NUM_SP_KEY,0)
        return num<3
    }

    fun setPermissionDidZero(){
        SPUtils.getInstance().put(PERMISSION_DID_CALL_NUM_SP_KEY,0)
    }

    fun checkPermissions(context: Context, vararg permissions: String): Boolean {
        return XXPermissions.isGranted(context, permissions)
    }

    // 更新隐私条款版本号，如果和本地不一致，就取消之前的授权，以触发重新弹窗；并缓存新的版本号
    fun updatePrivacyVersion() {
        scope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(CommonConstants.SDK_PRIVACY_VERSION_URL)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    LogUtils.debug(TAG,"updatePrivacyVersion onFailure:${e.toString()}")
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        LogUtils.debug(TAG,"updatePrivacyVersion onResponse:${response}")
                        if (response.isSuccessful) {
                            val responseBodyStr = response.body?.string().toString()
                            LogUtils.debug(TAG,"updatePrivacyVersion responseBodyStr:${responseBodyStr}")
                            var responseBody = JsonUtil.fromJson(responseBodyStr, PolicyValue::class.java)
                            if (responseBody == null){
                                return
                            }
                            val oldVersion = SPUtils.getInstance().getString(POLICY_VERSION_KEY,"")
                            // 一定是在有变化的时候才重置，防止第一次授权后，第二次使用时又弹窗
                            if (!oldVersion.isNullOrEmpty() && responseBody.value.version != oldVersion){
                                setNewCallEnable(false)
                                SPUtils.getInstance().put(POLICY_CHANGE_KEY,true)
                            }
                            SPUtils.getInstance().put(POLICY_VERSION_KEY,responseBody.value.version)
                        }
                    } catch (e: Exception){
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    fun startPrivacyActivity(context: Context){
        WebActivity.startActivity(context, CommonConstants.SDK_PRIVACY_URL,"隐私政策",null)

    }

    fun startUserServiceActivity(context: Context){
        WebActivity.startActivity(context,CommonConstants.SDK_USER_SERVICE_URL,"用户协议",null)
    }
}