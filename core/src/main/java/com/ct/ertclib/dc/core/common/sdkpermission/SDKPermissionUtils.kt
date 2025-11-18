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
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.SPUtils
import com.ct.ertclib.dc.core.common.WebActivity
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.data.common.PolicyValue
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.LogUtils
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
    const val FELLOW_DIALER_KEY = "fellow_dialer_key"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    fun hasAllPermissions(context: Context):Boolean{
        return isNewCallEnable() && getNotGrantedPermissions(context).isEmpty() && Settings.canDrawOverlays(context)
    }

    // 这个权限的判断比较特殊,有特权和没有特权的判断要兼容，以保证在普通终端上运行local版本
    @SuppressLint("ObsoleteSdkInt")
    fun hasUsageStatsPermission(context: Context): Boolean {
        val state =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            true
        }
        return state || checkPermissions(context, Manifest.permission.PACKAGE_USAGE_STATS)
    }

    // 列出需要但是没有获取的权限
    fun getNotGrantedPermissions(context: Context): Array<String> {
        val list = mutableListOf<String>()
        if (!hasUsageStatsPermission(context)){
            list.add(Manifest.permission.PACKAGE_USAGE_STATS)
        }
        if (!checkPermissions(context, Manifest.permission.READ_PHONE_STATE)){
            list.add(Manifest.permission.READ_PHONE_STATE)
        }
        return list.toTypedArray()
    }

    // 保存需要立即生效
    fun setNewCallEnable(enable:Boolean){
        SPUtils.getInstance().put(ENABLE_NEW_CALL_SP_KEY, enable,true)
    }

    fun isNewCallEnable():Boolean{
        return SPUtils.getInstance().getBoolean(ENABLE_NEW_CALL_SP_KEY, false)
    }

    // 保存需要立即生效
    fun setFellowDialer(enable:Boolean){
        SPUtils.getInstance().put(FELLOW_DIALER_KEY, enable,true)
    }

    fun isFellowDialer():Boolean{
        return SPUtils.getInstance().getBoolean(FELLOW_DIALER_KEY, false)
    }

    fun isPrivacyChanged():Boolean{
        return SPUtils.getInstance().getBoolean(POLICY_CHANGE_KEY, false)
    }

    fun addPermissionDidOnce(){
        // 累加一次
        val num = SPUtils.getInstance().getInt(PERMISSION_DID_CALL_NUM_SP_KEY,0)
        SPUtils.getInstance().put(PERMISSION_DID_CALL_NUM_SP_KEY, num+1,true)
    }

    fun permissionDoneAllTimes():Boolean{
        // 最多三次
        val num = SPUtils.getInstance().getInt(PERMISSION_DID_CALL_NUM_SP_KEY,0)
        return num<3
    }

    fun setPermissionDidZero(){
        SPUtils.getInstance().put(PERMISSION_DID_CALL_NUM_SP_KEY,0,true)
    }

    fun checkPermissions(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
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
                                SPUtils.getInstance().put(POLICY_CHANGE_KEY,true,true)
                            }
                            SPUtils.getInstance().put(POLICY_VERSION_KEY,responseBody.value.version,true)
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