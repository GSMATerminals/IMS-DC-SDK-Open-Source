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

package com.ct.ertclib.dc.feature.testing

import android.content.ContentValues
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
import com.ct.ertclib.dc.core.ui.activity.BaseAppCompatActivity
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.feature.testing.databinding.ActivityLocalTestingMainBinding
import com.ct.ertclib.dc.feature.testing.socket.SocketNetworkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocalTestingMainActivity : BaseAppCompatActivity() {
    private val TAG = "LocalTestingMainActivity"
    private val sLogger = Logger.getLogger(TAG)
    private lateinit var binding: ActivityLocalTestingMainBinding
    private lateinit var viewModel: TestingViewModel
    private lateinit var scope: CoroutineScope
    private lateinit var spUtils: SPUtils

    private var mocktest = false
    private var isServer = false
    private var ip = "127.0.0.1"
    private var port = 9988
    private var networkManager: INetworkManager? = null

    var callInfo: CallInfo? = null
    var testNetworkManager: DCManager? = null
    var mCallGuideManager: BDCManager? = null
    var callsManager: NewCallsManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalTestingMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = Color.TRANSPARENT

//        val filter = IntentFilter()
//        filter.addAction(Intent.ACTION_SCREEN_ON)
//        filter.addAction(Intent.ACTION_SCREEN_OFF)
//        filter.addAction(Intent.ACTION_USER_PRESENT)
//        registerReceiver(ScreenBroadcastReceiver(), filter)

        viewModel = ViewModelProvider(this).get(TestingViewModel::class.java)
        scope = CoroutineScope(Dispatchers.Default)
        spUtils = SPUtils.getInstance()

        binding.backIcon.setOnClickListener {
            finish()
        }
        binding.btnSetting.setOnClickListener {
            val intent = Intent(this, LocalTestingSettingActivity::class.java)
            startActivity(intent)
        }


        binding.btnFakePrecall.setOnClickListener {
            val enableNewCall = spUtils.getBoolean("enableNewCall", false)
            if (!enableNewCall) {
                ToastUtils.showShortToast(this@LocalTestingMainActivity, "请先开启增强通话功能")
                return@setOnClickListener
            }
            binding.btnFakePrecall.isEnabled = false
            binding.btnFakeIncall.isEnabled = true
            binding.btnFakeEndcall.isEnabled = true
//            if (mocktest) {


                isServer = spUtils.getBoolean("isServer", false)
                var myNumber: String? = null//"15388916503"
                var remoteNumber: String? = "15388916504"
                if (isServer) {
                    val cacheNumber = myNumber
                    myNumber = remoteNumber
                    remoteNumber = cacheNumber
                }

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
                callsManager?.notifyOnCallAdded(callInfo!!)


                ip = spUtils.getString("ip", "127.0.0.1")
                port = spUtils.getInt("port", 9988)
                sLogger.info("SocketNetworkManager isServer:$isServer, ip:$ip, port:$port")
                SocketNetworkManager.init(
                    isServer,
                    ip,
                    port,
                    viewModel.isConnected,
                    callsManager!!
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    val uri = DBUriUtil.getContactUri()

                    val query = myNumber?.let {
                        contentResolver.query(
                            uri,
                            null,
                            "phoneNumber=?",
                            arrayOf(it),
                            null
                        )
                    }

                    query?.let { cursor ->
                        if (!cursor.moveToFirst()) {
                            val values = ContentValues()
                            values.put("phoneNumber", myNumber)
//                            values.put("avatarUri", "")
                            contentResolver.insert(uri, values)
                        }
                    }
                    val query2 = remoteNumber?.let {
                        contentResolver.query(
                            uri,
                            null,
                            "phoneNumber=?",
                            arrayOf(remoteNumber),
                            null
                        )
                    }

                    query2?.let { cursor ->
                        if (!cursor.moveToFirst()) {
                            val values = ContentValues()
                            values.put("phoneNumber", remoteNumber)
//                            values.put("avatarUri", "")
                            contentResolver.insert(uri, values)
                        }
                    }
                }
//            }
        }

        binding.btnFakeIncall.setOnClickListener {
            binding.btnFakePrecall.isEnabled = false
            binding.btnFakeIncall.isEnabled = false
            binding.btnFakeEndcall.isEnabled = true
            binding.btnSketchshare.isEnabled = true
//            miniAppEntryManager.displayEntry()
            scope.launch {
                if (mocktest) {
                    networkManager = SocketNetworkManager.getINSTANCE()
                    sLogger.info("SocketNetworkManager networkManager:$networkManager")
                    networkManager?.let {
                        (networkManager as SocketNetworkManager).start()
                    }
                    networkManager?.setCallback(object : INetworkCallback {
                        override fun onMessage(message: String) {
                            scope.launch(Dispatchers.Main) {
                                binding.etMessageReceive.append(message + "\n")
                            }
                        }
                    })
                }

                callInfo?.let {
                    it.state = Call.STATE_ACTIVE
                    sLogger.info("testNotifyCallStateChange")
                    callsManager?.testNotifyCallStateChange(it.telecomCallId, it.state)
//                    mCallGuideManager?.onCallStateChanged(it, Call.STATE_ACTIVE)
//                    MiniAppPackageManagerImpl.mMiniAppPMMap[it.telecomCallId]?.onCallStateChanged(
//                        it,
//                        Call.STATE_ACTIVE
//                    )
                }

            }

        }

        binding.btnFakeEndcall.setOnClickListener {
            hangup()

        }

        binding.btnSketchshare.setOnClickListener {
            networkManager?.sendCMD("cmd:sketch:request")
        }

        binding.btnSendMessage.setOnClickListener {
            val message = binding.etMessage.text.toString()
            sLogger.info("message to send $message")
            networkManager?.sendByte(message.toByteArray())
            binding.etMessage.setText("")
            binding.etMessage.clearFocus()
        }
    }

    fun hangup(){
        binding.btnFakePrecall.isEnabled = true
        binding.btnFakeIncall.isEnabled = false
        binding.btnFakeEndcall.isEnabled = false
        binding.btnSketchshare.isEnabled = false
//            miniAppEntryManager.dismissEntry()
        TestImsDataChannelManager.closeBdc(0, "TC@1")
        testNetworkManager?.unBindService(Utils.getApp())
        sLogger.info("onImsDCClose mCallGuideManager:$mCallGuideManager")
        mCallGuideManager?.onImsCallRemovedBDCClose()
        scope.launch {
            if (mocktest) {
                networkManager?.let {
                    (it as SocketNetworkManager).stop()
                }
            }
            callInfo?.let {
                it.state = Call.STATE_DISCONNECTED
                callsManager?.testNotifyCallStateChange(it.telecomCallId, it.state)
//                    mCallGuideManager?.onCallStateChanged(it, Call.STATE_DISCONNECTED)
//                    MiniAppPackageManagerImpl.mMiniAppPMMap[it.telecomCallId]?.onCallStateChanged(
//                        it,
//                        Call.STATE_DISCONNECTED
//                    )
                callInfo = null
                testNetworkManager = null
                callsManager = null
            }
        }
    }
    override fun onResume() {
        super.onResume()
        mocktest = spUtils.getBoolean("mock_socket", false)
    }

    override fun onDestroy() {
        super.onDestroy()
        hangup()
    }
}
