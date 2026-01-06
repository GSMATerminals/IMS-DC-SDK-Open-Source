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

package com.ct.ertclib.dc.core.manager.screenshare

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.port.manager.IScreenShareManager
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.PkgUtils
import com.newcalllib.sharescreen.IScreenShareHandler
import com.newcalllib.sharescreen.IScreenShareStatusListener
import com.newcalllib.sharescreen.ScreenShareStatus
import org.koin.core.component.KoinComponent

class ScreenShareManager(private val context: Context) : IScreenShareManager, KoinComponent {

    companion object {
        private const val TAG = "ScreenShareManager"
    }

    private var screenShareService: IScreenShareHandler? = null
    private var isSharing = false

    override fun startShareScreen(): Boolean {
        if (isSharing) {
            LogUtils.warn(TAG, "startShareScreen isSharing, return")
            return false
        }
        LogUtils.info(TAG, "startShareScreen")
        isSharing = true
        screenShareService?.startNativeScreenShare(screenShareStatusListener)
        return true
    }

    override fun stopShareScreen() {
        LogUtils.info(TAG, "stopShareScreen")
        if (screenShareService == null) {
            LogUtils.warn(TAG, "stopShareScreen failed, screenShareService is null")
            return
        }
        if (!isSharing) {
            LogUtils.info(TAG, "stopShareScreen failed, isSharing is false, return")
            return
        }
        kotlin.runCatching {
            screenShareService?.stopNativeScreenShare()
        }.onFailure {
            LogUtils.error(TAG, "stopScreenShare failure: $it")
        }
        isSharing = false
    }

    override fun requestScreenShareAbility(): Boolean {
        val result = try {
            screenShareService?.requestNativeScreenShareAbility() ?: false
        } catch (e: Exception) {
            LogUtils.error(TAG, "requestScreenShareAbility exception", e)
            false
        }
        LogUtils.info(TAG, "requestScreenShareAbility, result: $result")
        return result
    }

    override fun initManager() {
        bindScreenShareService(context)
    }

    override fun release() {
        if (isSharing) {
            stopShareScreen()
        }
        unbindScreenShareService(context)
        screenShareService = null
        isSharing = false
    }

    override fun isSharing(): Boolean {
        return isSharing
    }

    private fun bindScreenShareService(context: Context) {
        val packageName = context.resources.getString(R.string.screenshareservice_package_name)
        val serviceName = context.getString(R.string.screenshareservice_name)
        val actionName = context.getString(R.string.screenshareservice_action)

        val intent = Intent().apply {
            when(PkgUtils.brand()){
                PkgUtils.XIAOMI -> {
                    `package` = packageName
                }
                else -> {
                    component = ComponentName(packageName, serviceName)
                }
            }
            action = actionName
        }
        kotlin.runCatching {
            LogUtils.info(TAG, "bindScreenShareService:${packageName} $serviceName $actionName")
            context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        }.onFailure {
            LogUtils.error(TAG, "bind screen share service failure: $it")
        }
    }

    private fun unbindScreenShareService(context: Context) {
        LogUtils.info(TAG, "unbindScreenShareService")
        kotlin.runCatching {
            context.unbindService(conn)
        }.onFailure {
            LogUtils.error(TAG, "unbindScreenShareService failure: $it")
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            LogUtils.info(TAG, "onServiceConnected")
            screenShareService = IScreenShareHandler.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            screenShareService = null
            LogUtils.info(TAG, "onServiceDisconnected")
        }
    }

    private val screenShareStatusListener = object : IScreenShareStatusListener.Stub() {
        override fun onScreenShareStatus(status: ScreenShareStatus?) {
            when (status) {
                ScreenShareStatus.SUCCESS -> {
                    LogUtils.info(TAG, "start screen share success")
                }

                ScreenShareStatus.FAILURE -> {
                    LogUtils.info(TAG, "start screen share failed")
                }

                else -> {
                    LogUtils.info(TAG, "start screen share status unknown")
                }
            }
        }
    }
}