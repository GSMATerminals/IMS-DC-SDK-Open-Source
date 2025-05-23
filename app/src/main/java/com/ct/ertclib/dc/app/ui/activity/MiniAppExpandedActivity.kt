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

package com.ct.ertclib.dc.app.ui.activity

import android.app.Activity
import android.app.KeyguardManager
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.ct.ertclib.dc.app.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.app.ui.fragment.MiniAppExpandedDialogFragment
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.ui.activity.BaseFragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MiniAppExpandedActivity : BaseFragmentActivity() {

    companion object {
        private const val TAG = "MiniAppExpandedActivity"
        private const val ACTIVITY_ANIMATION_DELAY = 300L
    }

    private lateinit var mDialogFragment: MiniAppExpandedDialogFragment
    private val handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mini_app_expanded)
        val point = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("point", Point::class.java)
        } else {
            intent.getParcelableExtra("point") as Point?
        }
        val miniAppList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("miniAppList", MiniAppList::class.java)
        } else {
            intent.getParcelableExtra("miniAppList") as MiniAppList?
        }
        val callInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("callInfo", CallInfo::class.java)
        } else {
            intent.getParcelableExtra("callInfo") as CallInfo?
        }

        mDialogFragment = MiniAppExpandedDialogFragment(point, miniAppList, callInfo)
        mDialogFragment.setCallback(object : MiniAppExpandedDialogFragment.Callback {
            override fun onDismiss() {
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "mDialogFragment onDismiss")
                handler.postDelayed({
                    this@MiniAppExpandedActivity.finish()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        this@MiniAppExpandedActivity.overrideActivityTransition(
                            Activity.OVERRIDE_TRANSITION_CLOSE,
                            0,
                            0,
                            Color.TRANSPARENT
                        )
                    } else {
                        this@MiniAppExpandedActivity.overridePendingTransition(0, 0)
                    }
                }, ACTIVITY_ANIMATION_DELAY)
            }
        })
        mDialogFragment.show(
            supportFragmentManager,
            MiniAppExpandedDialogFragment::class.simpleName
        )
        startCollectFlow()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        // 如果锁屏就请求用户解锁
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val locked = keyguardManager.isKeyguardLocked
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "is screen lock:$locked")
        if (locked){
            keyguardManager.newKeyguardLock("unLock")
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissError() {
                    super.onDismissError()
                }

                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                }

                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }

    private fun startCollectFlow() {
        lifecycleScope.launch(Dispatchers.Main) {
            NewCallAppSdkInterface.closeExpandedViewFlow.distinctUntilChanged().collect { isClose ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "closeExpandedViewFlow collect: $isClose")
                if (isClose) {
                    try {
                        mDialogFragment.dismiss()
                    } catch (e: IllegalStateException) {
                        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.ERROR_LEVEL, TAG, "close miniapp fragment failed, message ${e.message}")
                    }
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            NewCallAppSdkInterface.callInfoEventFlow.distinctUntilChanged().collect { it ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "collect callInfoEventFlow, event: $it")
                mDialogFragment.refreshCallInfo(it)
            }
        }
    }
}
