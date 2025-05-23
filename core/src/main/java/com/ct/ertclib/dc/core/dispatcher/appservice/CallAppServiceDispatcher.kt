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

package com.ct.ertclib.dc.core.dispatcher.appservice

import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.MiniAppConstants
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.dispatcher.IAppServiceEventDispatcher

class CallAppServiceDispatcher : IAppServiceEventDispatcher {

    override fun dispatchEvent(telecomCallId: String, appId: String, appRequest: AppRequest, iMessageCallback: IMessageCallback?) {
        val replayMessage: String
        when (appRequest.actionName) {
            CommonConstants.ACTION_IS_PEER_SUPPORT_DC -> {
                replayMessage = AppResponse(
                    CommonConstants.APP_RESPONSE_CODE_SUCCESS,
                    CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS,
                    mapOf(
                        MiniAppConstants.IS_PEER_SUPPORT_DC_PARAMS to (MiniAppManager.getAppPackageManager(telecomCallId)
                            ?.isPeerSupportDc() == true))
                ).toJson()
                iMessageCallback?.reply(replayMessage)
            }
            CommonConstants.ACTION_HANGUP -> {
                MiniAppManager.hangUp(appRequest.map["telecomCallId"] as String)
            }
            CommonConstants.ACTION_PLAY_DTMF_TONE -> {
                MiniAppManager.playDtmfTone(appRequest.map["telecomCallId"] as String,(appRequest.map[MiniAppConstants.DIGIT] as String).first())
            }
            CommonConstants.ACTION_SET_SPEAKERPHONE -> {
                val on = appRequest.map[MiniAppConstants.SPEAKERPHONE_ON]
                MiniAppManager.setSpeakerphone(on as Boolean)
            }
            CommonConstants.ACTION_IS_SPEAKERPHONE_ON -> {
                replayMessage = AppResponse(
                    CommonConstants.APP_RESPONSE_CODE_SUCCESS,
                    CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS,
                    mapOf(
                        MiniAppConstants.IS_SPEAKERPHONE_ON to (MiniAppManager.isSpeakerphoneOn()))
                ).toJson()
                iMessageCallback?.reply(replayMessage)
            }
            CommonConstants.ACTION_SET_MUTED -> {
                val muted = appRequest.map[MiniAppConstants.MUTED]
                MiniAppManager.setMuted(muted as Boolean)
            }
            CommonConstants.ACTION_IS_MUTED -> {
                replayMessage = AppResponse(
                    CommonConstants.APP_RESPONSE_CODE_SUCCESS,
                    CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS,
                    mapOf(
                        MiniAppConstants.IS_MUTED to (MiniAppManager.isMuted()))
                ).toJson()
                iMessageCallback?.reply(replayMessage)
            }
        }
    }
}