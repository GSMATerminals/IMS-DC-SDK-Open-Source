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

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.telecom.Call
import android.telecom.Call.STATE_RINGING
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.manager.call.BDCManager
import com.ct.ertclib.dc.core.miniapp.MiniAppStartManager
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.manager.call.DCManager
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.core.ui.activity.BaseAppCompatActivity
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.feature.testing.databinding.ActivityLocalTestingMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class LocalTestingMainActivity : BaseAppCompatActivity() {
    private val TAG = "LocalTestingMainActivity"
    private val sLogger = Logger.getLogger(TAG)
    private lateinit var binding: ActivityLocalTestingMainBinding
    private lateinit var viewModel: TestingViewModel
    private lateinit var scope: CoroutineScope
    private lateinit var spUtils: SPUtils

    var callInfo: CallInfo? = null
    var testNetworkManager: DCManager? = null
    var mCallGuideManager: BDCManager? = null
    var callsManager: NewCallsManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalTestingMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = Color.TRANSPARENT

        viewModel = ViewModelProvider(this).get(TestingViewModel::class.java)
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        spUtils = SPUtils.getInstance()

        binding.backIcon.setOnClickListener {
            finish()
        }
        binding.btnSetting.setOnClickListener {
            val intent = Intent(this, LocalTestMiniAppWarehouseActivity::class.java)
            startActivity(intent)
        }

        binding.btnFakePrecall.setOnClickListener {
            val enableNewCall = spUtils.getBoolean("enableNewCall", false)
            if (!enableNewCall) {
                ToastUtils.showShortToast(this@LocalTestingMainActivity, getString(com.ct.ertclib.dc.core.R.string.please_open))
                return@setOnClickListener
            }
            binding.btnSetting.isEnabled = false
            binding.btnFakePrecall.isEnabled = false
            binding.btnFakeIncall.isEnabled = true
            binding.btnFakeEndcall.isEnabled = true


            var myNumber: String? = "12345678901"
            var remoteNumber: String? = "12345678902"

            testNetworkManager = DCManager()

            callInfo = CallInfo(
                slotId = 0,
                telecomCallId = "TC@1",
                myNumber = myNumber,
                remoteNumber = remoteNumber,
                state = STATE_RINGING,
                videoState = 0,
                isConference = false,
                isOutgoingCall = false,
                isCtCall = true
            )

            spUtils.put("myNumber", myNumber)

            val callList = ArrayList<CallInfo>().also {
                it.add(callInfo!!)
            }
            callsManager = NewCallsManager(callList)
            MiniAppManager.setCallsManager(callsManager!!)
            MiniAppManager.setNetworkManager(testNetworkManager!!)
            val miniAppPackageManagerImpl = MiniAppManager(callInfo!!)
            miniAppPackageManagerImpl.setMiniAppStartManager(MiniAppStartManager)
            mCallGuideManager = BDCManager(callInfo!!, miniAppPackageManagerImpl)
            testNetworkManager?.setCurrentCallId("TC@1")
            testNetworkManager?.registerBDCCallback(
                "TC@1",
                mCallGuideManager!!
            )
            callsManager?.addCallStateListener("TC@1", testNetworkManager!!)
            callsManager?.addCallStateListener("TC@1", miniAppPackageManagerImpl)
            callsManager?.addCallStateListener("TC@1", mCallGuideManager!!)
            callsManager?.addCallStateListener("TC@1", object : ICallStateListener{
                override fun onCallAdded(
                    context: Context,
                    callInfo: CallInfo
                ) {
                }

                override fun onCallRemoved(
                    context: Context,
                    callInfo: CallInfo
                ) {
                }

                override fun onCallStateChanged(
                    info: CallInfo,
                    state: Int
                ) {
                    scope.launch(Dispatchers.Main) {
                        if (state == Call.STATE_DISCONNECTED){
                            binding.btnSetting.isEnabled = true
                            binding.btnFakePrecall.isEnabled = true
                            binding.btnFakeIncall.isEnabled = false
                            binding.btnFakeEndcall.isEnabled = false
                            TestImsDataChannelManager.closeBdc(0, "TC@1")
                            testNetworkManager?.unBindService(Utils.getApp())
                            sLogger.info("onImsDCClose mCallGuideManager:$mCallGuideManager")
                            mCallGuideManager?.onImsCallRemovedBDCClose()
                            testNetworkManager = null
                            callsManager = null
                            callInfo = null
                        } else if (state == Call.STATE_ACTIVE){
                            binding.btnSetting.isEnabled = false
                            binding.btnFakePrecall.isEnabled = false
                            binding.btnFakeIncall.isEnabled = false
                            binding.btnFakeEndcall.isEnabled = true
                        }
                    }
                }

                override fun onAudioDeviceChange() {
                }

            })
            callsManager?.notifyOnCallAdded(callInfo!!)

        }

        binding.btnFakeIncall.setOnClickListener {
            scope.launch {
                callInfo?.let {
                    it.state = Call.STATE_ACTIVE
                    sLogger.info("testNotifyCallStateChange")
                    callsManager?.testNotifyCallStateChange(it.telecomCallId, it.state)
                }
            }
        }
        binding.btnFakeEndcall.setOnClickListener {
            hangup()
        }
    }

    fun hangup(){
        scope.launch {
            callInfo?.let {
                it.state = Call.STATE_DISCONNECTED
                callsManager?.testNotifyCallStateChange(it.telecomCallId, it.state)
            }
        }
    }
    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        hangup()
    }
}
