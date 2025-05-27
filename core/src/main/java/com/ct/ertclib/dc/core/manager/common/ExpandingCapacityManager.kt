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

package com.ct.ertclib.dc.core.manager.common

import android.content.Context
import com.ct.ctec.CtEC
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.data.common.ECBaseData
import com.ct.ertclib.dc.core.port.expandcapacity.IExpandingCapacityListener
import com.ct.ertclib.dc.base.port.ec.IEC
import com.ct.ertclib.dc.base.port.ec.IECCallback
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.oemec.OemEC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap


object ExpandingCapacityManager {

    private const val TAG = "ExpandingCapacityManager"
    private val sLogger: Logger = Logger.getLogger(TAG)

    const val CT = "CT"
    const val CM = "CM"
    const val CU = "CU"
    const val OEM = "OEM"

    private val mECListenerMap = ConcurrentHashMap<String, IExpandingCapacityListener>()
    private val mECModulesMap = ConcurrentHashMap<String, ConcurrentHashMap<String, ArrayList<String>>>()//防止能力提供者胡乱回调
    private val scope = CoroutineScope(Dispatchers.Default)
    private val mProviderMap = ConcurrentHashMap<String, IEC>()

    // 哪通电话的哪个小程序要使用哪个能力提供者的哪些能力
    fun registerECListener(
        callId:String,
        appId:String,
        providerModules:ConcurrentHashMap<String, ArrayList<String>>,
        ecListener: IExpandingCapacityListener
    ) {
        sLogger.info("registerECCallback callId: $callId, appId: $appId")
        mECListenerMap[callId+appId] = ecListener
        mECModulesMap[callId+appId] = providerModules
    }

    fun unregisterECListener(
        context: Context,
        callId:String,
        appId:String
    ) {
        sLogger.info("unregisterECCallback callId: $callId,appId: $appId")
        mProviderMap.forEach { (_, instance) ->
            instance.releaseMiniApp(context, callId, appId)
        }
        mECListenerMap.remove(callId+appId)
        mECModulesMap.remove(callId+appId)
    }

    fun init(context: Context) {
        sLogger.info("init")
        // 添加OEM拓展能力
        mProviderMap[OEM] = OemEC.instance
        // 添加电信拓展能力
        mProviderMap[CT] = CtEC.instance
        // todo 添加其他

        // 初始化
        mProviderMap.forEach { (provider, instance) ->
            instance.init(context, ECCallback(provider))
        }
    }

    fun release(context: Context) {
        sLogger.debug("release")
        // 释放拓展能力
        mProviderMap.forEach { (_, instance) ->
            instance.releaseAll(context)
        }
        mProviderMap.clear()
        mECListenerMap.clear()
        mECModulesMap.clear()

    }

    fun request(context: Context,callId:String, appId:String, content: String?): Int {
        try {
            sLogger.info("request callId: $callId, appId: $appId, content: $content")
            val requestData = content?.let { JsonUtil.fromJson(it, ECBaseData::class.java) }
            if (requestData!=null){
                // 给能力提供方的参数要去掉content中的provider字段
                if (mECModulesMap[callId+appId]?.get(requestData.provider)?.contains(requestData.module)  == true){
                    val contentToSend = JsonUtil.removeJsonFieldWithGson(content, "provider")
                    return mProviderMap[requestData.provider]?.request(context,callId,appId,contentToSend)!!
                }
            }
        } catch (e:Exception){
            e.printStackTrace()
        }
        return -1
    }



    // 其他能力提供者能关联callId和appId
    class ECCallback(val provider: String) : IECCallback {
        override fun onCallback(callId:String?, appId:String?,content: String?) {
            sLogger.debug("ECCallback onCallback content: $content")
            dealCallback(callId,appId,this.provider,content)
        }
    }

    private fun dealCallback(callId:String?,appId:String?,provider:String,content: String?){
        try {
            val callbackData = content?.let { JsonUtil.fromJson(it, ECBaseData::class.java) }
            if (callbackData!=null){
                // 能力提供方的返回数据要填充content中的provider字段值
                callbackData.provider = provider
                val contentToBack = JsonUtil.toJson(callbackData)
                if (callId == null || appId == null){
                    // 不区分小程序，按module进行广播
                    mECListenerMap.forEach{ (key, listener) ->
                        if (mECModulesMap[key]?.get(provider)?.contains(callbackData.module)  == true){
                            scope.launch {
                                sLogger.debug("OEMECCallback onCallback key :$key contentToBack: $contentToBack")
                                listener.onCallback(content)
                            }
                        }
                    }
                } else {
                    // 指定小程序
                    val key = callId+appId
                    if (mECModulesMap[key]?.get(provider)?.contains(callbackData.module)  == true){
                        scope.launch {
                            sLogger.debug("ECCallback onCallback key :$key contentToBack: $contentToBack")
                            mECListenerMap[key]?.onCallback(contentToBack)
                        }
                    }
                }
            }
        } catch (e:Exception){
            e.printStackTrace()
        }
    }
}