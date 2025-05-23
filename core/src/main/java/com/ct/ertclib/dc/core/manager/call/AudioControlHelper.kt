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

package com.ct.ertclib.dc.core.manager.call

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.ct.ertclib.dc.core.utils.logger.Logger

class AudioControlHelper(private val context: Context) {
    companion object {
        private const val TAG = "AudioControlHelper"
    }
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var onAudioDeviceChangeListener: OnAudioDeviceChangeListener? = null
    private val sLogger: Logger = Logger.getLogger(TAG)

    // region Android 11+ 的新API实现
    @android.annotation.SuppressLint("NewApi")
    private fun setCommunicationDevice(enableSpeaker: Boolean): Boolean {
        val devices = audioManager.availableCommunicationDevices

        val targetType = if (enableSpeaker) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            // 优先尝试使用听筒，如果没有则使用其他设备
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        }

        // 首先尝试精确匹配
        var targetDevice = devices.firstOrNull { it.type == targetType }

        // 如果没有找到精确匹配的设备
        if (targetDevice == null) {
            targetDevice = if (enableSpeaker) {
                // 找其他类型的扬声器或输出设备
                devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            } else {
                // 找其他类型的输入设备
                devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                    ?: devices.firstOrNull() // 最后回退到第一个可用设备
            }
        }

        return targetDevice?.let {
            audioManager.setCommunicationDevice(it)
            true
        } == true
    }

    // 控制免提/扬声器（声音外放）
    fun setSpeakerphone(enable: Boolean): Boolean {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            setCommunicationDevice(enable)
//        } else {
//            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = enable
            // 设置正确的音频模式
            audioManager.mode = if (enable) {
                AudioManager.MODE_IN_COMMUNICATION
            } else {
                AudioManager.MODE_IN_CALL
            }
            return true
//        }
    }

    fun isSpeakerphoneOn(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val currentDevice = audioManager.communicationDevice
            currentDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn
        }
    }

    // 控制麦克风静音（对方听不到你的声音）
    fun setMuted(muted: Boolean) {
        audioManager.isMicrophoneMute = muted
    }

    fun isMuted(): Boolean{
        val isMuted = audioManager.isMicrophoneMute
        return isMuted
    }

    fun registerAudioDeviceCallback(listener: OnAudioDeviceChangeListener) {
        onAudioDeviceChangeListener = listener
        audioDeviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                onAudioDeviceChangeListener?.onAudioDeviceChange()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                onAudioDeviceChangeListener?.onAudioDeviceChange()
            }
        }

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null) // 使用主线程Handler
    }


    fun unregisterAudioDeviceCallback() {
        audioDeviceCallback?.let {
            audioManager.unregisterAudioDeviceCallback(it)
        }
        audioDeviceCallback = null
        onAudioDeviceChangeListener = null
    }

    interface OnAudioDeviceChangeListener {
        fun onAudioDeviceChange()
    }
}