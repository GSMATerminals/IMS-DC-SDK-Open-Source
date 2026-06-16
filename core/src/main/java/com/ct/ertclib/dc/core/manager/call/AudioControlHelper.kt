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
        sLogger.info("setSpeakerphone")
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

    fun setAudioDevice(name: String?, type: String?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.availableCommunicationDevices
            val targetDevice = devices.firstOrNull {
                sLogger.info("deviceName: ${it.productName}, type: ${it.type}")
                (name == null || it.productName.toString() == name) && (type == null || getDeviceTypeString(it.type) == type)
            }
            targetDevice?.let {
                sLogger.info("type: ${it.type}")
                if (it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                } else if (it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                    audioManager.mode = AudioManager.MODE_IN_CALL
                } else {
                    audioManager.mode = AudioManager.MODE_IN_CALL
                    val result = audioManager.setCommunicationDevice(it)
                    sLogger.info("setAudioDevice result: $result")
                    return result
                }
            } ?: return false
        } else {
            sLogger.info("setAudioDevice: Using setSpeakerphone for API < 31, API: ${Build.VERSION.SDK_INT}")
            if (type == "Speaker") {
                return setSpeakerphone(true)
            } else if (type == "Earpiece") {
                return setSpeakerphone(false)
            } else {
                return false
            }
        }
        return false
    }

    fun clearAudioDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        audioManager.mode = AudioManager.MODE_NORMAL
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

    fun getAudioDevices(): List<Map<String, String>> {
        val deviceList = mutableListOf<Map<String, String>>()
        val devices =
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            val deviceMap = mutableMapOf<String, String>()
            deviceMap["name"] = device.productName.toString()
            deviceMap["type"] = getDeviceTypeString(device.type)
            deviceList.add(deviceMap)
        }
        return deviceList
    }

    fun getCurrentAudioDevice(): Map<String, String>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.communicationDevice
            if (device != null) {
                return mapOf(
                    "name" to device.productName.toString(),
                    "type" to getDeviceTypeString(device.type)
                )
            }
        }
        return null
    }

    private fun getDeviceTypeString(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            else -> "Unknown ($type)"
        }
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