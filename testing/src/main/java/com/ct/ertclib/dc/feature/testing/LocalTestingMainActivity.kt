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

package com.ct.ertclib.dc.feature.testing

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.Call.STATE_RINGING
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

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

        binding.btnSimulateCall.setOnClickListener {
            DCSocketManager.notifyCallAdded()
            checkPermission()
        }

        binding.btnAccept.setOnClickListener {
            DCSocketManager.notifyCallActive()
            dealActiveCall()
        }
        binding.btnDecline.setOnClickListener {
            DCSocketManager.notifyHangUp()
            dealHangup()
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
                    binding.contentLayout.background = ContextCompat.getDrawable(this@LocalTestingMainActivity, R.drawable.call_background)
                    binding.btnSetting.visibility = View.GONE
                    binding.btnSimulateCall.visibility = View.GONE
                    binding.layoutState.visibility = View.VISIBLE
                    binding.ivRingingBell.visibility = View.VISIBLE
                    binding.tvState.text = getString(R.string.ringing)
                    val newDrawable = ContextCompat.getDrawable(this@LocalTestingMainActivity, R.drawable.ic_bell_animated)
                    binding.ivRingingBell.setImageDrawable(newDrawable)
                    (binding.ivRingingBell.drawable as? Animatable)?.start()
                    binding.layoutIncomingCall.visibility = View.VISIBLE
                    binding.btnAccept.visibility = View.VISIBLE
                }
            }

            override fun onCallRemoved(
                context: Context,
                callInfo: CallInfo
            ) {
                scope.launch(Dispatchers.Main) {
                    binding.ivRingingBell.visibility = View.GONE
                    binding.tvState.text = getString(R.string.hangingup)
                    binding.layoutIncomingCall.visibility = View.GONE
                    delay(1000)
                    binding.btnSetting.visibility = View.VISIBLE
                    binding.btnSimulateCall.visibility = View.VISIBLE
                    (binding.ivRingingBell.drawable as? Animatable)?.stop()
                    binding.layoutState.visibility = View.GONE
                    binding.btnAccept.visibility = View.VISIBLE
                    binding.contentLayout.background = null
                }
            }

            override fun onCallStateChanged(
                callInfo: CallInfo,
                state: Int
            ) {
                scope.launch(Dispatchers.Main) {
                    if (state == Call.STATE_ACTIVE){
                        (binding.ivRingingBell.drawable as? Animatable)?.stop()
                        binding.layoutState.visibility = View.VISIBLE
                        binding.ivRingingBell.visibility = View.GONE
                        binding.tvState.text = getString(R.string.active)
                        binding.btnAccept.visibility = View.GONE
                    }
                }
            }

            override fun onAudioDeviceChange() {
            }

        })

        val callInfo = CallInfo(
            slotId = 0,
            telecomCallId = CALL_ID,
            myNumber = "12345678901",
            remoteNumber = "12345678902",
            state = STATE_RINGING,
            videoState = 0,
            isConference = false,
            isOutgoingCall = false,
            isCtCall = true
        )

        // 模拟建立通话
        InCallServiceManager.instance.onBind(this@LocalTestingMainActivity,null)
        InCallServiceManager.instance.onCallAdded(callInfo,null)

    }

    private fun dealActiveCall(){
        // 处理接听
        scope.launch {
            NewCallsManager.instance.testNotifyCallStateChange(CALL_ID, Call.STATE_ACTIVE)
        }
    }

    private fun dealHangup(){
        scope.launch {
            // 处理挂断
            NewCallsManager.instance.testNotifyCallStateChange(CALL_ID, Call.STATE_DISCONNECTED)
            TestImsDataChannelManager.closeBdc(0, CALL_ID)

            InCallServiceManager.instance.onCallRemoved(CALL_ID)
            InCallServiceManager.instance.onUnbind()
        }
    }
    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        DCSocketManager.notifyHangUp()
        dealHangup()
        DCSocketManager.unRegisterCallObserver()
    }
}
