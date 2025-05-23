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

package com.ct.ertclib.dc.core.usecase.main


import android.content.Context
import com.ct.ertclib.dc.core.constants.OEMECConstants
import com.ct.ertclib.dc.core.constants.OEMECConstants.MODULE_SCREEN_SHARE
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.common.LicenseManager
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_RESPONSE_CODE_SUCCESS
import com.ct.ertclib.dc.core.constants.CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SCREEN_SHARE_NOTIFY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.NOTIFY_STATUS_PARAM
import com.ct.ertclib.dc.core.constants.MiniAppConstants.REQUEST_ABILITY_FAILED
import com.ct.ertclib.dc.core.constants.MiniAppConstants.REQUEST_ABILITY_PARAMS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.REQUEST_ABILITY_SUCCESS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.START_SHARE_FAILED_OCCUPIED
import com.ct.ertclib.dc.core.constants.MiniAppConstants.START_SHARE_LICENSE_ERROR
import com.ct.ertclib.dc.core.constants.MiniAppConstants.START_SHARE_PARAMS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.START_SHARE_SUCCESS
import com.ct.ertclib.dc.core.data.event.NotifyEvent
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.manager.common.ExpandingCapacityManager
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.common.IParentToMiniNotify
import com.ct.ertclib.dc.core.port.expandcapacity.IExpandingCapacityListener
import com.ct.ertclib.dc.core.port.manager.IScreenShareManager
import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.utils.common.LogUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap


class ScreenShareUseCase(
    private val screenShareManager: IScreenShareManager,
    private val parentToMiniNotifier: IParentToMiniNotify
) : IScreenShareUseCase, KoinComponent {

    companion object {
        private const val TAG = "ScreenShareUseCase"
        private const val STATUS_CLOSE = "close"
        private const val STATUS_OPEN = "open"
    }

    private val logger = Logger.getLogger(TAG)
    private val applicationContext: Context by inject()
    private var callId = ""
    private var appId = ""

    override fun startScreenShare(appId:String, license: String,iMessageCallback: IMessageCallback?) {

        // license校验
        var result = LicenseManager.getInstance().verifyLicense(appId,
            LicenseManager.ApiCode.START_SCREEN_SHARE.apiCode,license)
        var responseCode = if (result) { START_SHARE_SUCCESS } else { START_SHARE_LICENSE_ERROR }
        if (result){
            result = screenShareManager.startShareScreen()
            responseCode = if (result) {
                setPrivacyModeEnabled(true)
                START_SHARE_SUCCESS
            } else {
                START_SHARE_FAILED_OCCUPIED
            }
        }
        val message = AppResponse(
            APP_RESPONSE_CODE_SUCCESS,
            APP_RESPONSE_MESSAGE_SUCCESS,
            mapOf(START_SHARE_PARAMS to responseCode )
        ).toJson()
        logger.info("startScreenShare, appId: $appId, responseCode: $responseCode")
        iMessageCallback?.reply(message)
    }

    override fun stopScreenShare(needNotifyToMini: Boolean) {
        logger.info("stopScreenShare, needNotifyToMini: $needNotifyToMini")
        screenShareManager.stopShareScreen()
        if (needNotifyToMini) {
            val stopScreenNotifyEvent = NotifyEvent(
                FUNCTION_SCREEN_SHARE_NOTIFY,
                mapOf(NOTIFY_STATUS_PARAM to STATUS_CLOSE)
            )
            parentToMiniNotifier.notifyEvent(callId, appId, stopScreenNotifyEvent)
        }
        this.callId = ""
        this.appId = ""
    }

    override fun requestScreenShareAbility(iMessageCallback: IMessageCallback?) {
        logger.info("requestScreenShareAbility")
        val result = screenShareManager.requestScreenShareAbility()
        val responseCode = if (result) { REQUEST_ABILITY_SUCCESS } else { REQUEST_ABILITY_FAILED }
        val message = AppResponse(
            APP_RESPONSE_CODE_SUCCESS,
            APP_RESPONSE_MESSAGE_SUCCESS,
            mapOf(REQUEST_ABILITY_PARAMS to responseCode )
        ).toJson()
        iMessageCallback?.reply(message)
    }

    override fun setPrivacyModeEnabled(isEnable: Boolean) {
        LogUtils.debug(TAG, "setPrivacyModeEnabled: isEnabled: $isEnable")
        ExpandingCapacityManager.request(
            applicationContext,
            TAG,
            TAG,
            "{\"provider\":\"${ExpandingCapacityManager.OEM}\",\"module\":\"${OEMECConstants.MODULE_SCREEN_SHARE}\",\"func\":\"${OEMECConstants.FUNCTION_SET_SCREEN_PRIVACY_MODE}\",\"data\":{\"isEnable\":$isEnable}}"
        )
    }

    override fun isInSharing(): Boolean {
        return screenShareManager.isSharing()
    }

    override fun initManager() {
        screenShareManager.initManager()
        initETEC()
    }

    override fun release() {
        screenShareManager.release()
        unRegisterETEC()
    }

    override fun setCallIdAndAppId(callId: String, appId: String) {
        this.callId = callId
        this.appId = appId
    }

    private fun initETEC() {
        LogUtils.debug(TAG, "initETEC")
        val modules = ArrayList<String>()
        modules.add(MODULE_SCREEN_SHARE)
        val providerModules = ConcurrentHashMap<String, ArrayList<String>>()
        providerModules[ExpandingCapacityManager.OEM] = modules
        ExpandingCapacityManager.registerECListener(
            TAG,
            TAG,
            providerModules,
            object : IExpandingCapacityListener {
                override fun onCallback(content: String?) {
                    LogUtils.debug(TAG, "ScreenShareUseCase onCallback content: $content")
                }
            })
    }

    private fun unRegisterETEC() {
        LogUtils.debug(TAG, "unRegisterETEC")
        ExpandingCapacityManager.unregisterECListener(applicationContext, TAG, TAG)
    }
}