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

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.Call.*
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.utils.common.CallUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.port.call.ICallInfoUpdateListener
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NewCallsManager() {

    companion object {
        private const val TAG = "NewCallsManager"
    }

    private var mCallInfoMap = ConcurrentHashMap<String, CallInfo>()
    private val mCallsMap = ConcurrentHashMap<String, Call>()
    private val mCallbackMap = ConcurrentHashMap<String, CallBack>()
    private val mCallStateListMap = ConcurrentHashMap<String, ArrayList<ICallStateListener>>()
    private val mCallInfoUpdateListMap = ConcurrentHashMap<String, ArrayList<ICallInfoUpdateListener>>()
    private val audioControlHelper by lazy { AudioControlHelper(Utils.getApp()) }
    private var inCallService : InCallService? = null

    @Volatile
    private var mCurrentTelecomCallId: String? = null

    private val sLogger: Logger = Logger.getLogger(TAG)


    init {
        audioControlHelper.registerAudioDeviceCallback(object : AudioControlHelper.OnAudioDeviceChangeListener {
            override fun onAudioDeviceChange() {
                handleAudioDeviceChange()
            }
        })
    }

    constructor(callInfoList: ArrayList<CallInfo>) : this() {
        callInfoList.forEach {
            mCallInfoMap[it.telecomCallId] = it
        }
        sLogger.info("constructor:$this")
    }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job1 : Job?= null

    init {
        job1 = scope.launch {
            StateFlowManager.callInfoFlow.distinctUntilChanged().collect { callState ->
                updateCallInfo(callState.callInfo.telecomCallId)
            }
        }
    }

    @SuppressLint("NewApi")
    fun addCallStateListener(telecomCallId: String, iCallStateListener: ICallStateListener) {
        mCallStateListMap.computeIfAbsent(
            telecomCallId
        ) {
            ArrayList()
        }.add(iCallStateListener)
    }

    fun removeCallStateListener(telecomCallId: String, iCallStateListener: ICallStateListener) {
        mCallStateListMap[telecomCallId]?.remove(iCallStateListener)
    }

    @SuppressLint("NewApi")
    fun addCallInfoUpdateListener(telecomCallId: String, iCallStateListener: ICallInfoUpdateListener) {
        mCallInfoUpdateListMap.computeIfAbsent(
            telecomCallId
        ) {
            ArrayList()
        }.add(iCallStateListener)
    }

    fun removeCallInfoUpdateListener(telecomCallId: String, iCallStateListener: ICallInfoUpdateListener) {
        mCallInfoUpdateListMap[telecomCallId]?.remove(iCallStateListener)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun onCallAdded(call: Call?): CallInfo? {
        call?.let {
            try {
                val callInfo = createCallInfo(call) ?: return null
                if (sLogger.isDebugActivated) {
                    sLogger.debug("onCallAdded callInfo: $callInfo")
                }
                val callBack = CallBack()
                mCallbackMap[callInfo.telecomCallId] = callBack
                call.registerCallback(callBack)
                mCallInfoMap[callInfo.telecomCallId] = callInfo
                mCallsMap[callInfo.telecomCallId] = call
                return callInfo
            } catch (e: Exception) {
                sLogger.error("onCallAdded error", e)
            }
        }
        return null
    }

    @SuppressLint("NewApi")
    inner class CallBack : Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("onStateChanged state: $state, call: $call")
            }
            val callId = CallUtils.getTelecomCallId(call)
            if (callId != null) {
                dispatchCallStateChange(callId)
            }
        }

        override fun onConnectionEvent(call: Call?, event: String?, extras: Bundle?) {
            super.onConnectionEvent(call, event, extras)
            // 对端呼叫保持
            // event: android.telecom.event.CALL_REMOTELY_HELD, extras: null
            // event: android.telephony.event.EVENT_SUPPLEMENTARY_SERVICE_NOTIFICATION, extras: Bundle[mParcelledData.dataSize=332]

            // 对方解除保持
            // event: android.telecom.event.CALL_REMOTELY_UNHELD, extras: null
            // event: android.telephony.event.EVENT_SUPPLEMENTARY_SERVICE_NOTIFICATION, extras: Bundle[mParcelledData.dataSize=324]
            sLogger.info("onConnectionEvent event: $event, extras: $extras")
            val callId = CallUtils.getTelecomCallId(call)
            if (callId != null) {
                when(event){
                    Connection.EVENT_CALL_REMOTELY_HELD ->{
                        dispatchRemoteCallStateChange(callId,STATE_HOLDING)

                    }
                    Connection.EVENT_CALL_REMOTELY_UNHELD->{
                        dispatchRemoteCallStateChange(callId,STATE_ACTIVE)
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    fun dispatchCallStateChange(callId: String) {
        val call = mCallsMap[callId]
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call?.details?.state
        } else {
            call?.state
        }
        if (state == null) {
//            sLogger.debug("dispatchCallStateChange state is null")
            return
        }
        val callInfo = getCallInfo(callId)
        if (callInfo == null) {
            sLogger.debug("dispatchCallStateChange call info is null")
            return
        }
        if (state == callInfo.state) {
//            sLogger.debug("onStateChanged call state not change")
            return
        }
        if (state == STATE_ACTIVE) {
            mCurrentTelecomCallId = callInfo.telecomCallId
        } else if (state == STATE_SELECT_PHONE_ACCOUNT || state == STATE_CONNECTING) {
            sLogger.debug("dispatchCallStateChange call state not need change")
            return
        }

        callInfo.state = state
        sLogger.debug("dispatchCallStateChange call state change to $state")

        val callStateList = mCallStateListMap.getOrDefault(callInfo.telecomCallId, null)
        if (callStateList == null) {
            sLogger.info("dispatchCallStateChange callStateList is null")
            return
        }
        callStateList.forEach {
            it.onCallStateChanged(callInfo, state)
        }
    }

    @SuppressLint("NewApi")
    fun dispatchRemoteCallStateChange(callId: String,state: Int) {
        val callInfo = getCallInfo(callId)
        if (callInfo == null) {
            sLogger.debug("dispatchCallStateChange call info is null")
            return
        }
        if (state == callInfo.state) {
//            sLogger.debug("onStateChanged call state not change")
            return
        }
        if (state == STATE_ACTIVE) {
            mCurrentTelecomCallId = callInfo.telecomCallId
        } else if (state == STATE_SELECT_PHONE_ACCOUNT || state == STATE_CONNECTING) {
            sLogger.debug("dispatchCallStateChange call state not need change")
            return
        }

        callInfo.state = state
        sLogger.debug("dispatchCallStateChange call state change to $state")

        val callStateList = mCallStateListMap.getOrDefault(callInfo.telecomCallId, null)
        if (callStateList == null) {
            sLogger.info("dispatchCallStateChange callStateList is null")
            return
        }
        callStateList.forEach {
            it.onCallStateChanged(callInfo, state)
        }
    }

    fun getCallInfo(callId: String): CallInfo? {
        return mCallInfoMap[callId]
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createCallInfo(call: Call): CallInfo? {
        if (call.details == null) {
            sLogger.info("createCallInfo call.details is null")
            return null
        }
        val handle = call.details.handle
        if (handle == null || "tel" != handle.scheme) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("createCallInfo - is not a telephone number：$handle")
            }
            return null
        }

        val subId = CallUtils.getSubId(call)
        val slotId = CallUtils.getSlotId(subId)
        if (sLogger.isDebugActivated) {
            sLogger.debug("createCallInfo subId：$subId, slotId:$slotId")
        }

        val telecomCallId = CallUtils.getTelecomCallId(call)
        val remoteNumber = CallUtils.getRemoteNumber(call)
        val isOutgoingCall = CallUtils.isOutgoingCall(call)
        val isConference = CallUtils.isConference(call)
        val isCtCall = CallUtils.isCtCall(subId)

        return CallInfo(
            slotId, telecomCallId!!, call.state, remoteNumber, null,
            call.details.videoState, isConference, isOutgoingCall, isCtCall
        )
    }

    private fun updateCallInfo(callId: String){
        // 在授权后及时利用定时任务刷新slotId和isCtCall
        val callInfo = mCallInfoMap[callId] ?: return
        if (callInfo.slotId != -1){// 不需要刷新
            return
        }
        val call = mCallsMap[callId] ?: return

        val subId = CallUtils.getSubId(call)
        val slotId = CallUtils.getSlotId(subId)
        if (subId != -1){
            callInfo.slotId = slotId
            if (sLogger.isDebugActivated) {
                sLogger.debug("CallsManager updateCallInfo subId：$subId, slotId:$slotId")
            }

            val isCtCall = CallUtils.isCtCall(subId)
            callInfo.isCtCall = isCtCall
        }
        mCallInfoUpdateListMap[callInfo.telecomCallId]?.forEach {
            it.onCallInfoUpdate(callInfo)
        }
    }

    fun notifyOnCallAdded(callInfo: CallInfo) {
        mCallStateListMap[callInfo.telecomCallId]?.forEach {
            it.onCallAdded(Utils.getApp(), callInfo)
        }
    }

    fun testNotifyCallStateChange(callId: String, state: Int) {
        sLogger.debug("testNotifyCallStateChange callId: $callId, state: $state")
        val callInfo = getCallInfo(callId)
        if (callInfo == null) {
            sLogger.debug("dispatchCallStateChange call info is null")
            return
        }
        if (state == STATE_ACTIVE) {
            mCurrentTelecomCallId = callInfo.telecomCallId
        } else if (state == STATE_SELECT_PHONE_ACCOUNT || state == STATE_CONNECTING) {
            sLogger.debug("dispatchCallStateChange call state not need change")
            return
        }

        callInfo.state = state
        sLogger.debug("dispatchCallStateChange call state change to $state")

        val callStateList = mCallStateListMap.getOrDefault(callInfo.telecomCallId, null)
        if (callStateList == null) {
            sLogger.info("dispatchCallStateChange callStateList is null")
            return
        }
        callStateList.forEach {
            it.onCallStateChanged(callInfo, state)
        }
    }

    @SuppressLint("NewApi")
    fun onCallRemoved(callId: String) {
        val call = mCallsMap[callId]
        val callBack = mCallbackMap[callId]
        call?.unregisterCallback(callBack)
        val callInfo = getCallInfo(callId)
        if (sLogger.isDebugActivated) {
            sLogger.debug("onCallRemoved callInfo: $callInfo")
        }
        if (callInfo != null) {
            if (mCurrentTelecomCallId != null && mCurrentTelecomCallId == callInfo.telecomCallId) {
                mCurrentTelecomCallId = null
            }
            callInfo.state = STATE_DISCONNECTED
            mCallStateListMap[callInfo.telecomCallId]?.forEach { stateListener ->
                stateListener.onCallRemoved(Utils.getApp(), callInfo)
            }
            mCallStateListMap.remove(callInfo.telecomCallId)
            mCallInfoUpdateListMap.remove(callInfo.telecomCallId)
            mCallInfoMap.remove(callInfo.telecomCallId)
            mCallsMap.remove(callInfo.telecomCallId)
            mCallbackMap.remove(callInfo.telecomCallId)
        }
    }

    @SuppressLint("NewApi")
    fun onCallServiceUnBind() {
        job1?.cancel()
        job1 = null
        mCallsMap.forEach {(callId,_) ->
            onCallRemoved(callId)
        }
        mCallStateListMap.clear()
        mCallInfoUpdateListMap.clear()
        mCallInfoMap.clear()
        mCallsMap.clear()
        mCallbackMap.clear()
        audioControlHelper.unregisterAudioDeviceCallback()
        inCallService = null
    }

    fun hangUp(telecomCallId: String){
        mCallsMap[telecomCallId]?.disconnect()
        sLogger.info("hangUp 1")
        if (FlavorUtils.getChannelName() == FlavorUtils.CHANNEL_LOCAL){
            scope.launch {
                mCallInfoMap[telecomCallId]?.let {
                    it.state = Call.STATE_DISCONNECTED
                    testNotifyCallStateChange(it.telecomCallId, it.state)
                }
            }
        }
    }

    fun answer(telecomCallId: String){
        mCallsMap[telecomCallId]?.answer(mCallsMap[telecomCallId]?.details?.videoState ?: VideoProfile.STATE_AUDIO_ONLY)
    }

    fun isVideoCall(telecomCallId: String):Boolean{
        val videoState = mCallsMap[telecomCallId]?.details?.videoState
        return videoState == VideoProfile.STATE_BIDIRECTIONAL || videoState == VideoProfile.STATE_TX_ENABLED || videoState == VideoProfile.STATE_RX_ENABLED
    }

    fun playDtmfTone(telecomCallId: String,digit: Char){
        sLogger.debug("playDtmfTone telecomCallId: $telecomCallId, digit: $digit")
        mCallsMap[telecomCallId]?.playDtmfTone(digit)
        scope.launch {
            delay(1000)
            mCallsMap[telecomCallId]?.stopDtmfTone()
        }
    }

    // 控制免提/扬声器（声音外放）
    fun setSpeakerphone(on: Boolean): Boolean {
        sLogger.debug("setSpeakerphone on: $on")
        val earpiece = CallAudioState.ROUTE_WIRED_OR_EARPIECE
        val speaker = CallAudioState.ROUTE_SPEAKER
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
            inCallService?.setAudioRoute(if (on) speaker else earpiece)
        } else {
            audioControlHelper.setSpeakerphone(on)
        }
        return true
    }

    fun isSpeakerphoneOn(): Boolean {
        val isSpeakerphoneOn = audioControlHelper.isSpeakerphoneOn()
        sLogger.debug("isSpeakerphoneOn: $isSpeakerphoneOn")
        return isSpeakerphoneOn
    }

    // 控制麦克风静音（对方听不到你的声音）
    fun setMuted(muted: Boolean) {
        sLogger.debug("setMuted muted: $muted")
        audioControlHelper.setMuted(muted)
    }

    fun isMuted(): Boolean{
        val isMuted = audioControlHelper.isMuted()
        sLogger.debug("isMuted: $isMuted")
        return isMuted
    }

    private fun handleAudioDeviceChange() {
        // 设备变化时自动调整路由
        mCallStateListMap.forEach { _,list ->
            list.forEach { stateListener ->
                stateListener.onAudioDeviceChange()
            }
        }
    }

    fun setInCallService(service: InCallService?) {
        inCallService = service
    }
}