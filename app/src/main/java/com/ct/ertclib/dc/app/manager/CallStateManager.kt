package com.ct.ertclib.dc.app.manager

import android.content.Context
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.SystemUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object CallStateManager {

    private const val TAG = "CallStateManager"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startListenCallState(context: Context) {
        LogUtils.debug(TAG, "startListenCallState")
        scope.launch {
            if (SystemUtils.isMainProcess(context)) {
                NewCallAppSdkInterface.callStateFlow.distinctUntilChanged().collect { state ->
                    LogUtils.debug(TAG, "collect callStateFlow state: $state")
                    when (state) {
                        NewCallAppSdkInterface.CALL_START -> {
                            initAppModule()
                        }
                        NewCallAppSdkInterface.CALL_STOP -> {
                            releaseAppModule()
                        }
                    }
                }
            }
        }
    }

    private fun initAppModule() {
        FloatingBallManager.init()
    }

    private fun releaseAppModule() {
        FloatingBallManager.release()
    }
}