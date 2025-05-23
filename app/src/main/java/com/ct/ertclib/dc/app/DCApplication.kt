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

package com.ct.ertclib.dc.app

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.ct.ertclib.dc.app.manager.FloatingBallManager
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import com.ct.ertclib.dc.core.utils.common.WebViewUtil

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
        WebViewUtil.handleWebViewDir(this)

        NewCallAppSdkInterface.init(this@DCApplication)
        FloatingBallManager.init()
        ScreenUtils.registerListener()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build();
    }

    override fun onTerminate() {
        super.onTerminate()
        FloatingBallManager.release()
        ScreenUtils.unRegisterListener()
    }
}