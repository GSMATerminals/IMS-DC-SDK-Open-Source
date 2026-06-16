package com.ct.ertclib.dc.app.manager

import android.content.Context
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.SystemUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object CallServiceStateManager {

    private const val TAG = "CallStateManager"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startListenCallServiceState(context: Context) {
        LogUtils.debug(TAG, "startListenCallState")
        scope.launch {
            if (SystemUtils.isMainProcess(context)) {
                NewCallAppSdkInterface.callServiceStateFlow.distinctUntilChanged().collect { state ->
                    LogUtils.debug(TAG, "collect callServiceStateFlow state: $state")
                    when (state) {
                        NewCallAppSdkInterface.CALL_SERVICE_START -> {
                            initAppModule()
                        }
                        NewCallAppSdkInterface.CALL_SERVICE_STOP -> {
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