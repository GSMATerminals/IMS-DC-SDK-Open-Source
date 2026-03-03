package com.ct.ertclib.dc.app

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.ct.ertclib.dc.app.manager.CallStateManager
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.coreModule
import com.ct.ertclib.dc.core.utils.common.WebViewUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DCApplication : Application(), CameraXConfig.Provider {

    companion object {
        private const val TAG = "DCApplication"
    }

    override fun onCreate() {
        super.onCreate()
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "onCreate")
        Thread.setDefaultUncaughtExceptionHandler(object : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(t: Thread, e: Throwable) {
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "uncaughtException : ${e.message}")
            }
        })

        startKoin {
            androidContext(this@DCApplication)
            modules(coreModule)
        }

        CallStateManager.startListenCallState(this)

        CoroutineScope(Dispatchers.IO).launch {
            WebViewUtil.handleWebViewDir(this@DCApplication)
        }
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build();
    }
}