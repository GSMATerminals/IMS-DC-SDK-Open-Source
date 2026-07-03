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

package com.ct.ertclib.dc.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.ct.ertclib.dc.app.manager.CallServiceStateManager
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.coreModule
import com.ct.ertclib.dc.core.utils.common.ActivityTracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * 这里不一定能执行，不要放核心逻辑
 */

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

        CallServiceStateManager.startListenCallServiceState(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                ActivityTracker.onActivityResumed(activity)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {
                ActivityTracker.onActivityPaused(activity)
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build();
    }
}