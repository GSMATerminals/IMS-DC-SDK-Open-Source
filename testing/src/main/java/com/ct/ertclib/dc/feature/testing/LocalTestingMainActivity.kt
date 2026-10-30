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

package com.ct.ertclib.dc.feature.testing

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.Call.STATE_RINGING
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.sdkpermission.IPermissionCallback
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionHelper
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.manager.common.InCallServiceManager
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.feature.testing.databinding.ActivityLocalTestingMainBinding
import com.ct.ertclib.dc.feature.testing.socket.DCSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import kotlin.system.exitProcess

class LocalTestingMainActivity : AppCompatActivity(), KoinComponent {
    companion object {
        private const val TAG = "LocalTestingMainActivity"
        private const val CALL_ID = "TC@1000000"
    }
    private val sLogger = Logger.getLogger(TAG)
    private lateinit var binding: ActivityLocalTestingMainBinding
    private lateinit var viewModel: TestingViewModel
    private var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalTestingMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = Color.TRANSPARENT

        viewModel = ViewModelProvider(this)[TestingViewModel::class.java]
        // 设置数字键盘监听
        setupDialpadListeners()
        DCSocketManager.registerCallObserver { value ->
            when(value){
                "added" -> {
                    scope.launch(Dispatchers.Main){
                        checkPermission()
                    }
                }
                "hangup" -> {
                    scope.launch(Dispatchers.Main){
                        dealHangup()
                    }
                }
                "active" -> {
                    scope.launch(Dispatchers.Main){
                        dealActiveCall()
                    }
                }
            }
        }

        binding.backIcon.setOnClickListener {
            finish()
        }
        binding.btnSetting.setOnClickListener {
            val intent = Intent(this, LocalTestMiniAppWarehouseActivity::class.java)
            startActivity(intent)
        }

        binding.btnCall.setOnClickListener {
            DCSocketManager.notifyCallAdded()
            checkPermission()
        }

        binding.btnVideoCall.setOnClickListener {
            DCSocketManager.notifyCallAdded()
            checkPermission()
        }

