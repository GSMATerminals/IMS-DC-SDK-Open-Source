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

package com.ct.ertclib.dc.core.usecase.common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REFRESH_MINI_PERMISSION
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_REFRESH_PERMISSION
import com.ct.ertclib.dc.core.constants.CommonConstants.COMMON_APP_EVENT
import com.ct.ertclib.dc.core.data.event.NotifyEvent
import com.ct.ertclib.dc.core.data.miniapp.AppRequest
import com.ct.ertclib.dc.core.data.miniapp.PermissionUsageData
import com.ct.ertclib.dc.core.data.miniapp.getCombineKey
import com.ct.ertclib.dc.core.data.model.PermissionModel
import com.ct.ertclib.dc.core.data.model.PermissionUsageEntity
import com.ct.ertclib.dc.core.port.common.IParentToMiniNotify
import com.ct.ertclib.dc.core.port.manager.IMiniToParentManager
import com.ct.ertclib.dc.core.port.miniapp.IPermissionDbRepo
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.utils.common.DateUtils
import com.ct.ertclib.dc.core.utils.common.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

class PermissionUseCase(
    private val context: Context,
    private val permissionDbRepo: IPermissionDbRepo,
    private val miniToParentManager: IMiniToParentManager,
    private val parentToMiniNotifier: IParentToMiniNotify
): IPermissionUseCase {

    companion object {
        private const val TAG = "PermissionUseCase"
        private const val MAX_TIME_GET_PERMISSIONS = 3000L
    }

    private val logger = Logger.getLogger(TAG)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val permissionDaoFlow = permissionDbRepo.getAll()
    private val permissionsMap = mutableMapOf<String, MutableMap<String, Boolean>>()

    init {
        scope.launch {
            permissionDaoFlow.collect {
                it.forEach { model ->
                    val resultMap = mutableMapOf<String, Boolean>()
                    val map = JsonUtil.fromJson(model.permissionMapString, Map::class.java)
                    map?.forEach { (entry, value) ->
                        val result = value as? Boolean
                        result?.let { resultMap[entry.toString()] = result }
                    }
                    permissionsMap[model.appId] = resultMap
                    logger.info("collect, update permissionTable,appId: ${model.appId},  permissionsMap: ${permissionsMap[model.appId]}")
                }
            }
        }
    }

    override suspend fun getPermission(
        appId: String
    ): MutableMap<String, Boolean> {
        val permissionMap = permissionsMap[appId]
        permissionMap?.let {
            return it
        } ?: run {
            val resultMap = mutableMapOf<String, Boolean>()
            val permissionJsonString = permissionDbRepo.getPermissionModelById(appId)
            permissionJsonString?.let { permissionModel ->
                val map = JsonUtil.fromJson(permissionModel.permissionMapString, Map::class.java)
                map?.forEach { (entry, value) ->
                    val result = value as? Boolean
                    result?.let { resultMap[entry.toString()] = result }
                }
            }
            permissionsMap[appId] = resultMap
            return resultMap
        }
    }

    override suspend fun savePermission(
        appId: String,
        map: MutableMap<String, Boolean>,
        isMainProcess: Boolean,
        callId: String,
    ) {
        var currentMap = permissionsMap[appId]
        currentMap?.putAll(map) ?: run {
            currentMap = map
            permissionsMap[appId] = map
        }
        currentMap?.let {
            val permissionJsonString = JsonUtil.toJson(it)
            permissionDbRepo.insertOrUpdate(PermissionModel(appId, permissionJsonString))
        }
        if (isMainProcess) {
            val permissionNotifyEvent = NotifyEvent(
                ACTION_REFRESH_MINI_PERMISSION,
                mapOf()
            )
            parentToMiniNotifier.notifyEvent(callId, appId, permissionNotifyEvent)
        } else {
            val appRequestJson = AppRequest(COMMON_APP_EVENT, ACTION_REFRESH_PERMISSION, mapOf()).toJson()
            miniToParentManager.sendMessageToParent(appRequestJson, null)
        }
    }

    override fun checkPermissionAndRecord(appId: String, permissions: List<String>, needRecord: Boolean): Boolean {
        var grantedResult = false
        permissionsMap[appId]?.let {
            grantedResult = isPermissionAllGranted(permissions, it)
        } ?: run {
            var map: MutableMap<String, Boolean> = mutableMapOf()
            runBlocking {
                withTimeoutOrNull(MAX_TIME_GET_PERMISSIONS) {
                    map = getPermission(appId)
                }
            }
            grantedResult = isPermissionAllGranted(permissions, map)
        }
        if (grantedResult && needRecord) {
            //当所有权限全部授予，且needRecord为true时，将权限使用记录插入到数据库中，默认情况下needRecord值为true
            insertPermissionUsages(appId, permissions)
        }
        return grantedResult
    }

    override fun isSystemPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun refreshPermissionMapFromRepo(appId: String) {
        val resultMap = mutableMapOf<String, Boolean>()
        val permissionJsonString = permissionDbRepo.getPermissionModelById(appId)
        permissionJsonString?.let { permissionModel ->
            val map = JsonUtil.fromJson(permissionModel.permissionMapString, Map::class.java)
            map?.forEach { (entry, value) ->
                val result = value as? Boolean
                result?.let { resultMap[entry.toString()] = result }
            }
        }
        LogUtils.debug(TAG, "refreshPermissionMapFromRepo appId: $appId, resultMap: $resultMap")
        permissionsMap[appId] = resultMap
    }

    override fun getPermissionUsage(appId: String, timeStamp: Long): MutableList<PermissionUsageData> {
        val permissionUsageList = permissionDbRepo.getPermissionUsageByIdWithInTime(appId, timeStamp)
        val resultList = permissionUsageList.asSequence().map {
            PermissionUsageData(PermissionUtils.getPermissionData(it.permissionName)?.permissionUsageName ?: "", DateUtils.timestampToDateTime(it.permissionUsageTimeStamp))
        }.filter { it.permissionTitle.isNotEmpty() }.groupBy { it.getCombineKey() }.map { (_, group) -> group.first() }.toMutableList()//对权限结果进行过滤，同分钟内同一个权限记录只保留一次
        return resultList
    }

    override fun insertPermissionUsages(appId: String, miniPermissionNames: List<String>) {
        scope.launch(Dispatchers.IO) {
            miniPermissionNames.forEach { miniPermissionName ->
                val permissionUsageEntity = PermissionUsageEntity(appId, PermissionUtils.convertToSingleSystemPermissions(miniPermissionName), System.currentTimeMillis())
                permissionDbRepo.insertPermissionUsage(permissionUsageEntity)
            }
        }
    }

    private fun isPermissionAllGranted(permissions: List<String>, permissionMap: MutableMap<String, Boolean>): Boolean {
        permissions.forEach { permission ->
            if (permissionMap[permission] != true) {
                return false
            }
        }
        return true
    }
}