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

package com.ct.ertclib.dc.core.utils.common

import android.Manifest
import android.content.Context
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.data.miniapp.PermissionData
import com.ct.ertclib.dc.core.data.miniapp.MiniAppPermissions
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object PermissionUtils : KoinComponent {

    private const val CAMERA_INDEX = 0
    private const val VIBRATE_INDEX = 1
    private const val RECORD_AUDIO_INDEX = 2
    private const val MANAGE_EXTERNAL_STORAGE_INDEX = 3
    private const val LOCATION_INDEX = 4
    private const val CALL_STATE_INDEX = 5
    private const val READ_CONTACTS_INDEX = 6
    private const val ACCESS_WIFI_INDEX = 7

    private val applicationContext: Context by inject()

    private val permissionDataList = listOf(
        PermissionData(applicationContext.resources.getString(R.string.camera_title), applicationContext.resources.getString(R.string.camera_description)),
        PermissionData(applicationContext.resources.getString(R.string.vibrate_title), applicationContext.resources.getString(R.string.vibrate_description)),
        PermissionData(applicationContext.resources.getString(R.string.record_audio_title), applicationContext.resources.getString(R.string.record_audio_description)),
        PermissionData(applicationContext.resources.getString(R.string.external_storage_title), applicationContext.resources.getString(R.string.external_storage_description)),
        PermissionData(applicationContext.resources.getString(R.string.location_title), applicationContext.resources.getString(R.string.location_description)),
        PermissionData(applicationContext.resources.getString(R.string.call_state_title), applicationContext.resources.getString(R.string.call_state_description)),
        PermissionData(applicationContext.resources.getString(R.string.contacts), applicationContext.resources.getString(R.string.contact_description)),
        PermissionData(applicationContext.resources.getString(R.string.wifi), applicationContext.resources.getString(R.string.wifi_description))
    )

    private val permissionDataMap = mutableMapOf(
        MiniAppPermissions.MINIAPP_CAMERA to permissionDataList[CAMERA_INDEX],
        MiniAppPermissions.MINIAPP_VIBRATE to permissionDataList[VIBRATE_INDEX],
        MiniAppPermissions.MINIAPP_RECORD_AUDIO to permissionDataList[RECORD_AUDIO_INDEX],
        MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE to permissionDataList[MANAGE_EXTERNAL_STORAGE_INDEX],
        MiniAppPermissions.MINIAPP_LOCATION to permissionDataList[LOCATION_INDEX],
        MiniAppPermissions.MINIAPP_GET_CALL_STATE to permissionDataList[CALL_STATE_INDEX],
        MiniAppPermissions.MINIAPP_READ_CONTACTS to permissionDataList[READ_CONTACTS_INDEX],
        MiniAppPermissions.MINIAPP_ACCESS_WIFI to permissionDataList[ACCESS_WIFI_INDEX]
    )

    private val systemPermissionDataMap = mutableMapOf(
        Manifest.permission.CAMERA to permissionDataList[CAMERA_INDEX],
        Manifest.permission.VIBRATE to permissionDataList[VIBRATE_INDEX],
        Manifest.permission.RECORD_AUDIO to permissionDataList[RECORD_AUDIO_INDEX],
        Manifest.permission.MANAGE_EXTERNAL_STORAGE to permissionDataList[MANAGE_EXTERNAL_STORAGE_INDEX],
        Manifest.permission.ACCESS_FINE_LOCATION to permissionDataList[LOCATION_INDEX],
        Manifest.permission.ACCESS_COARSE_LOCATION to permissionDataList[LOCATION_INDEX],
        Manifest.permission.READ_PHONE_STATE to permissionDataList[CALL_STATE_INDEX],
        Manifest.permission.READ_CONTACTS to permissionDataList[READ_CONTACTS_INDEX],
        Manifest.permission.ACCESS_WIFI_STATE to permissionDataList[ACCESS_WIFI_INDEX]
    )

    //将permissionDataList转换成permissionMap，方便进行权限比对
    @JvmStatic
    fun convertPermissionDataToPermissionMap(permissionDataList: List<PermissionData>): MutableMap<String, Boolean> {
        val resultMap = mutableMapOf<String, Boolean>()
        permissionDataList.forEach {
            resultMap.putAll(convertPermissionDataToMap(it))
        }
        return resultMap
    }


    //将miniApp Permission列表转换成系统权限列表
    @JvmStatic
    fun convertToSystemPermissions(permissions: List<String>): MutableList<String> {
        val allPermission = mutableListOf<String>()
        permissions.forEach { permission ->
            when (permission) {
                MiniAppPermissions.MINIAPP_CAMERA -> allPermission.add(Manifest.permission.CAMERA)
                MiniAppPermissions.MINIAPP_VIBRATE -> allPermission.add(Manifest.permission.VIBRATE)
                MiniAppPermissions.MINIAPP_RECORD_AUDIO -> allPermission.add(Manifest.permission.RECORD_AUDIO)
                MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE -> allPermission.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                MiniAppPermissions.MINIAPP_LOCATION -> {
                    allPermission.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    allPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                MiniAppPermissions.MINIAPP_GET_CALL_STATE -> allPermission.add(Manifest.permission.READ_PHONE_STATE)
                MiniAppPermissions.MINIAPP_READ_CONTACTS -> allPermission.add(Manifest.permission.READ_CONTACTS)
                MiniAppPermissions.MINIAPP_ACCESS_WIFI -> allPermission.add(Manifest.permission.ACCESS_WIFI_STATE)
            }
        }
        return allPermission
    }

    //将单个系统权限转换成单个miniApp Permission
    fun convertToSingleMiniAppPermissions(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> MiniAppPermissions.MINIAPP_CAMERA
            Manifest.permission.VIBRATE -> MiniAppPermissions.MINIAPP_VIBRATE
            Manifest.permission.RECORD_AUDIO -> MiniAppPermissions.MINIAPP_RECORD_AUDIO
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> {
                MiniAppPermissions.MINIAPP_LOCATION
            }

            Manifest.permission.READ_PHONE_STATE -> MiniAppPermissions.MINIAPP_GET_CALL_STATE
            Manifest.permission.READ_CONTACTS -> MiniAppPermissions.MINIAPP_READ_CONTACTS
            Manifest.permission.ACCESS_WIFI_STATE -> MiniAppPermissions.MINIAPP_ACCESS_WIFI
            else -> ""
        }
    }

    //根据系统权限获取对应的权限名称
    fun convertToSystemPermissionData(permissions: List<String>): MutableList<PermissionData> {
        val permissionDataList = mutableListOf<PermissionData>()
        permissions.forEach {
            val permissionData = systemPermissionDataMap[it]
            permissionData?.let {
                if (!permissionDataList.contains(permissionData)) {
                    permissionDataList.add(permissionData)
                }
            }
        }
        return  permissionDataList
    }

    //将系统权限列表转换成单个miniApp Permission列表
    @JvmStatic
    fun convertToMiniAppPermissions(permissions: List<String>): MutableList<String> {
        val allPermission = mutableListOf<String>()
        permissions.forEach { permission ->
            val miniAppPermission = convertToSingleMiniAppPermissions(permission)
            if (!allPermission.contains(miniAppPermission) && miniAppPermission.isNotEmpty()) {
                allPermission.add(miniAppPermission)
            }
        }
        return allPermission
    }

    //基于permission list构造recyclerView需要的permissionData list
    @JvmStatic
    fun convertPermissionDataList(permissions: MutableList<String>): MutableList<PermissionData> {
        val permissionDataList = mutableListOf<PermissionData>()
        permissions.forEach {
            permissionDataMap[it]?.let { data ->
                if (!permissionDataList.contains(data)) {
                    permissionDataList.add(data)
                }
            }
        }
        return permissionDataList
    }

    //将permissionMap 转换成permissionDataList
    @JvmStatic
    fun convertMapToPermissionData(permissionMap: MutableMap<String, Boolean>): MutableList<PermissionData> {
        val permissionDataList = mutableListOf<PermissionData>()
        permissionMap.forEach { entry, value ->
            permissionDataMap[entry]?.let { permissionDataList.add(it).apply { it.willBeGranted = value } }
        }
        return permissionDataList
    }


    @JvmStatic
    private fun convertPermissionDataToMap(permissionData: PermissionData): Map<String, Boolean> {
        val map = mutableMapOf<String, Boolean>()
        when (permissionData.permissionName) {
            applicationContext.resources.getString(R.string.camera_title) -> {
                map[MiniAppPermissions.MINIAPP_CAMERA] = permissionData.willBeGranted
            }

            applicationContext.resources.getString(R.string.vibrate_title) -> {
                map[MiniAppPermissions.MINIAPP_VIBRATE] = permissionData.willBeGranted
            }

            applicationContext.resources.getString(R.string.record_audio_title) -> {
                map[MiniAppPermissions.MINIAPP_RECORD_AUDIO] = permissionData.willBeGranted
            }

            applicationContext.resources.getString(R.string.external_storage_title) -> {
                map[MiniAppPermissions.MINIAPP_EXTERNAL_STORAGE] = permissionData.willBeGranted
            }

            applicationContext.resources.getString(R.string.location_title) -> {
                map[MiniAppPermissions.MINIAPP_LOCATION] = permissionData.willBeGranted
            }
            applicationContext.resources.getString(R.string.call_state_title) -> {
                map[MiniAppPermissions.MINIAPP_GET_CALL_STATE] = permissionData.willBeGranted
            }
            applicationContext.resources.getString(R.string.contacts) -> {
                map[MiniAppPermissions.MINIAPP_READ_CONTACTS] = permissionData.willBeGranted
            }
            applicationContext.resources.getString(R.string.wifi) -> {
                map[MiniAppPermissions.MINIAPP_ACCESS_WIFI] = permissionData.willBeGranted
            }
        }
        return map
    }
}