        // 接听/挂断
        binding.btnAnswer.setOnClickListener {
            DCSocketManager.notifyCallActive()
            dealActiveCall()
        }
        binding.btnHangup.setOnClickListener {
            DCSocketManager.notifyHangUp()
            dealHangup()
        }
    }

    /**
     * 设置拨号键盘监听
     */
    private fun setupDialpadListeners() {
        val dialButtons = listOf(
            binding.btn0 to "0",
            binding.btn1 to "1",
            binding.btn2 to "2",
            binding.btn3 to "3",
            binding.btn4 to "4",
            binding.btn5 to "5",
            binding.btn6 to "6",
            binding.btn7 to "7",
            binding.btn8 to "8",
            binding.btn9 to "9",
            binding.btnStar to "*",
            binding.btnHash to "#"
        )

        dialButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                appendDigit(digit)
            }
        }

        // 删除按钮
        binding.btnDelete.setOnClickListener {
            deleteLastDigit()
        }

        // 长按删除全部
        binding.btnDelete.setOnLongClickListener {
            binding.tvPhoneNumber.text = ""
            true
        }
    }

    /**
     * 添加数字到号码显示
     */
    private fun appendDigit(digit: String) {
        val current = binding.tvPhoneNumber.text.toString()
        if (current.length < 20) {  // 限制长度
            binding.tvPhoneNumber.text = current + digit
        }
    }

    /**
     * 删除最后一个数字
     */
    private fun deleteLastDigit() {
        val current = binding.tvPhoneNumber.text.toString()
        if (current.isNotEmpty()) {
            binding.tvPhoneNumber.text = current.substring(0, current.length - 1)
        }
    }

    private fun checkPermission() {
        val permissionHelper = SDKPermissionHelper(Utils.getApp(),object : IPermissionCallback {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onAgree() {
                dealAddCall()
            }
            override fun onDenied() {
                sLogger.debug("checkPermission onCallAdded onDenied and will check permission after call")
            }
        })
        permissionHelper.checkAndRequestPermission(NewCallAppSdkInterface.PERMISSION_TYPE_BEFORE_CALL)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun dealAddCall(){
        NewCallsManager.instance.addCallStateListener(CALL_ID, object : ICallStateListener{
            override fun onCallAdded(
                context: Context,
                callInfo: CallInfo
            ) {
                scope.launch(Dispatchers.Main) {
                    showCallUI(callInfo)
                }
            }

            override fun onCallRemoved(
                context: Context,
                callInfo: CallInfo
            ) {
                scope.launch(Dispatchers.Main) {
                    binding.tvCallStatus.text = getString(R.string.hangingup)
                    binding.btnAnswerContainer.visibility = View.GONE
                    binding.btnAnswer.visibility = View.GONE
                    binding.btnHangupContainer.visibility = View.GONE
                    binding.btnHangup.visibility = View.GONE
                    delay(1000)
                    showMainUI()
                }
            }

            override fun onCallStateChanged(
                callInfo: CallInfo,
                state: Int
            ) {
                scope.launch(Dispatchers.Main) {
                    if (state == Call.STATE_ACTIVE){
                        binding.tvCallStatus.text = getString(R.string.active)
                        binding.btnAnswerContainer.visibility = View.GONE
                        binding.btnAnswer.visibility = View.GONE
                        binding.btnHangupContainer.visibility = View.VISIBLE
                        binding.btnHangup.visibility = View.VISIBLE
                        startCallTimer()
                    } else if (state == Call.STATE_DISCONNECTING){
                        DCSocketManager.notifyHangUp()
                        dealHangup()
                    } else if (state == Call.STATE_CONNECTING){
                        DCSocketManager.notifyCallActive()
                        dealActiveCall()
                    }
                }
            }

            override fun onAudioDeviceChange() {
            }

        })
        val current = binding.tvPhoneNumber.text.toString()

        val callInfo = CallInfo(
            slotId = 0,
            telecomCallId = CALL_ID,
            myNumber = "12345678901",
            remoteNumber = current,
            state = STATE_RINGING,
            videoState = 0,
            isConference = false,
            isOutgoingCall = false,
            isCtCall = true
        )

        InCallServiceManager.instance.onBind(this@LocalTestingMainActivity,null)
        InCallServiceManager.instance.onCallAdded(callInfo,null)
    }

    private fun showMainUI() {
        binding.mainContainer.visibility = View.VISIBLE
        binding.callingView.visibility = View.GONE
        binding.btnAnswer.visibility = View.GONE
        binding.btnHangup.visibility = View.GONE
        binding.btnAnswerContainer.visibility = View.GONE
        binding.btnHangupContainer.visibility = View.GONE
        binding.tvCallStatus.text = ""
        binding.tvCallDuration.text = ""

        // 恢复显示拨号键盘
        binding.dialpadContainer.visibility = View.VISIBLE
        binding.tvPhoneNumber.visibility = View.VISIBLE
    }

    private fun showCallUI(callInfo: CallInfo) {
        binding.mainContainer.visibility = View.GONE
        binding.callingView.visibility = View.VISIBLE

//        binding.tvCallStatus.visibility = View.VISIBLE
        binding.tvCallStatus.text = getString(R.string.ringing)
        binding.tvCallPhoneNumber.text = callInfo.remoteNumber ?: ""
        binding.tvCallName.text = ""
        binding.tvCallDuration.text = ""

        binding.btnAnswerContainer.visibility = View.VISIBLE
        binding.btnHangupContainer.visibility = View.VISIBLE
        binding.btnAnswer.visibility = View.VISIBLE
        binding.btnHangup.visibility = View.VISIBLE
    }

    private fun dealActiveCall(){
        scope.launch {
            NewCallsManager.instance.testNotifyCallStateChange(CALL_ID, Call.STATE_ACTIVE)
        }
    }

    private fun dealHangup(){
        scope.launch {
            NewCallsManager.instance.testNotifyCallStateChange(CALL_ID, Call.STATE_DISCONNECTED)
            TestImsDataChannelManager.closeBdc(0, CALL_ID)
            InCallServiceManager.instance.onCallRemoved(CALL_ID)
            InCallServiceManager.instance.onUnbind()
            stopCallTimer()
        }
    }

    private var timerJob: kotlinx.coroutines.Job? = null
    private var callStartTime: Long = 0

    private fun startCallTimer() {
        stopCallTimer()
        timerJob = scope.launch {
            callStartTime = System.currentTimeMillis()
            while (isActive) {
                try {
                    val duration = System.currentTimeMillis() - callStartTime
                    val seconds = (duration / 1000) % 60
                    val minutes = (duration / 1000) / 60
                    withContext(Dispatchers.Main) {
                        binding.tvCallDuration.text = String.format("%02d:%02d", minutes, seconds)
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

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCallTimer()
        DCSocketManager.notifyHangUp()
        dealHangup()
        DCSocketManager.unRegisterCallObserver()
        finishAffinity()
        exitProcess(0)
    }
}