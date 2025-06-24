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

package com.ct.oemec

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.ct.ertclib.dc.base.port.ec.IEC
import com.ct.ertclib.dc.base.port.ec.IECCallback
import com.newcalllib.expandingCapacity.IExpandingCapacity
import com.newcalllib.expandingCapacity.IExpandingCapacityCallback
import com.ct.oemec.utils.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OemEC: IEC {
    companion object {
        val instance: OemEC by lazy { OemEC() }
    }
    private val TAG = "OemEC"
    private val sLogger = Logger.getLogger(TAG)
    private var callback: IECCallback? = null
    private var mOEMBindECServiceResult = false
    private var mOEMExpandingCapacity: IExpandingCapacity? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var mOEMECServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            sLogger.info("bind oemEC onServiceConnected ")
            mOEMExpandingCapacity = IExpandingCapacity.Stub.asInterface(service)

            if (!mOEMBindECServiceResult || mOEMExpandingCapacity == null) {
                if (sLogger.isDebugActivated) {
                    sLogger.debug("mOEMBindECServiceResult: $mOEMBindECServiceResult, or mExpandingCapacity is null")
                }
                return
            }
            try {
                val oemECCallback = object : IExpandingCapacityCallback.Stub() {
                    override fun onCallback(content: String?) {
                        sLogger.debug("OEMECCallback onCallback content: $content")
                        callback?.onCallback(null,null,content)
                    }
                }
                mOEMExpandingCapacity!!.setCallback(oemECCallback)
            } catch (e: Exception) {
                sLogger.error("setCallback failed", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sLogger.info("bind ec onServiceDisconnected ")
        }

    }

    override fun init(context: Context, callback: IECCallback?) {
        this.callback = callback
        // bind OEM拓展能力服务
        if (!mOEMBindECServiceResult) {
            var ecPackage = context.getString(R.string.expand_capacity_service_package_name)
            var ecClass = context.getString(R.string.expand_capacity_service_cls)
            var ecAction = context.getString(R.string.expand_capacity_service_action)

            // 使用TestECService进行测试
            if (ecPackage.isEmpty()){
                ecPackage = context.packageName
            }
            if (ecClass.isEmpty()){
                ecClass = "com.ct.oemec.test.TestECService"
            }
            if (ecAction.isEmpty()){
                ecAction = "com.newcalllib.datachannel.V1_0.ECService"
            }

            sLogger.info("bindECService: $ecPackage $ecClass $ecAction")

            val intent = Intent(ecAction)
            intent.component = ComponentName(ecPackage, ecClass)

            try {
                mOEMBindECServiceResult =
                    context.bindService(intent, mOEMECServiceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                sLogger.error(e.message, e)
                mOEMBindECServiceResult = false
            }
            if (sLogger.isDebugActivated) {
                sLogger.debug("bindECService mBindECServiceResult:$mOEMBindECServiceResult")
            }
            if (!mOEMBindECServiceResult) {
                sLogger.debug("绑定EC Service失败,请检查配置是否正确")
            }
        }
    }

    override fun request(context: Context, callId: String, appId: String, content: String):Int {
        try {
            sLogger.info("request content: $content")
            if (mOEMBindECServiceResult) {
                if (mOEMExpandingCapacity == null) {
                    sLogger.debug("request mOEMExpandingCapacity is null")
                    return -1
                }
                scope.launch {
                    try {
                        mOEMExpandingCapacity?.request(content)
                        return@launch
                    } catch (e: Exception) {
                        if (sLogger.isDebugActivated) {
                            sLogger.error("request", e)
                        }
                    }
                }
            }
            return 0
        }catch (e:Exception){
            e.printStackTrace()
        }
        return -1
    }

    override fun releaseMiniApp(context: Context, callId: String, miniAppId: String) {
    }

    override fun releaseAll(context: Context) {
        // unbind OEM拓展能力服务
        if (mOEMBindECServiceResult) {
            mOEMBindECServiceResult = false
            context.unbindService(mOEMECServiceConnection)
        }
        mOEMExpandingCapacity = null
    }
}