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

package com.ct.ertclib.dc.core.manager.call

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.newcalllib.datachannel.V1_0.IImsDataChannelCallback
import com.newcalllib.datachannel.V1_0.IImsDataChannelServiceController
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.port.dc.ImsDcServiceConnectionCallback
import com.ct.ertclib.dc.core.utils.common.CallUtils
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import com.ct.ertclib.dc.core.utils.common.JsonUtil


object DCServiceManager {

    private const val TAG = "DCServiceManager"
    private val sLogger: Logger = Logger.getLogger(TAG)

    private var mBindDcServiceResult = false
    private var mDcController: IImsDataChannelServiceController? = null
    private var mConnectionCallback: ImsDcServiceConnectionCallback? = null
    private var mDcServiceConnection: DcServiceConnection = DcServiceConnection()

    fun setDcCallback(
        imsDcCallback: IImsDataChannelCallback?,
        slotId: Int,
        telecomCallId: String
    ) {
        sLogger.info("setDcCallback telecomCallId: $telecomCallId")

        if (!mBindDcServiceResult || mDcController == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("mBindDcServiceResult: $mBindDcServiceResult, or mDcController is null")
            }
            return
        }
        try {
            mDcController!!.setImsDataChannelCallback(imsDcCallback, slotId, telecomCallId)
        } catch (e: Exception) {
            sLogger.error("setDcCallback failed", e)
        }
    }

    fun setImsDcServiceConnectionCallback(serviceConnectionCallback: ImsDcServiceConnectionCallback?) {
        mConnectionCallback = serviceConnectionCallback
    }

    fun bindDcService(context: Context) {
        sLogger.info("bindDcService")

        if (mBindDcServiceResult) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("dc service already bind")
            }
            return
        }

        var dcPackage = context.getString(R.string.ims_data_channel_service_package_name)
        var dcClass = context.getString(R.string.ims_data_channel_service_cls)
        var dcAction = context.getString(R.string.ims_data_channel_service_action)
        var mccMncList = context.getString(R.string.ims_data_channel_service_mccmnc)

        // 本地测试
        if (FlavorUtils.getChannelName() == FlavorUtils.CHANNEL_LOCAL){
            sLogger.debug("dc service local test")
            dcPackage = context.packageName

            dcClass = "com.ct.ertclib.dc.feature.testing.TestImsDataChannelService"

            dcAction = "com.newcalllib.datachannel.V1_0.ImsDataChannelService"

            mccMncList = "[ALL]"
        }

        sLogger.info("bindDcService: $dcPackage $dcClass $dcAction $mccMncList")
        val intent = Intent(dcAction)
        try {
            val mutableListOf = JsonUtil.fromJson(mccMncList, ArrayList::class.java)
            intent.putExtra("mccMncList",mutableListOf)
        } catch (e: Exception){
            e.printStackTrace()
            sLogger.info("bindDcService getMccMncList failed ${e.message}")
        }
        intent.component = ComponentName(dcPackage, dcClass)

        try {
            mBindDcServiceResult =
                context.bindService(intent, mDcServiceConnection, BIND_AUTO_CREATE)
        } catch (e: Exception) {
            sLogger.error(e.message, e)
            mBindDcServiceResult = false
        }
        if (sLogger.isDebugActivated) {
            sLogger.debug("bindDcService mBindDcServiceResult:$mBindDcServiceResult")
        }
        if (!mBindDcServiceResult) {
            sLogger.debug("绑定DC Service失败,请检查配置是否正确")
        }
    }

    fun unbindDcService(context: Context) {
        sLogger.debug("unbindDcService")
        if (mBindDcServiceResult) {
            mBindDcServiceResult = false
            context.unbindService(mDcServiceConnection)
        }
        mDcController = null
        mConnectionCallback?.onServiceDisconnected()
        mConnectionCallback = null
    }

    class DcServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            sLogger.info("bind dc onServiceConnected ")
            mDcController = IImsDataChannelServiceController.Stub.asInterface(service)
            mConnectionCallback?.onServiceConnected()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sLogger.info("bind dc onServiceDisconnected ")
        }

    }

    fun createImsDc(labels: Array<String>, description: String, slotId: Int, callId: String,
                    phoneNumber: String?): Int {
        if (mBindDcServiceResult) {
            if (mDcController == null) {
                sLogger.debug("createImsDc dc is null")
                return 1
            }
            try {
                mDcController?.createImsDataChannel(labels, description, slotId, callId, phoneNumber)
                return 0
            } catch (e: Exception) {
                if (sLogger.isDebugActivated) {
                    sLogger.error("createImsDc", e)
                }
            }
        }
        return 1
    }

    fun bindDcServiceResult():Boolean{
        return mBindDcServiceResult
    }
}