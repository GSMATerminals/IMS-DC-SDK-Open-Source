/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.feature.testing.netdc.ui.activity

import LiveKitManager
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ct.ertclib.dc.feature.testing.R
import com.ct.ertclib.dc.feature.testing.databinding.ActivityDialerBinding
import com.ct.ertclib.dc.net.websocket.InterfaceHandler
import kotlinx.coroutines.*
import org.json.JSONObject
import androidx.core.graphics.toColorInt
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.sdkpermission.IPermissionCallback
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionHelper
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.manager.common.InCallServiceManager
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.core.utils.common.VibratorHelper
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.net.data.ConfigData
import com.ct.ertclib.dc.net.data.PageState
import com.ct.ertclib.dc.net.data.RecentCall
import com.ct.ertclib.dc.feature.testing.netdc.ui.adapter.RecentCallsAdapter
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Date

@RequiresApi(Build.VERSION_CODES.Q)
class DcDialerActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "DcDialerActivity"

        private const val KEY_CONFIG_DATA = "config_data"
        private const val KEY_RECENT_CALLS = "recent_calls"
        private const val MAX_RECENT_CALLS = 50
    }

    private val sLogger = Logger.getLogger(TAG)

    private lateinit var binding: ActivityDialerBinding
    private lateinit var recentCallsAdapter: RecentCallsAdapter

    private var currentCallId: String? = null
    private var callStartTime: Long = 0
    private var timerJob: Job? = null
    private var isDestroyed = false

    private var configData: ConfigData? = null

    private var isVideoCall = false
    private var isVideoEnabled = false
    private val recentCalls = mutableListOf<RecentCall>()
    private var dialogPhoneNumberTv: TextView? = null

    private var dialog: Dialog? = null

    var liveKitManager: LiveKitManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDialerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        setupCallButtons()

        checkPermissionsAndInit()

        initLiveKit()


    }

    private fun initLiveKit() {
        binding.localSurfaceViewRenderer.setZOrderOnTop(true)
        liveKitManager = LiveKitManager(
            this@DcDialerActivity,
            binding.remoteSurfaceViewRenderer,
            binding.localSurfaceViewRenderer
        )
        liveKitManager?.init(object : LiveKitManager.Listener {
            override fun onRoomConnected() {
                sLogger.info("LiveKit room connected (signaling only)")
                // 房间连接成功，等待通话激活
            }

            override fun onCallActivated() {
                sLogger.info("LiveKit call activated")
                // 通话已激活，音视频已发布和订阅
            }

            override fun onCallDeactivated() {
                sLogger.info("LiveKit call deactivated")
                // 通话已暂停，音视频已停止
            }

            override fun onLocalVideoReady() {
                sLogger.info("Local video ready")
                binding.localSurfaceViewRenderer.visibility = View.VISIBLE
            }

            override fun onRemoteVideoReady() {
                sLogger.info("Remote video ready")
                binding.remoteSurfaceViewRenderer.visibility = View.VISIBLE
            }

            override fun onRoomDisconnected() {
                sLogger.info("LiveKit room disconnected")
                binding.remoteSurfaceViewRenderer.visibility = View.GONE
                binding.localSurfaceViewRenderer.visibility = View.GONE
            }

            override fun onError(error: String) {
                sLogger.info("LiveKit error: $error")
                ToastUtils.showShortToast(this@DcDialerActivity, "Media error: $error")
            }
        })
    }
    private fun checkPermissionsAndInit() {
        val permissionHelper = SDKPermissionHelper(Utils.getApp(), object : IPermissionCallback {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onAgree() {
                val permissions = arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
                )
                XXPermissions.with(this@DcDialerActivity)
                    .permission(permissions)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                            if (allGranted) {
                                initData()
                            } else {
                                ToastUtils.showShortToast(this@DcDialerActivity, "Please grant permissions")
                            }
                        }
                        override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                            ToastUtils.showShortToast(this@DcDialerActivity, "Please grant permissions")
                        }
                    })
            }
            override fun onDenied() {
                sLogger.debug("checkPermission onCallAdded onDenied and will check permission after call")
            }
        })
        permissionHelper.checkAndRequestPermission(NewCallAppSdkInterface.PERMISSION_TYPE_AFTER_CALL)
    }

    private fun initData() {
        if (isDestroyed) return
        val configDataJson = SPUtils.getInstance().getString(KEY_CONFIG_DATA, "")
        if (configDataJson.isNotEmpty()) {
            configData = GsonUtils.fromJson(configDataJson, ConfigData::class.java)
        }
        // 校验SDK证书有效期
        val validateResult = configData?.validateSDKCertificate()
        sLogger.info("validateSDKCertificate: $validateResult")
        if (validateResult != true){
            ToastUtils.showShortToast(this@DcDialerActivity, "SDK certificate expired")
        }
        if (configData == null || validateResult != true) {
            showSettingsDialog()
        } else {
            initWebSocket()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showSettingsDialog() {
        binding.myNumber.text = ""
        binding.initLoadingView.visibility = View.GONE
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val tvPhoneNumber = dialogView.findViewById<TextView>(R.id.tvPhoneNumber)
        dialogPhoneNumberTv = tvPhoneNumber
        val btnCancel = dialogView.findViewById<View>(R.id.btn_cancel)
        val btnDone = dialogView.findViewById<View>(R.id.btn_done)
        val btnScanQRCode = dialogView.findViewById<View>(R.id.btn_scan_qrcode)

        tvPhoneNumber.text = configData?.phoneNumber ?: "暂无卡号信息"

        if (dialog == null) {
            dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        btnCancel.setOnClickListener {
            dialog?.dismiss()
            finish()
        }

        btnDone.setOnClickListener {
            try {
                if (configData == null) {
                    ToastUtils.showShortToast(this@DcDialerActivity, "Please scan QR code first")
                    return@setOnClickListener
                }
                ToastUtils.showShortToast(this@DcDialerActivity, "Register")
                val configDataJson = GsonUtils.toJson(configData)
                SPUtils.getInstance().put(KEY_CONFIG_DATA, configDataJson,true)
                initData()
            } catch (e: Exception) {
                sLogger.error("Failed to save config data", e)
            }
        }

        btnScanQRCode.setOnClickListener {
            startScan()
        }

        dialog?.show()
    }

    private fun initWebSocket() {


        val authInfo = JSONObject().apply {
            put("signature", configData?.signature ?: "")
        }
        val deviceInfo = JSONObject().apply {
            put("os", "android")
            put("clientVersion", "1.0.0")
        }

        InterfaceHandler.init(
            context = this,
            serverUrl = configData?.webSocketUrl ?: "",
            registerNum = configData?.phoneNumber ?: "",
            authInfo = authInfo,
            deviceInfo = deviceInfo,
            initCallback = object : InterfaceHandler.InitCallback {
                override fun onInitSuccess() {
                    lifecycleScope.launch {
                        sLogger.info("init success")
                        if (dialog?.isShowing == true) {
                            dialog?.dismiss()
                        }
                        binding.myNumber.text = configData?.phoneNumber ?: ""
                        binding.initLoadingView.visibility = View.GONE
                        showPage("", PageState.INIT)
                        loadRecentCallsFromCache()
                    }
                }

                override fun onInitFailed(error: String) {
                    lifecycleScope.launch {
                        ToastUtils.showShortToast(this@DcDialerActivity, error)
                        showSettingsDialog()
                    }
                }

                override fun onDisconnected() {
                    lifecycleScope.launch {
                        hangupCall()
                        InterfaceHandler.release()
                        ToastUtils.showShortToast(this@DcDialerActivity, "failed to connect")
                        showSettingsDialog()
                    }
                }
            },
            callCallback = object : InterfaceHandler.CallCallback {
                override fun onCallAck(callId: String, callerNumber: String, calleeNumber: String, requestStatus: String, reason: String,token:String,liveKitUrl:String) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (requestStatus == "accepted") {
                            currentCallId = callId
                            connectRoom(token,liveKitUrl)
                            if (isVideoCall) {
                                binding.localSurfaceViewRenderer.visibility = View.VISIBLE
                            }
                            val callInfo = CallInfo(
                                slotId = 0,
                                telecomCallId = callId,
                                myNumber = callerNumber,
                                remoteNumber = calleeNumber,
                                state = Call.STATE_DIALING,
                                videoState = 0,
                                isConference = false,
                                isOutgoingCall = true,
                                isCtCall = true
                            )
                            NewCallsManager.instance.addCallStateListener(callId, object : ICallStateListener {
                                override fun onCallAdded(context: Context, callInfo: CallInfo) {}
                                override fun onCallRemoved(context: Context, callInfo: CallInfo) {}
                                override fun onCallStateChanged(callInfo: CallInfo, state: Int) {
                                    lifecycleScope.launch() {
                                        if (state == Call.STATE_DISCONNECTING) {
                                            hangupCall()
                                        }
                                    }
                                }
                                override fun onAudioDeviceChange() {}
                            })
                            InCallServiceManager.instance.onBind(this@DcDialerActivity, null)
                            InCallServiceManager.instance.onCallAdded(callInfo, null)
                            showPage(calleeNumber, PageState.OUT_CALLING)
                        } else {
                            ToastUtils.showShortToast(this@DcDialerActivity, "Call failed $reason")
                            showPage("", PageState.INIT)
                        }
                    }
                }

                override fun onRinging(callId: String, callerNumber: String,callFun:String,token: String,liveKitUrl: String) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        currentCallId = callId

                        isVideoCall = callFun == "video"
                        if (isVideoCall) {
                            binding.localSurfaceViewRenderer.visibility = View.VISIBLE
                        }

                        val callInfo = CallInfo(
                            slotId = 0,
                            telecomCallId = callId,
                            myNumber = configData?.phoneNumber ?: "",
                            remoteNumber = callerNumber,
                            state = Call.STATE_RINGING,
                            videoState = 0,
                            isConference = false,
                            isOutgoingCall = false,
                            isCtCall = true
                        )
                        NewCallsManager.instance.addCallStateListener(callId, object : ICallStateListener {
                            override fun onCallAdded(context: Context, callInfo: CallInfo) {}
                            override fun onCallRemoved(context: Context, callInfo: CallInfo) {}
                            override fun onCallStateChanged(callInfo: CallInfo, state: Int) {
                                lifecycleScope.launch() {
                                    if (state == Call.STATE_DISCONNECTING) {
                                        hangupCall()
                                    } else if (state == Call.STATE_CONNECTING) {
                                        answerCall()
                                    }
                                }
                            }
                            override fun onAudioDeviceChange() {}
                        })
                        InCallServiceManager.instance.onBind(this@DcDialerActivity, null)
                        InCallServiceManager.instance.onCallAdded(callInfo, null)
                        showPage(callerNumber, PageState.RINGING)
                        connectRoom(token,liveKitUrl)
                    }
                }

                override fun onCallEstablished(callId: String, callerNumber: String) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        NewCallsManager.instance.testNotifyCallStateChange(callId, Call.STATE_ACTIVE)
                        NewCallsManager.instance.getCallInfo(callId)?.let {
                            it.remoteNumber?.let { phoneNumber -> showPage(phoneNumber, PageState.ACTIVE) }
                        }
                        if (isVideoCall) {
                            binding.remoteSurfaceViewRenderer.visibility = View.VISIBLE
                            binding.localSurfaceViewRenderer.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onCallEnd(callId: String, reason: String, callerNumber: String, calleeNumber: String) {
                    releaseCall(NewCallsManager.instance.getCallInfo(callId))
                }
            }
        )
    }

    /**
     * 初始化 LiveKit
     */
    private fun connectRoom(token: String, liveKitUrl: String) {
        if (currentCallId == null) return
        liveKitManager?.connectRoom(liveKitUrl, token, isVideoCall)
    }

    private fun activateCall(){
        liveKitManager?.activateCall()
    }

    private fun disconnectRoom() {
        liveKitManager?.disconnectRoom()
    }

    private fun releaseLiveKit() {
        liveKitManager?.release()
    }

    private fun releaseCall(callInfo: CallInfo?) {
        if (callInfo == null || callInfo.state == Call.STATE_DISCONNECTED) {
            return
        }
        lifecycleScope.launch {
            stopCallTimer()
            lifecycleScope.launch(Dispatchers.IO) {
                callInfo.remoteNumber?.let { saveRecentCallToCache(it, it, callInfo.isOutgoingCall, isVideoCall) }

                disconnectRoom()
                isVideoCall = false
                isVideoEnabled = false

                withContext(Dispatchers.Main) {
                    NewCallsManager.instance.testNotifyCallStateChange(callInfo.telecomCallId, Call.STATE_DISCONNECTED)
                    InCallServiceManager.instance.onCallRemoved(callInfo.telecomCallId)
                    showPage("", PageState.INIT)
                }
            }
        }
    }

    private fun loadRecentCallsFromCache() {
        if (isDestroyed) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonString = SPUtils.getInstance().getString(KEY_RECENT_CALLS, "")
                if (jsonString.isNotEmpty()) {
                    val list = GsonUtils.fromJson(jsonString, Array<RecentCall>::class.java)
                    withContext(Dispatchers.Main) {
                        recentCalls.clear()
                        recentCalls.addAll(list)
                        recentCallsAdapter.updateCalls(recentCalls)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (!isDestroyed) {
                            binding.recyclerViewRecentCalls.visibility = View.GONE
                            binding.dialpadContainer.visibility = View.VISIBLE
                            binding.btnDialpad.setTextColor("#008000".toColorInt())
                            binding.btnRecentCalls.setTextColor("#8E8E93".toColorInt())
                        }
                    }
                }
            } catch (e: Exception) {
                sLogger.info("加载最近通话缓存失败: ${e.message}")
            }
        }
    }

    private fun saveRecentCallToCache(phoneNumber: String, name: String, isOutgoing: Boolean, isVideoCall: Boolean = false) {
        if (isDestroyed) return
        try {
            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val currentTime = dateFormat.format(Date())

            val newCall = RecentCall(name, phoneNumber, currentTime, isOutgoing, isVideoCall)

            recentCalls.add(0, newCall)
            if (recentCalls.size > MAX_RECENT_CALLS) {
                recentCalls.removeAt(recentCalls.size - 1)
            }

            val jsonString = GsonUtils.toJson(recentCalls)
            SPUtils.getInstance().put(KEY_RECENT_CALLS, jsonString)

            sLogger.info("保存最近通话缓存成功 ${recentCalls.size}, $jsonString")
            lifecycleScope.launch(Dispatchers.Main) {
                recentCallsAdapter.updateCalls(recentCalls)
            }
        } catch (e: Exception) {
            sLogger.info("保存最近通话缓存失败: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        recentCallsAdapter = RecentCallsAdapter { phoneNumber, type ->
            if (isDestroyed) return@RecentCallsAdapter
            initiateCall(phoneNumber, type)
        }
        binding.recyclerViewRecentCalls.apply {
            layoutManager = LinearLayoutManager(this@DcDialerActivity)
            adapter = recentCallsAdapter
        }
    }

    private fun setupListeners() {
        binding.backIcon.setOnClickListener {
            finish()
        }

        binding.btnUnregister.setOnClickListener {
            InterfaceHandler.release()
            showSettingsDialog()
            ToastUtils.showShortToast(this@DcDialerActivity, "Unregister")
        }

        binding.btnRecentCalls.setOnClickListener {
            if (!isDestroyed) {
                binding.recyclerViewRecentCalls.visibility = View.VISIBLE
                binding.dialpadContainer.visibility = View.GONE
                binding.btnRecentCalls.setTextColor("#008000".toColorInt())
                binding.btnDialpad.setTextColor("#8E8E93".toColorInt())
            }
        }

        binding.btnDialpad.setOnClickListener {
            if (!isDestroyed) {
                binding.recyclerViewRecentCalls.visibility = View.GONE
                binding.dialpadContainer.visibility = View.VISIBLE
                binding.btnDialpad.setTextColor("#008000".toColorInt())
                binding.btnRecentCalls.setTextColor("#8E8E93".toColorInt())
            }
        }
    }

    private fun setupCallButtons() {
        val dialButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9, binding.btnStar, binding.btnHash
        )

        dialButtons.forEach { button ->
            button.setOnClickListener {
                if (!isDestroyed) {
                    val digit = button.text.toString()
                    appendDigit(digit)
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            if (!isDestroyed) {
                deleteLastDigit()
            }
        }

        binding.btnDelete.setOnLongClickListener {
            if (!isDestroyed) {
                binding.tvPhoneNumber.text = ""
                true
            } else {
                false
            }
        }

        binding.btnCall.setOnClickListener {
            if (!isDestroyed) {
                val phoneNumber = binding.tvPhoneNumber.text.toString()
                if (phoneNumber.isNotEmpty()) {
                    initiateCall(phoneNumber, "voice")
                }
            }
        }

        binding.btnVideoCall.setOnClickListener {
            if (!isDestroyed) {
                val phoneNumber = binding.tvPhoneNumber.text.toString()
                if (phoneNumber.isNotEmpty()) {
                    initiateCall(phoneNumber, "video")
                }
            }
        }

        binding.btnAnswer.setOnClickListener {
            if (!isDestroyed) {
                answerCall()
            }
        }

        binding.btnHangup.setOnClickListener {
            if (!isDestroyed) {
                hangupCall()
            }
        }

        binding.btnToggleVideo.setOnClickListener {
            if (!isDestroyed) {
                toggleVideo()
            }
        }
    }

    private fun initiateCall(phoneNumber: String, callType: String) {
        isVideoCall = callType == "video"
        lifecycleScope.launch {
            if (!InterfaceHandler.isConnected()) {
                ToastUtils.showShortToast(this@DcDialerActivity, "Not connected to the server.")
                return@launch
            }
            if (configData?.phoneNumber == phoneNumber) {
                ToastUtils.showShortToast(this@DcDialerActivity, "You cannot call your own number.")
                return@launch
            }
            try {
                configData?.phoneNumber?.let {
                    InterfaceHandler.makeCall(it, phoneNumber, callType, "app")
                }?.let {
                    if (!it) {
                        ToastUtils.showShortToast(this@DcDialerActivity, "Call initiation failed.")
                        showPage("", PageState.INIT)
                    }
                }
            } catch (e: Exception) {
                ToastUtils.showShortToast(this@DcDialerActivity, "Call anomaly: ${e.message}")
                showPage("", PageState.INIT)
            }
        }
    }

    private fun answerCall() {
        if (isDestroyed || currentCallId == null) return

        lifecycleScope.launch {
            try {
                val success = InterfaceHandler.answerCall(currentCallId!!)
                if (!success) {
                    ToastUtils.showShortToast(this@DcDialerActivity, "Answer failed.")
                }
            } catch (e: Exception) {
                ToastUtils.showShortToast(this@DcDialerActivity, "Answer anomaly: ${e.message}")
            }
        }
    }

    private fun toggleVideo() {
        if (!isVideoCall) return
        isVideoEnabled = !isVideoEnabled

    }

    @SuppressLint("SetTextI18n")
    private fun showPage(phoneNumber: String, state: PageState) {
        if (isDestroyed) return

        when (state) {
            PageState.INIT -> {
                binding.mainContainer.visibility = View.VISIBLE
                binding.callingView.visibility = View.GONE
                binding.remoteSurfaceViewRenderer.visibility = View.INVISIBLE
                binding.localSurfaceViewRenderer.visibility = View.INVISIBLE
                binding.btnToggleVideo.setImageResource(R.drawable.icon_video_closed)
                currentCallId = null
                isVideoCall = false
                isVideoEnabled = false
                binding.tvCallDuration.text = ""
                stopCallTimer()
                VibratorHelper.stopVibration()
            }
            PageState.OUT_CALLING -> {
                binding.mainContainer.visibility = View.GONE
                binding.callingView.visibility = View.VISIBLE
                binding.btnAnswerContainer.visibility = View.GONE
                binding.btnHangupContainer.visibility = View.VISIBLE
                if (isVideoCall) {
                    binding.btnToggleVideo.setImageResource(R.drawable.icon_video)
                } else {
                    binding.btnToggleVideo.setImageResource(R.drawable.icon_video_closed)
                }
                binding.tvCallPhoneNumber.text = phoneNumber
                binding.tvCallName.text = getNameFromPhoneNumber(phoneNumber)
                binding.tvCallStatus.text = getString(R.string.calling)
                binding.tvCallDuration.text = ""
            }
            PageState.ACTIVE -> {
                binding.mainContainer.visibility = View.GONE
                binding.callingView.visibility = View.VISIBLE
                binding.btnAnswerContainer.visibility = View.GONE
                binding.btnHangupContainer.visibility = View.VISIBLE
                binding.tvCallPhoneNumber.text = phoneNumber
                binding.tvCallName.text = getNameFromPhoneNumber(phoneNumber)
                binding.tvCallStatus.text = getString(R.string.active)
                binding.tvCallDuration.text = "00:00"
                startCallTimer()
                VibratorHelper.stopVibration()
                activateCall()
            }
            PageState.RINGING -> {
                binding.mainContainer.visibility = View.GONE
                binding.callingView.visibility = View.VISIBLE
                binding.btnAnswerContainer.visibility = View.VISIBLE
                binding.btnHangupContainer.visibility = View.VISIBLE
                if (isVideoCall) {
                    binding.btnToggleVideo.setImageResource(R.drawable.icon_video)
                } else {
                    binding.btnToggleVideo.setImageResource(R.drawable.icon_video_closed)
                }
                binding.tvCallPhoneNumber.text = phoneNumber
                binding.tvCallName.text = getNameFromPhoneNumber(phoneNumber)
                binding.tvCallStatus.text = getString(R.string.ringing)
                binding.tvCallDuration.text = ""
                VibratorHelper.startVibration(this@DcDialerActivity, longArrayOf(1000, 500))
            }
        }
    }

    private fun startCallTimer() {
        stopCallTimer()
        timerJob = lifecycleScope.launch {
            callStartTime = System.currentTimeMillis()
            while (isActive && !isDestroyed) {
                try {
                    val duration = System.currentTimeMillis() - callStartTime
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                    withContext(Dispatchers.Main) {
                        if (!isDestroyed) {
                            binding.tvCallDuration.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                        }
                    }
                    delay(1000)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun stopCallTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun hangupCall() {
        if (isDestroyed) return
        try {
            if (currentCallId != null) {
                InterfaceHandler.hangupCall(currentCallId!!)
                releaseCall(NewCallsManager.instance.getCallInfo(currentCallId!!))
            } else {
                showPage("", PageState.INIT)
            }
        } catch (_: Exception) {
            showPage("", PageState.INIT)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun appendDigit(digit: String) {
        if (isDestroyed) return
        val current = binding.tvPhoneNumber.text.toString()
        binding.tvPhoneNumber.text = current + digit
    }

    private fun deleteLastDigit() {
        if (isDestroyed) return
        val current = binding.tvPhoneNumber.text.toString()
        if (current.isNotEmpty()) {
            binding.tvPhoneNumber.text = current.substring(0, current.length - 1)
        }
    }

    private fun getNameFromPhoneNumber(phoneNumber: String): String {
        return recentCalls.find { it.phoneNumber == phoneNumber }?.name ?: ""
    }

    override fun onDestroy() {
        isDestroyed = true
        timerJob?.cancel()
        try {
            hangupCall()
            releaseLiveKit()
            InterfaceHandler.release()
        } catch (e: Exception) {
            sLogger.info("Failed to release resources: ${e.message}")
        }
        super.onDestroy()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    // 定义 launcher 和回调
    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult? ->
        try {
            if (result == null || result.contents == null) {
                ToastUtils.showShortToast(this, "扫描取消或失败")
            } else {
                val scannedContent = result.contents
                sLogger.info("Scan result: $scannedContent")
                configData = GsonUtils.fromJson(scannedContent, ConfigData::class.java)
                dialogPhoneNumberTv?.text = configData?.phoneNumber ?: "暂无卡号信息"
            }
        } catch (e: Exception) {
            sLogger.info("Scan failed: ${e.message}")
        }
    }

    // 启动扫描的方法
    private fun startScan() {
        scanLauncher.launch(ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE) // 可指定多种格式
            setPrompt("将二维码放入框内扫描")              // 提示文字
            setCameraId(0)                                 // 后置摄像头
            setBeepEnabled(false)                           // 成功时发出"哔"声
            setOrientationLocked(true)                    // 是否锁定方向，设为false允许跟随系统
        })
    }
}